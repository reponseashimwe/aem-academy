package com.reponse.mvn.core.jobs;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reponse.mvn.core.jobs.data.RowEvent;
import com.reponse.mvn.core.jobs.importer.CsvParser;
import com.reponse.mvn.core.jobs.importer.XlsxParser;
import com.reponse.mvn.core.jobs.utils.ImportUtils;

import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = JobConsumer.class,
    property = { JobConsumer.PROPERTY_TOPICS + "=com/reponse/mvn/course/import" }
)
public class CourseImportJobConsumer implements JobConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(CourseImportJobConsumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String DEFAULT_TARGET_PATH = "/content/codehills/courses";
    private static final String HISTORY_PARENT = "/var/reponse/course-import-history";

    @Reference
    private SlingRepository repository;

    @Reference
    private CourseImportProgressStore progressStore;

    @Override
    public JobResult process(Job job) {
        String filePath   = job.getProperty("filePath", String.class);
        String dupMode    = ImportUtils.nvl(job.getProperty("duplicateHandling", String.class), "SKIP");
        String targetPath = ImportUtils.nvl(job.getProperty("targetPath", String.class), DEFAULT_TARGET_PATH);
        String jobId      = safe(job != null ? job.getId() : null);
        String triggeredBy  = job != null ? ImportUtils.nvl(job.getProperty("triggeredBy",  String.class), "manual") : "manual";
        String scheduledAt  = job != null ? ImportUtils.nvl(job.getProperty("scheduledAt",  String.class), "")       : "";
        Calendar jobCreated = job != null ? job.getCreated() : null;
        String createdAt  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                                .format(jobCreated != null ? jobCreated.getTime() : new Date());

        int created = 0, updated = 0, failed = 0, skipped = 0;
        List<RowEvent> rowEvents  = new ArrayList<>();
        String historyNodePath    = null;

        if (filePath == null || filePath.trim().isEmpty()) {
            LOG.error("[FAIL] No filePath in job properties");
            return JobResult.FAILED;
        }
        filePath  = filePath.trim();
        dupMode   = dupMode.trim().toUpperCase(Locale.ENGLISH);
        targetPath = targetPath.trim();
        if (!"SKIP".equals(dupMode) && !"OVERRIDE".equals(dupMode) && !"ALLOW".equals(dupMode)) {
            dupMode = "SKIP";
        }

        Session session = null;
        try {
            historyNodePath = initHistory(job, filePath, targetPath);
            progressStore.put(jobId, new CourseImportProgressStore.ProgressState(
                "QUEUED", 0, 0, 0, 0, 0, 0, filePath, triggeredBy, createdAt, scheduledAt));

            @SuppressWarnings("deprecation")
            Session adminSession = repository.loginAdministrative(null);
            session = adminSession;

            if (!session.nodeExists(targetPath)) {
                LOG.error("[FAIL] Target path does not exist: {}", targetPath);
                updateHistory(historyNodePath, "FAILED", 0, 0, 0, 1, 0, 0, "[]");
                progressStore.remove(jobId);
                return JobResult.FAILED;
            }
            InputStream stream = getAssetStream(session, filePath);
            if (stream == null) {
                LOG.error("[FAIL] Asset not found or unreadable: {}", filePath);
                updateHistory(historyNodePath, "FAILED", 0, 0, 0, 1, 0, 0, "[]");
                progressStore.remove(jobId);
                return JobResult.FAILED;
            }

            List<String[]> rawRows;
            String lower = filePath.toLowerCase(Locale.ENGLISH);
            if (lower.endsWith(".csv")) {
                rawRows = CsvParser.parse(stream);
            } else if (lower.endsWith(".xlsx")) {
                rawRows = XlsxParser.parse(stream);
            } else {
                LOG.error("[FAIL] Unsupported file type: {}", filePath);
                updateHistory(historyNodePath, "FAILED", 0, 0, 0, 1, 0, 0, "[]");
                progressStore.remove(jobId);
                return JobResult.FAILED;
            }

            List<Map<String, String>> rows = ImportUtils.rowsToMaps(rawRows);
            progressStore.put(jobId, new CourseImportProgressStore.ProgressState(
                "RUNNING", 0, rows.size(), 0, 0, 0, 0, filePath, triggeredBy, createdAt, scheduledAt));

            if (rows.isEmpty()) {
                LOG.warn("[FILE] rows=0 — nothing to import from {}", filePath);
                updateHistory(historyNodePath, "COMPLETED", 0, 0, 0, 0, 0, 0, "[]");
                progressStore.remove(jobId);
                return JobResult.OK;
            }

            LOG.info("[FILE] rows={} columns={}", rows.size(), rows.get(0).size());

            for (int i = 0; i < rows.size(); i++) {
                int rowNum = i + 2;
                Map<String, String> row = rows.get(i);
                String rowTitle = ImportUtils.nvl(row.get("title"), "").trim();
                try {
                    RowOutcome outcome = processRow(session, row, rowNum, targetPath, dupMode);
                    switch (outcome.result) {
                        case CREATED: created++; break;
                        case UPDATED: updated++; break;
                        case SKIPPED: skipped++; break;
                        default:      failed++;  break;
                    }
                    rowEvents.add(new RowEvent(rowNum, rowTitle, outcome.result.name(), outcome.message));
                } catch (Exception e) {
                    failed++;
                    LOG.warn("[ROW {}][FAIL] Unexpected error — {}", rowNum, e.getMessage());
                    rowEvents.add(new RowEvent(rowNum, rowTitle, "FAILED", "Unexpected error: " + e.getMessage()));
                }
                progressStore.put(jobId, new CourseImportProgressStore.ProgressState(
                    "RUNNING", i + 1, rows.size(), created, updated, failed, skipped,
                    filePath, triggeredBy, createdAt, scheduledAt));
            }

            session.save();
            LOG.info("[DONE] created={} updated={} skipped={} failed={} total={}",
                     created, updated, skipped, failed, rows.size());

            String rowEventsJson = toRowEventsJson(rowEvents);
            updateHistory(historyNodePath, "COMPLETED", rows.size(), created, updated, failed, skipped, rows.size(), rowEventsJson);
            progressStore.remove(jobId);
            return JobResult.OK;

        } catch (Exception e) {
            LOG.error("[FAIL] Fatal error in CourseImportJob — {}", e.getMessage(), e);
            updateHistory(historyNodePath, "FAILED", -1, created, updated, failed, skipped, -1, toRowEventsJson(rowEvents));
            progressStore.remove(jobId);
            return JobResult.FAILED;
        } finally {
            if (session != null) session.logout();
        }
    }

    private String initHistory(Job job, String filePath, String targetPath) {
        Session s = null;
        try {
            @SuppressWarnings("deprecation")
            Session adminSession = repository.loginAdministrative(null);
            s = adminSession;
            if (!s.nodeExists("/var/reponse")) {
                s.getNode("/var").addNode("reponse", "nt:unstructured");
                s.save();
            }
            if (!s.nodeExists(HISTORY_PARENT)) {
                s.getNode("/var/reponse").addNode("course-import-history", "nt:unstructured");
                s.save();
            }
            String iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
            String nodeName = "run-" + System.currentTimeMillis();
            Node run = s.getNode(HISTORY_PARENT).addNode(nodeName, "nt:unstructured");
            run.setProperty("jobId", safe(job != null ? job.getId() : null));
            run.setProperty("status", "QUEUED");
            run.setProperty("processedRows", 0L);
            run.setProperty("totalRows", 0L);
            run.setProperty("created", 0L);
            run.setProperty("updated", 0L);
            run.setProperty("failed", 0L);
            run.setProperty("skipped", 0L);
            run.setProperty("rowEventsJson", "[]");
            Calendar jobCreated = job.getCreated();
            run.setProperty("createdAt", jobCreated != null
                ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(jobCreated.getTime()) : iso);
            String scheduledAt = job != null ? job.getProperty("scheduledAt", String.class) : null;
            String triggeredBy = job != null ? job.getProperty("triggeredBy", String.class) : null;
            run.setProperty("scheduledAt", scheduledAt != null ? scheduledAt : "");
            run.setProperty("triggeredBy", triggeredBy != null ? triggeredBy : "manual");
            run.setProperty("filePath", filePath != null ? filePath : "");
            run.setProperty("targetPath", targetPath != null ? targetPath : "");
            s.save();
            LOG.info("[HISTORY] Initialized run {} — jobId={}", nodeName, safe(job != null ? job.getId() : null));
            return HISTORY_PARENT + "/" + nodeName;
        } catch (Exception e) {
            LOG.warn("[HISTORY] Failed to initialize run record: {}", e.getMessage());
            return null;
        } finally {
            if (s != null) s.logout();
        }
    }

    private void updateHistory(String historyNodePath, String status, int processedRows, int created,
                               int updated, int failed, int skipped, int totalRows, String rowEventsJson) {
        if (historyNodePath == null) return;
        Session s = null;
        try {
            @SuppressWarnings("deprecation")
            Session adminSession = repository.loginAdministrative(null);
            s = adminSession;
            if (!s.nodeExists(historyNodePath)) return;
            Node node = s.getNode(historyNodePath);
            node.setProperty("status", status);
            if (processedRows >= 0) node.setProperty("processedRows", (long) processedRows);
            if (totalRows >= 0) node.setProperty("totalRows", (long) totalRows);
            node.setProperty("created", (long) created);
            node.setProperty("updated", (long) updated);
            node.setProperty("failed", (long) failed);
            node.setProperty("skipped", (long) skipped);
            node.setProperty("rowEventsJson", rowEventsJson != null ? rowEventsJson : "[]");
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                node.setProperty("completedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            }
            node.setProperty("updatedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date()));
            s.save();
        } catch (Exception e) {
            LOG.warn("[HISTORY] Could not update run record: {}", e.getMessage());
        } finally {
            if (s != null) s.logout();
        }
    }

    private InputStream getAssetStream(Session session, String filePath) {
        try {
            String renditionPath = filePath + "/jcr:content/renditions/original/jcr:content";
            if (session.nodeExists(renditionPath)) {
                Property dataProp = session.getNode(renditionPath).getProperty("jcr:data");
                Binary binary = dataProp.getBinary();
                return binary.getStream();
            }
            String ntFilePath = filePath + "/jcr:content";
            if (session.nodeExists(ntFilePath)) {
                Node content = session.getNode(ntFilePath);
                if (content.hasProperty("jcr:data")) {
                    return content.getProperty("jcr:data").getBinary().getStream();
                }
            }
            return null;
        } catch (Exception e) {
            LOG.error("[FAIL] Cannot read asset at {}: {}", filePath, e.getMessage());
            return null;
        }
    }
    private enum RowResult { CREATED, UPDATED, SKIPPED, FAILED }

    private static final class RowOutcome {
        private final RowResult result;
        private final String message;

        private RowOutcome(RowResult result, String message) {
            this.result = result;
            this.message = message;
        }
    }

    private RowOutcome processRow(Session session, Map<String, String> row, int rowNum,
                                  String targetPath, String mode) throws RepositoryException {
        String title = row.getOrDefault("title", "").trim();
        String startDateStr = row.getOrDefault("startdate", "").trim();
        String abstractText = row.getOrDefault("abstract", "").trim();
        String tagsRaw = row.getOrDefault("tags", "").trim();
        String link = row.getOrDefault("link", "").trim();
        String fileRef = row.getOrDefault("filereference", "").trim();
        if (title.isEmpty()) {
            LOG.warn("[ROW {}][FAIL] Missing required field: title", rowNum);
            return new RowOutcome(RowResult.FAILED, "Missing required field: title");
        }
        Calendar startDate = null;
        if (!startDateStr.isEmpty()) {
            startDate = ImportUtils.parseDate(startDateStr);
            if (startDate == null) {
                LOG.warn("[ROW {}][FAIL] Cannot parse startDate '{}'", rowNum, startDateStr);
                return new RowOutcome(RowResult.FAILED, "Cannot parse startDate '" + startDateStr + "'");
            }
        }
        String[] tags = ImportUtils.normalizeTags(tagsRaw);
        for (String tag : tags) ensureTag(session, tag);
        String slug = ImportUtils.generateSlug(title);
        String pagePath = targetPath + "/" + slug;
        boolean exists = session.nodeExists(pagePath);
        if (exists) {
            switch (mode) {
                case "OVERRIDE":
                    updatePage(session, pagePath, title, startDate, abstractText, tags, link, fileRef);
                    LOG.info("[ROW {}][SUCCESS] Updated: {}", rowNum, slug);
                    return new RowOutcome(RowResult.UPDATED, "Updated " + slug);
                case "ALLOW":
                    pagePath = ImportUtils.findUniquePagePath(session, targetPath, slug);
                    createPage(session, pagePath, title, startDate, abstractText, tags, link, fileRef);
                    String allowSlug = pagePath.substring(pagePath.lastIndexOf('/') + 1);
                    LOG.info("[ROW {}][SUCCESS] Created: {} (suffixed)", rowNum, allowSlug);
                    String allowMsg = "Created " + allowSlug;
                    if (!fileRef.isEmpty() && !session.nodeExists(fileRef)) {
                        allowMsg += " \u2014 image not found in DAM";
                    }
                    return new RowOutcome(RowResult.CREATED, allowMsg);
                default:
                    LOG.info("[ROW {}][SKIPPED] {}", rowNum, slug);
                    return new RowOutcome(RowResult.SKIPPED, "Page already exists");
            }
        }
        createPage(session, pagePath, title, startDate, abstractText, tags, link, fileRef);
        LOG.info("[ROW {}][SUCCESS] Created: {}", rowNum, slug);
        String createMsg = "Created " + slug;
        if (!fileRef.isEmpty() && !session.nodeExists(fileRef)) {
            createMsg += " \u2014 image not found in DAM";
        }
        return new RowOutcome(RowResult.CREATED, createMsg);
    }

    private void createPage(Session session, String pagePath, String title, Calendar startDate,
                            String abstractText, String[] tags, String link, String fileRef)
            throws RepositoryException {
        String parentPath = pagePath.substring(0, pagePath.lastIndexOf('/'));
        String pageName = pagePath.substring(pagePath.lastIndexOf('/') + 1);
        Node parent = session.getNode(parentPath);
        Node page = parent.addNode(pageName, "cq:Page");
        Node content = page.addNode("jcr:content", "cq:PageContent");
        content.setProperty("jcr:title", title);
        content.setProperty("sling:resourceType", "academy-codenova/components/structure/course-page");
        content.setProperty("cq:template", "/apps/academy-codenova/templates/course-page");
        applyCourseContent(content, title, startDate, abstractText, tags, link, fileRef);
    }

    private void updatePage(Session session, String pagePath, String title, Calendar startDate,
                            String abstractText, String[] tags, String link, String fileRef)
            throws RepositoryException {
        Node content = session.getNode(pagePath + "/jcr:content");
        content.setProperty("jcr:title", title);
        applyCourseContent(content, title, startDate, abstractText, tags, link, fileRef);
    }

    private void applyCourseContent(Node content, String title, Calendar startDate, String abstractText,
                                    String[] tags, String link, String fileRef)
            throws RepositoryException {
        Node breadcrumbNode = getOrCreate(content, "breadcrumb");
        breadcrumbNode.setProperty("sling:resourceType", "academy-codenova/components/atomic/breadcrumb");
        breadcrumbNode.setProperty("startLevel", 1L);
        Node imageNode = getOrCreate(content, "image");
        imageNode.setProperty("sling:resourceType", "academy-codenova/components/atomic/image");
        if (!fileRef.isEmpty()) imageNode.setProperty("fileReference", fileRef);
        Node metaNode = getOrCreate(content, "meta");
        metaNode.setProperty("sling:resourceType", "academy-codenova/components/atomic/course-meta");
        if (startDate != null) metaNode.setProperty("startDate", startDate);
        if (tags.length > 0) metaNode.setProperty("cq:tags", tags);
        if (!link.isEmpty()) metaNode.setProperty("link", link);
        Node titleNode = getOrCreate(content, "title");
        titleNode.setProperty("sling:resourceType", "academy-codenova/components/atomic/title");
        titleNode.setProperty("type", "h1");
        titleNode.setProperty("typographyVariant", "title-3");
        titleNode.setProperty("jcr:title", title);
        titleNode.setProperty("text", title);
        Node abstractNode = getOrCreate(content, "abstract");
        abstractNode.setProperty("sling:resourceType", "academy-codenova/components/atomic/text");
        abstractNode.setProperty("text", ImportUtils.toHtml(abstractText));
        Node parsysNode = getOrCreate(content, "parsys");
        parsysNode.setProperty("sling:resourceType", "wcm/foundation/components/parsys");
    }

    private Node getOrCreate(Node parent, String name) throws RepositoryException {
        if (parent.hasNode(name)) return parent.getNode(name);
        return parent.addNode(name, "nt:unstructured");
    }

    private void ensureTag(Session session, String tagId) {
        if (tagId == null) return;
        String normalizedTag = tagId.trim();
        if (normalizedTag.isEmpty()) return;
        int colonIdx = normalizedTag.indexOf(':');
        if (colonIdx <= 0) {
            LOG.warn("[TAG] Skipping invalid tag ID (missing namespace): '{}'", normalizedTag);
            return;
        }
        try {
            if (!session.nodeExists("/content/cq:tags")) {
                LOG.warn("[TAG] /content/cq:tags not found — skipping tag creation");
                return;
            }
            String namespace = normalizedTag.substring(0, colonIdx);
            String[] parts = normalizedTag.substring(colonIdx + 1).split("/");
            String current = "/content/cq:tags/" + namespace;
            createTagNode(session, current, namespace);
            for (String part : parts) {
                if (!part.isEmpty()) {
                    current = current + "/" + part;
                    createTagNode(session, current, part);
                }
            }
        } catch (RepositoryException e) {
            LOG.warn("[TAG] Failed to ensure tag '{}': {}", normalizedTag, e.getMessage());
        }
    }

    private void createTagNode(Session session, String path, String nameSegment) throws RepositoryException {
        if (session.nodeExists(path)) return;
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        if (!session.nodeExists(parentPath)) return;
        String nodeName = path.substring(path.lastIndexOf('/') + 1);
        Node tag = session.getNode(parentPath).addNode(nodeName, "cq:Tag");
        String title = nameSegment.isEmpty() ? nameSegment
            : Character.toUpperCase(nameSegment.charAt(0)) + nameSegment.substring(1).replace('-', ' ');
        tag.setProperty("jcr:title", title);
        LOG.info("[TAG] Created {}", path);
    }

    private String toRowEventsJson(List<RowEvent> rowEvents) {
        try {
            return MAPPER.writeValueAsString(rowEvents);
        } catch (Exception e) {
            LOG.warn("[HISTORY] Could not serialize row events: {}", e.getMessage());
            return "[]";
        }
    }

    private static String safe(String s) { return s != null ? s : ""; }
}
