package com.reponse.mvn.core.servlets;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reponse.mvn.core.jobs.CourseImportProgressStore;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/academy/course-import/history",
        "sling.servlet.methods=GET"
    }
)
public class CourseImportHistoryServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CourseImportHistoryServlet.class);
    private static final String HISTORY_PARENT = "/var/reponse/course-import-history";
    private static final int MAX_RECORDS = 50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private SlingRepository repository;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile CourseImportProgressStore progressStore;

    static final class RunRecord {
        public String jobId, status, triggeredBy, createdAt, scheduledAt, filePath, targetPath;
        public long processedRows, totalRows, created, updated, failed, skipped;
        @JsonRawValue public String rowEvents = "[]";
    }

    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json;charset=UTF-8");
        String jobId = trimOrNull(req.getParameter("jobId"));
        Session s = null;

        try {
            @SuppressWarnings("deprecation")
            Session admin = repository.loginAdministrative(null);
            s = admin;

            if (jobId != null) {
                CourseImportProgressStore.ProgressState state =
                    progressStore != null ? progressStore.get(jobId) : null;
                if (state != null) {
                    MAPPER.writeValue(res.getWriter(), fromState(state, jobId));
                    return;
                }
                if (!s.nodeExists(HISTORY_PARENT)) {
                    res.getWriter().print("{\"status\":\"not_found\"}");
                    return;
                }
                Node run = findByJobId(s.getNode(HISTORY_PARENT), jobId);
                if (run == null) { res.getWriter().print("{\"status\":\"not_found\"}"); return; }
                MAPPER.writeValue(res.getWriter(), fromNode(run));
                return;
            }

            if (!s.nodeExists(HISTORY_PARENT)) { res.getWriter().print("[]"); return; }

            NodeIterator it = s.getNode(HISTORY_PARENT).getNodes();
            List<String> names = new ArrayList<>();
            while (it.hasNext()) names.add(it.nextNode().getName());
            names.sort(Comparator.reverseOrder());

            Node parent = s.getNode(HISTORY_PARENT);
            List<RunRecord> runs = new ArrayList<>();
            for (String name : names) {
                if (runs.size() >= MAX_RECORDS) break;
                runs.add(fromNode(parent.getNode(name)));
            }
            MAPPER.writeValue(res.getWriter(), runs);

        } catch (Exception e) {
            LOG.error("[HISTORY] Failed to read run history", e);
            res.setStatus(500);
            res.getWriter().print("{\"error\":\"server error\"}");
        } finally {
            if (s != null) s.logout();
        }
    }

    private RunRecord fromNode(Node n) throws Exception {
        RunRecord r = new RunRecord();
        r.jobId         = str(n, "jobId");
        r.status        = str(n, "status", "FAILED");
        r.triggeredBy   = str(n, "triggeredBy", "manual");
        r.createdAt     = str(n, "createdAt");
        r.scheduledAt   = str(n, "scheduledAt");
        r.filePath      = str(n, "filePath");
        r.targetPath    = str(n, "targetPath");
        r.processedRows = lng(n, "processedRows");
        r.totalRows     = lng(n, "totalRows");
        r.created       = lng(n, "created");
        r.updated       = lng(n, "updated");
        r.failed        = lng(n, "failed");
        r.skipped       = lng(n, "skipped");
        r.rowEvents     = str(n, "rowEventsJson", "[]");
        return r;
    }

    private RunRecord fromState(CourseImportProgressStore.ProgressState state, String jobId) throws Exception {
        RunRecord r = new RunRecord();
        r.jobId         = jobId;
        r.status        = state.status;
        r.triggeredBy   = state.triggeredBy;
        r.createdAt     = state.createdAt;
        r.scheduledAt   = state.scheduledAt;
        r.filePath      = state.filePath;
        r.processedRows = state.processedRows;
        r.totalRows     = state.totalRows;
        r.created       = state.created;
        r.updated       = state.updated;
        r.failed        = state.failed;
        r.skipped       = state.skipped;
        r.rowEvents     = "[]";
        return r;
    }

    private Node findByJobId(Node parent, String jobId) {
        try {
            NodeIterator it = parent.getNodes();
            while (it.hasNext()) {
                Node n = it.nextNode();
                if (jobId.equals(str(n, "jobId"))) return n;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String str(Node n, String prop) { return str(n, prop, ""); }
    private String str(Node n, String prop, String def) {
        try { return n.hasProperty(prop) ? n.getProperty(prop).getString() : def; }
        catch (Exception e) { return def; }
    }
    private long lng(Node n, String prop) {
        try { return n.hasProperty(prop) ? n.getProperty(prop).getLong() : 0L; }
        catch (Exception e) { return 0L; }
    }
    private String trimOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
