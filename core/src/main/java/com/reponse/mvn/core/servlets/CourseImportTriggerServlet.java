package com.reponse.mvn.core.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.reponse.mvn.core.jobs.CourseImportJobConsumer;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts POST {@code /bin/academy/course-import} and enqueues a Sling Job
 * that imports course pages from a CSV or Excel (.xlsx) file in the AEM DAM.
 *
 * <p><b>POST parameters:</b>
 * <ul>
 *   <li>{@code filePath} — DAM asset path to .csv or .xlsx (required)</li>
 *   <li>{@code duplicateHandling} — SKIP | OVERRIDE | ALLOW (default: SKIP)</li>
 *   <li>{@code targetPath} — parent page path (default: /content/codehills/courses)</li>
 * </ul>
 * Returns JSON: {@code {"status":"queued","jobId":"..."}} or an error object.
 * </p>
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/academy/course-import",
        "sling.servlet.methods=POST"
    }
)
public class CourseImportTriggerServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CourseImportTriggerServlet.class);

    @Reference
    private JobManager jobManager;

    // ── POST — enqueue job ────────────────────────────────────────────────────

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter w = response.getWriter();

        String filePath      = param(request, "filePath", null);
        String dupHandling   = param(request, "duplicateHandling", "SKIP");
        String targetPath    = param(request, "targetPath", CourseImportJobConsumer.DEFAULT_TARGET_PATH);

        // Validate filePath
        if (filePath == null) {
            response.setStatus(400);
            w.print("{\"status\":\"error\",\"message\":\"filePath is required\"}");
            return;
        }
        String lower = filePath.toLowerCase();
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".csv")) {
            response.setStatus(400);
            w.print("{\"status\":\"error\",\"message\":\"filePath must end with .xlsx or .csv\"}");
            return;
        }

        // Normalize duplicate handling
        dupHandling = dupHandling.toUpperCase();
        if (!"SKIP".equals(dupHandling) && !"OVERRIDE".equals(dupHandling) && !"ALLOW".equals(dupHandling)) {
            dupHandling = "SKIP";
        }

        // Build job properties
        Map<String, Object> props = new HashMap<>();
        props.put(CourseImportJobConsumer.PROP_FILE_PATH,          filePath);
        props.put(CourseImportJobConsumer.PROP_DUPLICATE_HANDLING, dupHandling);
        props.put(CourseImportJobConsumer.PROP_TARGET_PATH,        targetPath);

        try {
            Job job = jobManager.addJob(CourseImportJobConsumer.JOB_TOPIC, props);
            if (job != null) {
                String jobId = job.getId();
                String shortId = jobId.substring(jobId.lastIndexOf('/') + 1);
                LOG.info("[QUEUED] {} | file={} mode={} target={}", shortId, filePath, dupHandling, targetPath);
                w.print("{\"status\":\"queued\",\"jobId\":\"" + escJson(job.getId()) + "\"}");
            } else {
                response.setStatus(500);
                w.print("{\"status\":\"error\",\"message\":\"JobManager returned null — is Sling Event running?\"}");
            }
        } catch (Exception e) {
            LOG.error("Failed to queue course import job", e);
            response.setStatus(500);
            w.print("{\"status\":\"error\",\"message\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String param(SlingHttpServletRequest req, String name, String defaultValue) {
        String v = req.getParameter(name);
        return (v != null && !v.trim().isEmpty()) ? v.trim() : defaultValue;
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
