package com.reponse.mvn.core.jobs;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

/**
 * Sling Job Consumer — imports course pages from a CSV or Excel (.xlsx) file in the AEM DAM.
 */
@Component(
    service = JobConsumer.class,
    property = { JobConsumer.PROPERTY_TOPICS + "=" + CourseImportJobConsumer.JOB_TOPIC }
)
public class CourseImportJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(CourseImportJobConsumer.class);

    // ── Job topic & property keys ─────────────────────────────────────────────

    public static final String JOB_TOPIC = "com/reponse/mvn/course/import";

    public static final String PROP_FILE_PATH          = "filePath";
    public static final String PROP_DUPLICATE_HANDLING = "duplicateHandling";
    public static final String PROP_TARGET_PATH        = "targetPath";

    public static final String DEFAULT_TARGET_PATH = "/content/codehills/courses";

    // ── JCR & component constants ─────────────────────────────────────────────

    private static final String TYPE_CQ_PAGE         = "cq:Page";
    private static final String TYPE_CQ_PAGE_CONTENT = "cq:PageContent";
    private static final String TYPE_NT_UNSTRUCTURED  = "nt:unstructured";

    private static final String RT_COURSE_PAGE = "academy-codenova/components/structure/course-page";
    private static final String RT_BREADCRUMB  = "academy-codenova/components/atomic/breadcrumb";
    private static final String RT_IMAGE       = "academy-codenova/components/atomic/image";
    private static final String RT_COURSE_META = "academy-codenova/components/atomic/course-meta";
    private static final String RT_TITLE       = "academy-codenova/components/atomic/title";
    private static final String RT_TEXT        = "academy-codenova/components/atomic/text";
    private static final String RT_PARSYS      = "wcm/foundation/components/parsys";
    private static final String CQ_TEMPLATE    = "/apps/academy-codenova/templates/course-page";

    // ── OSGi reference ────────────────────────────────────────────────────────

    @Reference
    private SlingRepository repository;

    // ── JobConsumer ───────────────────────────────────────────────────────────

    @Override
    public JobResult process(Job job) {
        String filePath   = job.getProperty(PROP_FILE_PATH, String.class);
        String dupMode    = ImportUtils.nvl(job.getProperty(PROP_DUPLICATE_HANDLING, String.class), "SKIP");
        String targetPath = ImportUtils.nvl(job.getProperty(PROP_TARGET_PATH, String.class), DEFAULT_TARGET_PATH);

        if (filePath == null || filePath.trim().isEmpty()) {
            LOG.error("[FAIL] No filePath in job properties");
            return JobResult.FAILED;
        }
        filePath   = filePath.trim();
        dupMode    = dupMode.trim().toUpperCase(Locale.ENGLISH);
        targetPath = targetPath.trim();

        if (!"SKIP".equals(dupMode) && !"OVERRIDE".equals(dupMode) && !"ALLOW".equals(dupMode)) {
            dupMode = "SKIP";
        }

        Session session = null;
        try {
            @SuppressWarnings("deprecation")
            Session adminSession = repository.loginAdministrative(null);
            session = adminSession;

            if (!session.nodeExists(targetPath)) {
                LOG.error("[FAIL] Target path does not exist: {}", targetPath);
                return JobResult.FAILED;
            }

            InputStream stream = getAssetStream(session, filePath);
            if (stream == null) {
                LOG.error("[FAIL] Asset not found or unreadable: {}", filePath);
                return JobResult.FAILED;
            }

            List<String[]> rawRows;
            String lower = filePath.toLowerCase(Locale.ENGLISH);
            if (lower.endsWith(".csv")) {
                rawRows = CsvParser.parse(stream);
            } else if (lower.endsWith(".xlsx")) {
                rawRows = XlsxParser.parse(stream);
            } else {
                LOG.error("[FAIL] Unsupported file type (expected .csv or .xlsx): {}", filePath);
                return JobResult.FAILED;
            }

            List<Map<String, String>> rows = ImportUtils.rowsToMaps(rawRows);
            if (rows.isEmpty()) {
                LOG.warn("[FILE] rows=0 — nothing to import from {}", filePath);
                return JobResult.OK;
            }

            LOG.info("[FILE] rows={} columns={}", rows.size(), rows.get(0).size());

            int created = 0, updated = 0, skipped = 0, failed = 0;
            for (int i = 0; i < rows.size(); i++) {
                int rowNum = i + 2;
                try {
                    switch (processRow(session, rows.get(i), rowNum, targetPath, dupMode)) {
                        case CREATED: created++; break;
                        case UPDATED: updated++; break;
                        case SKIPPED: skipped++; break;
                        default:      failed++;  break;
                    }
                } catch (Exception e) {
                    failed++;
                    LOG.warn("[ROW {}][FAIL] Unexpected error — {}", rowNum, e.getMessage());
                }
            }

            session.save();
            LOG.info("[DONE] created={} updated={} skipped={} failed={} total={}",
                     created, updated, skipped, failed, rows.size());
            return JobResult.OK;

        } catch (Exception e) {
            LOG.error("[FAIL] Fatal error in CourseImportJob — {}", e.getMessage(), e);
            return JobResult.FAILED;
        } finally {
            if (session != null) session.logout();
        }
    }

    // ── Asset stream resolution ───────────────────────────────────────────────

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

    // ── Row processing ────────────────────────────────────────────────────────

    private enum RowResult { CREATED, UPDATED, SKIPPED, FAILED }

    private RowResult processRow(Session session, Map<String, String> row, int rowNum,
                                  String targetPath, String mode) throws RepositoryException {
        String title        = row.getOrDefault("title", "").trim();
        String startDateStr = row.getOrDefault("startdate", "").trim();
        String abstractText = row.getOrDefault("abstract", "").trim();
        String tagsRaw      = row.getOrDefault("tags", "").trim();
        String link         = row.getOrDefault("link", "").trim();
        String fileRef      = row.getOrDefault("filereference", "").trim();

        if (title.isEmpty()) {
            LOG.warn("[ROW {}][FAIL] Missing required field: title", rowNum);
            return RowResult.FAILED;
        }

        Calendar startDate = null;
        if (!startDateStr.isEmpty()) {
            startDate = ImportUtils.parseDate(startDateStr);
            if (startDate == null) {
                LOG.warn("[ROW {}][FAIL] Cannot parse startDate '{}'", rowNum, startDateStr);
                return RowResult.FAILED;
            }
        }

        String[] tags = ImportUtils.normalizeTags(tagsRaw);
        for (String tag : tags) {
            ensureTag(session, tag);
        }

        String slug     = ImportUtils.generateSlug(title);
        String pagePath = targetPath + "/" + slug;
        boolean exists  = session.nodeExists(pagePath);

        if (exists) {
            switch (mode) {
                case "OVERRIDE":
                    updatePage(session, pagePath, title, startDate, abstractText, tags, link, fileRef);
                    LOG.info("[ROW {}][SUCCESS] Updated: {}", rowNum, slug);
                    return RowResult.UPDATED;
                case "ALLOW":
                    pagePath = ImportUtils.findUniquePagePath(session, targetPath, slug);
                    createPage(session, pagePath, title, startDate, abstractText, tags, link, fileRef);
                    LOG.info("[ROW {}][SUCCESS] Created: {} (suffixed)", rowNum,
                             pagePath.substring(pagePath.lastIndexOf('/') + 1));
                    return RowResult.CREATED;
                default:
                    LOG.info("[ROW {}][SKIPPED] {}", rowNum, slug);
                    return RowResult.SKIPPED;
            }
        }

        createPage(session, pagePath, title, startDate, abstractText, tags, link, fileRef);
        LOG.info("[ROW {}][SUCCESS] Created: {}", rowNum, slug);
        return RowResult.CREATED;
    }

    // ── JCR page creation ─────────────────────────────────────────────────────

    private void createPage(Session session, String pagePath, String title, Calendar startDate,
                             String abstractText, String[] tags, String link, String fileRef)
            throws RepositoryException {

        String parentPath = pagePath.substring(0, pagePath.lastIndexOf('/'));
        String pageName   = pagePath.substring(pagePath.lastIndexOf('/') + 1);

        Node parent  = session.getNode(parentPath);
        Node page    = parent.addNode(pageName, TYPE_CQ_PAGE);
        Node content = page.addNode("jcr:content", TYPE_CQ_PAGE_CONTENT);

        content.setProperty("jcr:title", title);
        content.setProperty("sling:resourceType", RT_COURSE_PAGE);
        content.setProperty("cq:template", CQ_TEMPLATE);

        Node breadcrumbNode = content.addNode("breadcrumb", TYPE_NT_UNSTRUCTURED);
        breadcrumbNode.setProperty("sling:resourceType", RT_BREADCRUMB);
        breadcrumbNode.setProperty("startLevel", 1L);

        Node imageNode = content.addNode("image", TYPE_NT_UNSTRUCTURED);
        imageNode.setProperty("sling:resourceType", RT_IMAGE);
        if (!fileRef.isEmpty()) imageNode.setProperty("fileReference", fileRef);

        Node metaNode = content.addNode("meta", TYPE_NT_UNSTRUCTURED);
        metaNode.setProperty("sling:resourceType", RT_COURSE_META);
        if (startDate != null) metaNode.setProperty("startDate", startDate);
        if (tags.length > 0) metaNode.setProperty("cq:tags", tags);
        if (!link.isEmpty()) metaNode.setProperty("link", link);

        Node titleNode = content.addNode("title", TYPE_NT_UNSTRUCTURED);
        titleNode.setProperty("sling:resourceType", RT_TITLE);
        titleNode.setProperty("type", "h1");
        titleNode.setProperty("typographyVariant", "title-3");
        titleNode.setProperty("jcr:title", title);
        titleNode.setProperty("text", title);

        Node abstractNode = content.addNode("abstract", TYPE_NT_UNSTRUCTURED);
        abstractNode.setProperty("sling:resourceType", RT_TEXT);
        abstractNode.setProperty("text", ImportUtils.toHtml(abstractText));

        Node parsysNode = content.addNode("parsys", TYPE_NT_UNSTRUCTURED);
        parsysNode.setProperty("sling:resourceType", RT_PARSYS);
    }

    private void updatePage(Session session, String pagePath, String title, Calendar startDate,
                             String abstractText, String[] tags, String link, String fileRef)
            throws RepositoryException {

        Node content = session.getNode(pagePath + "/jcr:content");
        content.setProperty("jcr:title", title);

        Node breadcrumbNode = getOrCreate(content, "breadcrumb");
        breadcrumbNode.setProperty("sling:resourceType", RT_BREADCRUMB);
        breadcrumbNode.setProperty("startLevel", 1L);

        Node imageNode = getOrCreate(content, "image");
        imageNode.setProperty("sling:resourceType", RT_IMAGE);
        if (!fileRef.isEmpty()) imageNode.setProperty("fileReference", fileRef);

        Node metaNode = getOrCreate(content, "meta");
        metaNode.setProperty("sling:resourceType", RT_COURSE_META);
        if (startDate != null) metaNode.setProperty("startDate", startDate);
        if (tags.length > 0) metaNode.setProperty("cq:tags", tags);
        if (!link.isEmpty()) metaNode.setProperty("link", link);

        Node titleNode = getOrCreate(content, "title");
        titleNode.setProperty("sling:resourceType", RT_TITLE);
        titleNode.setProperty("type", "h1");
        titleNode.setProperty("typographyVariant", "title-3");
        titleNode.setProperty("jcr:title", title);
        titleNode.setProperty("text", title);

        Node abstractNode = getOrCreate(content, "abstract");
        abstractNode.setProperty("sling:resourceType", RT_TEXT);
        abstractNode.setProperty("text", ImportUtils.toHtml(abstractText));

        if (!content.hasNode("parsys")) {
            content.addNode("parsys", TYPE_NT_UNSTRUCTURED)
                   .setProperty("sling:resourceType", RT_PARSYS);
        }
    }

    private Node getOrCreate(Node parent, String name) throws RepositoryException {
        return parent.hasNode(name) ? parent.getNode(name) : parent.addNode(name, TYPE_NT_UNSTRUCTURED);
    }

    // ── Tag auto-creation ─────────────────────────────────────────────────────

    private void ensureTag(Session session, String tagId) {
        if (tagId == null || tagId.trim().isEmpty()) return;
        tagId = tagId.trim();
        int colonIdx = tagId.indexOf(':');
        if (colonIdx <= 0) {
            LOG.warn("[TAG] Skipping invalid tag ID (missing namespace): '{}'", tagId);
            return;
        }
        try {
            if (!session.nodeExists("/content/cq:tags")) {
                LOG.warn("[TAG] /content/cq:tags not found — skipping tag creation");
                return;
            }
            String namespace  = tagId.substring(0, colonIdx);
            String[] parts    = tagId.substring(colonIdx + 1).split("/");
            String current    = "/content/cq:tags/" + namespace;
            createTagNode(session, current, namespace);
            for (String part : parts) {
                if (!part.isEmpty()) {
                    current = current + "/" + part;
                    createTagNode(session, current, part);
                }
            }
        } catch (RepositoryException e) {
            LOG.warn("[TAG] Failed to ensure tag '{}': {}", tagId, e.getMessage());
        }
    }

    private void createTagNode(Session session, String path, String nameSegment)
            throws RepositoryException {
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

}
