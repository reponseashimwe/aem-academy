package com.reponse.mvn.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Returns the last 50 course import run records as a JSON array, newest first.
 */
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
    private static final int    MAX_RECORDS    = 50;

    @Reference
    private SlingRepository repository;

    @Override
    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse res)
            throws ServletException, IOException {

        res.setContentType("application/json;charset=UTF-8");
        PrintWriter w = res.getWriter();

        Session s = null;
        try {
            @SuppressWarnings("deprecation")
            Session adminSession = repository.loginAdministrative(null);
            s = adminSession;

            if (!s.nodeExists(HISTORY_PARENT)) {
                w.print("[]");
                return;
            }

            Node parent = s.getNode(HISTORY_PARENT);
            NodeIterator it = parent.getNodes();

            List<String> names = new ArrayList<>();
            while (it.hasNext()) {
                names.add(it.nextNode().getName());
            }
            Collections.sort(names, Collections.reverseOrder());

            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (String name : names) {
                if (count >= MAX_RECORDS) break;
                if (count > 0) sb.append(',');

                Node run = parent.getNode(name);
                sb.append('{');
                appendStr(sb, "status",      prop(run, "status",      "FAILED")); sb.append(',');
                appendStr(sb, "triggeredBy", prop(run, "triggeredBy", "manual")); sb.append(',');
                appendStr(sb, "createdAt",   prop(run, "createdAt",   ""));       sb.append(',');
                appendStr(sb, "scheduledAt", prop(run, "scheduledAt", ""));       sb.append(',');
                appendStr(sb, "filePath",    prop(run, "filePath",    ""));       sb.append(',');
                appendStr(sb, "targetPath",  prop(run, "targetPath",  ""));       sb.append(',');
                appendLong(sb, "created",    longProp(run, "created"));           sb.append(',');
                appendLong(sb, "updated",    longProp(run, "updated"));           sb.append(',');
                appendLong(sb, "failed",     longProp(run, "failed"));            sb.append(',');
                appendLong(sb, "skipped",    longProp(run, "skipped"));
                sb.append('}');
                count++;
            }
            sb.append(']');
            w.print(sb.toString());

        } catch (Exception e) {
            LOG.error("[HISTORY] Failed to read run history", e);
            res.setStatus(500);
            w.print("{\"error\":\"" + esc(e.getMessage()) + "\"}");
        } finally {
            if (s != null) s.logout();
        }
    }

    private String prop(Node node, String name, String def) {
        try {
            return node.hasProperty(name) ? node.getProperty(name).getString() : def;
        } catch (Exception e) {
            return def;
        }
    }

    private long longProp(Node node, String name) {
        try {
            return node.hasProperty(name) ? node.getProperty(name).getLong() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private void appendStr(StringBuilder sb, String key, String value) {
        sb.append('"').append(key).append("\":\"").append(esc(value)).append('"');
    }

    private void appendLong(StringBuilder sb, String key, long value) {
        sb.append('"').append(key).append("\":").append(value);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
