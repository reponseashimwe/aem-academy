package com.reponse.mvn.core.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;

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
 * Trigger servlet for the Course CSV/Excel importer.
 *
 * <ul>
 *   <li>GET  {@code /bin/academy/course-import} — renders an HTML form</li>
 *   <li>POST {@code /bin/academy/course-import} — enqueues a Sling Job; returns JSON</li>
 * </ul>
 *
 * <p><b>POST parameters:</b>
 * <ul>
 *   <li>{@code filePath} — DAM asset path to .csv or .xlsx (required)</li>
 *   <li>{@code duplicateHandling} — SKIP | OVERRIDE | ALLOW (default: SKIP)</li>
 *   <li>{@code targetPath} — parent page path (default: /content/academy-codenova/courses)</li>
 * </ul>
 * </p>
 */
@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/academy/course-import",
        "sling.servlet.methods=GET",
        "sling.servlet.methods=POST"
    }
)
public class CourseImportTriggerServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(CourseImportTriggerServlet.class);

    @Reference
    private JobManager jobManager;

    // ── GET — HTML form ───────────────────────────────────────────────────────

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        // ?template=true → stream the DAM template CSV as a file download
        if ("true".equals(request.getParameter("template"))) {
            Resource res = request.getResourceResolver().getResource(
                "/content/dam/mvnreponse/course-import-template.csv/jcr:content/renditions/original/jcr:content");
            if (res != null) {
                InputStream in = res.adaptTo(InputStream.class);
                if (in != null) {
                    response.setContentType("text/csv;charset=UTF-8");
                    response.setHeader("Content-Disposition", "attachment; filename=\"course-import-template.csv\"");
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        response.getOutputStream().write(buf, 0, n);
                    }
                    return;
                }
            }
            response.setStatus(404);
            response.getWriter().print("Template CSV not found — deploy the ui.content package first.");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter w = response.getWriter();

        w.println("<!DOCTYPE html><html lang=\"en\">");
        w.println("<head>");
        w.println("<meta charset=\"UTF-8\"/>");
        w.println("<title>Academy Course Importer</title>");
        w.println("<style>");
        w.println("  body{font-family:sans-serif;max-width:640px;margin:48px auto;color:#222;}");
        w.println("  h1{font-size:1.4rem;margin-bottom:1.5rem;}");
        w.println("  label{display:block;margin-top:1.2rem;font-weight:600;font-size:.9rem;}");
        w.println("  input,select{width:100%;padding:8px 10px;margin-top:5px;border:1px solid #ccc;");
        w.println("               border-radius:4px;box-sizing:border-box;font-size:.95rem;}");
        w.println("  .hint{font-size:.8rem;color:#666;margin-top:3px;}");
        w.println("  button{margin-top:1.8rem;padding:10px 28px;background:#0070f3;color:#fff;");
        w.println("         border:none;border-radius:4px;cursor:pointer;font-size:1rem;}");
        w.println("  button:hover{background:#0059c1;}");
        w.println("  #result{margin-top:1.2rem;padding:10px 14px;border-radius:4px;display:none;");
        w.println("          font-family:monospace;font-size:.85rem;white-space:pre-wrap;}");
        w.println("  .ok{background:#e6f4ea;border:1px solid #a8d5b5;}");
        w.println("  .err{background:#fdecea;border:1px solid #f5c2c2;}");
        w.println("</style>");
        w.println("</head><body>");
        w.println("<h1>Academy Course Importer</h1>");
        w.println("<p style=\"margin-bottom:1.5rem;display:flex;gap:1.2rem;align-items:center\">");
        w.println("  <a href=\"/bin/academy/course-import?template=true\"");
        w.println("     style=\"font-size:.9rem;color:#0070f3;text-decoration:none;border:1px solid #0070f3;");
        w.println("            padding:5px 12px;border-radius:4px;\">");
        w.println("    &#8595; Download Template</a>");
        w.println("  <a href=\"/assets.html/content/dam/mvnreponse\" target=\"_blank\"");
        w.println("     style=\"font-size:.9rem;color:#555;text-decoration:none;border:1px solid #ccc;");
        w.println("            padding:5px 12px;border-radius:4px;\">");
        w.println("    &#128193; Open in DAM</a>");
        w.println("</p>");
        w.println("<form id=\"importForm\">");

        w.println("  <label>DAM File Path");
        w.println("    <input type=\"text\" name=\"filePath\"");
        w.println("           placeholder=\"/content/dam/mvnreponse/courses.xlsx\" required/>");
        w.println("    <div class=\"hint\">Upload the .xlsx or .csv file to the DAM first, then paste its path here.</div>");
        w.println("  </label>");

        w.println("  <label>Duplicate Handling");
        w.println("    <select name=\"duplicateHandling\">");
        w.println("      <option value=\"SKIP\" selected>SKIP — leave existing pages untouched</option>");
        w.println("      <option value=\"OVERRIDE\">OVERRIDE — update existing pages</option>");
        w.println("      <option value=\"ALLOW\">ALLOW — always create (append suffix if needed)</option>");
        w.println("    </select>");
        w.println("  </label>");

        w.println("  <label>Target Path <span style=\"font-weight:normal\">(optional)</span>");
        w.println("    <input type=\"text\" name=\"targetPath\"");
        w.println("           placeholder=\"/content/codehills/courses\"/>");
        w.println("  </label>");

        w.println("  <button type=\"submit\">Start Import</button>");
        w.println("</form>");
        w.println("<div id=\"result\"></div>");

        w.println("<script>");
        w.println("document.getElementById('importForm').addEventListener('submit', function(e) {");
        w.println("  e.preventDefault();");
        w.println("  var form = e.target;");
        w.println("  var data = new URLSearchParams(new FormData(form));");
        w.println("  var btn = form.querySelector('button');");
        w.println("  var el  = document.getElementById('result');");
        w.println("  btn.disabled = true; btn.textContent = 'Submitting\u2026';");
        w.println("  function showError(msg) {");
        w.println("    el.style.display = 'block'; el.className = 'err';");
        w.println("    el.textContent = msg;");
        w.println("    btn.disabled = false; btn.textContent = 'Start Import';");
        w.println("  }");
        w.println("  // AEM requires a CSRF token on every POST to /bin/");
        w.println("  fetch('/libs/granite/csrf/token.json')");
        w.println("    .then(function(r){ return r.json(); })");
        w.println("    .then(function(csrf){");
        w.println("      return fetch('/bin/academy/course-import', {");
        w.println("        method: 'POST',");
        w.println("        headers: { 'CSRF-Token': csrf.token },");
        w.println("        body: data");
        w.println("      });");
        w.println("    })");
        w.println("    .then(function(r){ return r.json(); })");
        w.println("    .then(function(json){");
        w.println("      el.style.display = 'block';");
        w.println("      if (json.status === 'queued') {");
        w.println("        el.className = 'ok';");
        w.println("        el.textContent = 'Job queued!\\nJob ID: ' + json.jobId");
        w.println("          + '\\n\\nMonitor progress in: crx-quickstart/logs/project-academy-reponse.log';");
        w.println("      } else {");
        w.println("        el.className = 'err';");
        w.println("        el.textContent = 'Error: ' + (json.message || JSON.stringify(json));");
        w.println("      }");
        w.println("      btn.disabled = false; btn.textContent = 'Start Import';");
        w.println("    })");
        w.println("    .catch(function(err){ showError('Request failed: ' + err); });");
        w.println("});");
        w.println("</script>");
        w.println("</body></html>");
    }

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
