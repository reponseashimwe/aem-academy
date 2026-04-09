package com.reponse.mvn.core.models;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

import com.adobe.cq.wcm.core.components.models.Image;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(
    adaptables = SlingHttpServletRequest.class,
    resourceType = {
        "academy-codenova/components/structure/course-page",
        "academy-codenova/components/atomic/course-meta"
    },
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class CourseModel {

    private static final Logger LOG = LoggerFactory.getLogger(CourseModel.class);

    private static final String COURSE_PAGE_RT   = "academy-codenova/components/structure/course-page";
    private static final String COURSE_META_RT   = "academy-codenova/components/atomic/course-meta";
    private static final String DEFAULT_DATE_FMT = "MMMM d, yyyy";
    private static final int MAX_RELATED = 6;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private QueryBuilder queryBuilder;

    @OSGiService
    private ModelFactory modelFactory;

    private String formattedDate;
    private String[] rawTagIds = new String[0];
    private List<String> tagTitles = Collections.emptyList();
    private String link = "";
    private String fileReference = "";
    private String courseAbstract = "";
    private List<CourseListModel.CourseItem> relatedCourses = Collections.emptyList();

    @PostConstruct
    protected void init() {
        Resource res = request.getResource();
        ValueMap cvm = res.getValueMap();
        TagManager tm = res.getResourceResolver().adaptTo(TagManager.class);

        String rt = res.getResourceType();
        Resource dateRes = null;
        Resource tagsRes = null;
        Resource metaRes = null;
        ValueMap pageVm = cvm;

        if (COURSE_PAGE_RT.equals(rt)) {
            metaRes = res.getChild("meta");
            dateRes = res.getChild("date");
            tagsRes = res.getChild("tags");
        } else if (COURSE_META_RT.equals(rt)) {
            metaRes = res;
            Resource parent = res.getParent();
            pageVm = parent != null ? parent.getValueMap() : ValueMap.EMPTY;
            dateRes = parent != null ? parent.getChild("date") : null;
            tagsRes = parent != null ? parent.getChild("tags") : null;
        }

        Calendar startCal = Fields.resolveStartCalendar(metaRes, dateRes, tagsRes, pageVm);
        resolveDate(startCal);
        rawTagIds = Fields.resolveTagIds(metaRes, tagsRes, pageVm);
        resolveTagLabels(rawTagIds, tm);
        link = Fields.resolveLink(metaRes, tagsRes, pageVm);

        if (COURSE_PAGE_RT.equals(res.getResourceType())) {
            Resource imageRes = res.getChild("image");
            fileReference = Fields.resolveImageUrl(
                    request, imageRes, modelFactory, res.getResourceResolver(), cvm.get("fileReference", ""));
        } else {
            fileReference = cvm.get("fileReference", "");
        }

        if (COURSE_PAGE_RT.equals(rt)) {
            courseAbstract = Fields.resolveAbstractText(res.getChild("abstract"), cvm);
        } else {
            Resource parent = res.getParent();
            Resource abstractRes = parent != null ? parent.getChild("abstract") : null;
            courseAbstract = Fields.resolveAbstractText(abstractRes, pageVm);
        }

        if (COURSE_PAGE_RT.equals(res.getResourceType()) && currentPage != null && queryBuilder != null) {
            buildRelatedCourses(res.getResourceResolver(), tm);
        }
    }

    private void resolveDate(Calendar cal) {
        if (cal != null) {
            formattedDate = new SimpleDateFormat(DEFAULT_DATE_FMT, Locale.ENGLISH).format(cal.getTime());
        }
    }

    private void resolveTagLabels(String[] ids, TagManager tm) {
        if (ids == null || ids.length == 0) {
            tagTitles = Collections.emptyList();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (String id : ids) {
            String label = id;
            if (tm != null) {
                Tag tag = tm.resolve(id);
                if (tag != null) {
                    label = tag.getTitle();
                }
            }
            labels.add(label);
        }
        tagTitles = labels;
    }

    private void buildRelatedCourses(ResourceResolver resolver, TagManager tm) {
        boolean hasTagFilter = rawTagIds.length > 0;
        Set<String> tagSet = new HashSet<>();
        for (String t : rawTagIds) {
            tagSet.add(t);
        }

        Page parent = currentPage.getParent();
        String root = parent != null ? parent.getPath() : "/content";

        Map<String, String> qm = new HashMap<>();
        qm.put("type", "cq:Page");
        qm.put("path", root);
        qm.put("1_property", "jcr:content/sling:resourceType");
        qm.put("1_property.value", COURSE_PAGE_RT);
        qm.put("p.limit", hasTagFilter ? "50" : String.valueOf(MAX_RELATED + 1));

        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);
        SimpleDateFormat humFmt = new SimpleDateFormat(DEFAULT_DATE_FMT, Locale.ENGLISH);

        try {
            Session session = resolver.adaptTo(Session.class);
            Query query = queryBuilder.createQuery(PredicateGroup.create(qm), session);
            SearchResult result = query.getResult();
            PageManager pm = resolver.adaptTo(PageManager.class);
            List<CourseListModel.CourseItem> found = new ArrayList<>();

            for (Hit hit : result.getHits()) {
                if (found.size() >= MAX_RELATED) {
                    break;
                }
                try {
                    Page course = pm != null ? pm.getPage(hit.getPath()) : null;
                    if (course == null || course.getPath().equals(currentPage.getPath())) {
                        continue;
                    }

                    Resource cc = course.getContentResource();
                    if (cc == null) {
                        continue;
                    }
                    ValueMap ccvm = cc.getValueMap();
                    Resource metaChild = cc.getChild("meta");
                    Resource tagsChild = cc.getChild("tags");
                    String[] cTags = Fields.resolveTagIds(metaChild, tagsChild, ccvm);
                    if (hasTagFilter) {
                        boolean overlap = false;
                        for (String t : cTags) {
                            if (tagSet.contains(t)) {
                                overlap = true;
                                break;
                            }
                        }
                        if (!overlap) {
                            continue;
                        }
                    }

                    List<String> tLabels = new ArrayList<>();
                    for (String t : cTags) {
                        String lbl = t;
                        if (tm != null) {
                            Tag tag = tm.resolve(t);
                            if (tag != null) {
                                lbl = tag.getTitle();
                            }
                        }
                        tLabels.add(lbl);
                    }

                    Resource dateChild = cc.getChild("date");
                    Calendar cal = Fields.resolveStartCalendar(metaChild, dateChild, tagsChild, ccvm);
                    String iso = "";
                    String human = "";
                    long startEpoch = Fields.startDateEpoch(cal);
                    if (cal != null) {
                        iso = isoFmt.format(cal.getTime());
                        human = humFmt.format(cal.getTime());
                    }

                    Resource imgChild = cc.getChild("image");
                    String img = Fields.resolveImageUrl(
                            request, imgChild, modelFactory, resolver, ccvm.get("fileReference", ""));

                    Resource abstractChild = cc.getChild("abstract");
                    String abs = Fields.resolveAbstractText(abstractChild, ccvm);
                    if (abs.isEmpty() && course.getDescription() != null) {
                        abs = course.getDescription();
                    }

                    String lnk = Fields.resolveLink(metaChild, tagsChild, ccvm);
                    if (lnk.isEmpty()) {
                        lnk = course.getPath() + ".html";
                    }

                    Resource titleChild = cc.getChild("title");
                    String courseTitle = titleChild != null
                            ? titleChild.getValueMap().get("jcr:title",
                                    course.getTitle() != null ? course.getTitle() : "")
                            : (course.getTitle() != null ? course.getTitle() : "");

                    found.add(new CourseListModel.CourseItem(
                            courseTitle, iso, human, startEpoch, abs, lnk, lnk, img, tLabels));
                } catch (Exception e) {
                    LOG.warn("CourseModel: skipping related hit", e);
                }
            }
            relatedCourses = found;
        } catch (Exception e) {
            LOG.error("CourseModel: related courses query failed", e);
        }
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public List<String> getTagTitles() {
        return tagTitles;
    }

    public String getLink() {
        return link;
    }

    public String getFileReference() {
        return fileReference;
    }

    public String getAbstract() {
        return courseAbstract;
    }

    public List<CourseListModel.CourseItem> getRelatedCourses() {
        return relatedCourses;
    }

    public boolean isRelatedEmpty() {
        return relatedCourses.isEmpty();
    }

    static final class Fields {

        private static final Logger FL = LoggerFactory.getLogger(Fields.class);

        private Fields() {
        }

        static String resolveImageUrl(SlingHttpServletRequest request, Resource imageRes,
                ModelFactory modelFactory, ResourceResolver resolver, String jcrContentFileRefFallback) {
            if (imageRes == null) {
                return mapOrEmpty(resolver, jcrContentFileRefFallback);
            }
            if (request != null && modelFactory != null) {
                try {
                    Image image = modelFactory.getModelFromWrappedRequest(request, imageRes, Image.class);
                    if (image != null && !isBlank(image.getSrc())) {
                        return image.getSrc();
                    }
                } catch (Exception e) {
                    FL.debug("resolveImageUrl: Core Image model failed for {}", imageRes.getPath(), e);
                }
            }
            ValueMap im = imageRes.getValueMap();
            String ref = im.get("fileReference", "");
            if (!isBlank(ref)) {
                return mapOrEmpty(resolver, ref);
            }
            return mapOrEmpty(resolver, jcrContentFileRefFallback);
        }

        private static boolean isBlank(String s) {
            return s == null || s.trim().isEmpty();
        }

        private static String mapOrEmpty(ResourceResolver resolver, String path) {
            if (isBlank(path)) {
                return "";
            }
            try {
                String mapped = resolver != null ? resolver.map(path) : path;
                return mapped != null ? mapped : path;
            } catch (Exception e) {
                FL.debug("mapOrEmpty failed for {}", path, e);
                return path;
            }
        }

        /** meta (page + canvas dialog) → legacy date / tags nodes → jcr:content */
        static Calendar resolveStartCalendar(Resource metaRes, Resource dateRes, Resource tagsRes, ValueMap jcrVm) {
            Calendar c = readStartDate(metaRes != null ? metaRes.getValueMap() : null);
            if (c != null) {
                return c;
            }
            c = readStartDate(dateRes != null ? dateRes.getValueMap() : null);
            if (c != null) {
                return c;
            }
            c = readStartDate(tagsRes != null ? tagsRes.getValueMap() : null);
            if (c != null) {
                return c;
            }
            return readStartDate(jcrVm);
        }

        private static Calendar readStartDate(ValueMap vm) {
            if (vm == null || !vm.containsKey("startDate")) {
                return null;
            }
            Calendar c = vm.get("startDate", Calendar.class);
            if (c != null) {
                return c;
            }
            return calendarFromIsoString(vm.get("startDate", String.class));
        }

        private static Calendar calendarFromIsoString(String raw) {
            if (isBlank(raw)) {
                return null;
            }
            String s = raw.trim();
            try {
                Instant instant = s.length() <= 10
                        ? LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant()
                        : Instant.parse(s);
                GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(instant.toEpochMilli());
                return cal;
            } catch (Exception e) {
                FL.debug("calendarFromIsoString: {}", raw, e);
                return null;
            }
        }

        static String[] resolveTagIds(Resource metaRes, Resource tagsRes, ValueMap jcrVm) {
            String[] fromPage = jcrVm.get("cq:tags", new String[0]);
            if (fromPage != null && fromPage.length > 0) {
                return fromPage;
            }
            if (metaRes != null && metaRes.getValueMap().containsKey("cq:tags")) {
                String[] t = metaRes.getValueMap().get("cq:tags", new String[0]);
                return t != null ? t : new String[0];
            }
            if (tagsRes != null) {
                return tagsRes.getValueMap().get("cq:tags", new String[0]);
            }
            return fromPage != null ? fromPage : new String[0];
        }

        static String resolveLink(Resource metaRes, Resource tagsRes, ValueMap jcrVm) {
            if (metaRes != null) {
                String l = metaRes.getValueMap().get("link", "");
                if (!isBlank(l)) {
                    return l;
                }
            }
            if (tagsRes != null) {
                String l = tagsRes.getValueMap().get("link", "");
                if (!isBlank(l)) {
                    return l;
                }
            }
            return jcrVm.get("link", "");
        }

        static String resolveAbstractText(Resource abstractRes, ValueMap jcrVm) {
            String abstractText = "";
            if (abstractRes != null) {
                abstractText = abstractRes.getValueMap().get("text", "");
                abstractText = abstractText.replaceAll("<[^>]*>", "").trim();
            }
            if (abstractText.isEmpty()) {
                abstractText = jcrVm.get("abstract", "");
            }
            return abstractText;
        }

        static long startDateEpoch(Calendar cal) {
            return cal != null ? cal.getTimeInMillis() : Long.MIN_VALUE;
        }
    }
}
