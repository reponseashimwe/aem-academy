package com.reponse.mvn.core.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
    adaptables = SlingHttpServletRequest.class,
    resourceType = {
        "academy-codenova/components/content/news",
        "academy-codenova/components/atomic/slider"
    },
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(name = "jackson", extensions = "json", selector = "courses")
public class CourseListModel {

    private static final Logger LOG = LoggerFactory.getLogger(CourseListModel.class);

    private static final String DEFAULT_COURSES_ROOT = "/content/codehills/courses";
    private static final String COURSE_RESOURCE_TYPE  = "academy-codenova/components/structure/course-page";
    private static final String SLIDER_RESOURCE_TYPE  = "academy-codenova/components/atomic/slider";

    @OSGiService
    private QueryBuilder queryBuilder;

    @Self
    private SlingHttpServletRequest request;

    private List<CourseItem> courses = new ArrayList<>();
    private boolean showSortControl;
    private boolean showTagFilter;
    private boolean hideDots;
    private boolean hideNavigation;

    @PostConstruct
    protected void init() {
        SimpleDateFormat isoOut = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);
        SimpleDateFormat humanOut = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        ResourceResolver resolver = request.getResourceResolver();

        // When the model is used from slider.html (via Sling include), the current
        // resource is the slider child node. Traverse up to the parent news node.
        Resource currentRes = request.getResource();
        Resource newsRes = SLIDER_RESOURCE_TYPE.equals(currentRes.getResourceType())
                ? currentRes.getParent()
                : currentRes;
        if (newsRes == null) return;

        // Settings are authored on the slider child node; fall back to the news node
        // for pages that existed before the slider dialog was first saved.
        Resource sliderRes = newsRes.getChild("slider");
        ValueMap sliderProps = (sliderRes != null) ? sliderRes.getValueMap() : ValueMap.EMPTY;
        ValueMap newsProps   = newsRes.getValueMap();

        String coursesRoot = sliderProps.get("coursesRoot", newsProps.get("coursesRoot", DEFAULT_COURSES_ROOT));
        String[] filterTags = sliderProps.containsKey("filterTags")
                ? sliderProps.get("filterTags", String[].class)
                : newsProps.get("filterTags", String[].class);

        String authoredSort = sliderProps.get("sortBy", newsProps.get("sortBy", ""));
        String sortBy = request.getParameter("sortBy") != null
                ? request.getParameter("sortBy")
                : authoredSort;

        showSortControl  = sliderProps.get("showSortControl",  newsProps.get("showSortControl",  false));
        showTagFilter    = sliderProps.get("showTagFilter",    newsProps.get("showTagFilter",    false));
        hideDots         = sliderProps.get("hideDots",         newsProps.get("hideDots",         false));
        hideNavigation   = sliderProps.get("hideNavigation",   newsProps.get("hideNavigation",   false));
        long maxItems    = sliderProps.get("maxItems",         newsProps.get("maxItems",         -1L));

        try {
            Map<String, String> queryMap = buildQueryMap(coursesRoot, filterTags, sortBy, maxItems);
            Session session = resolver.adaptTo(Session.class);
            Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
            SearchResult result = query.getResult();

            PageManager pageManager = resolver.adaptTo(PageManager.class);
            TagManager tagManager  = resolver.adaptTo(TagManager.class);

            for (Hit hit : result.getHits()) {
                try {
                    String path = hit.getPath();
                    Page coursePage = pageManager != null ? pageManager.getPage(path) : null;
                    if (coursePage == null) continue;

                    Resource content = coursePage.getContentResource();
                    if (content == null) continue;

                    ValueMap vm = content.getValueMap();

                    String startDate = "";
                    String formattedStartDate = "";
                    Calendar cal = vm.get("startDate", Calendar.class);
                    if (cal != null) {
                        startDate = isoOut.format(cal.getTime());
                        formattedStartDate = humanOut.format(cal.getTime());
                    } else {
                        startDate = vm.get("startDate", "");
                        formattedStartDate = startDate;
                    }

                    List<String> tags = new ArrayList<>();
                    for (String tagId : vm.get("cq:tags", new String[0])) {
                        String label = tagId;
                        if (tagManager != null) {
                            Tag tag = tagManager.resolve(tagId);
                            if (tag != null) label = tag.getTitle();
                        }
                        tags.add(label);
                    }

                    courses.add(new CourseItem(
                            coursePage.getTitle() != null ? coursePage.getTitle() : "",
                            startDate,
                            formattedStartDate,
                            vm.get("abstract", ""),
                            vm.get("link", coursePage.getPath() + ".html"),
                            vm.get("fileReference", ""),
                            tags
                    ));
                } catch (Exception e) {
                    LOG.warn("CourseListModel: skipping hit due to error", e);
                }
            }
        } catch (Exception e) {
            LOG.error("CourseListModel: failed to build courses list", e);
        }
    }

    public List<CourseItem> getCourses() {
        return courses;
    }

    public boolean isShowSortControl() {
        return showSortControl;
    }

    public boolean isShowTagFilter() {
        return showTagFilter;
    }

    public boolean isHideDots() {
        return hideDots;
    }

    public boolean isHideNavigation() {
        return hideNavigation;
    }

    // -------------------------------------------------------------------------

    private Map<String, String> buildQueryMap(String coursesRoot, String[] filterTags, String sortBy, long maxItems) {
        Map<String, String> map = new HashMap<>();
        map.put("type", "cq:Page");
        map.put("path", coursesRoot);
        map.put("1_property", "jcr:content/sling:resourceType");
        map.put("1_property.value", COURSE_RESOURCE_TYPE);

        if (filterTags != null && filterTags.length > 0) {
            map.put("tagid.property", "jcr:content/cq:tags");
            map.put("tagid.or", "true");
            for (int i = 0; i < filterTags.length; i++) {
                map.put("tagid." + (i + 1) + "_value", filterTags[i]);
            }
        }

        if ("startDate_asc".equals(sortBy)) {
            map.put("orderby", "@jcr:content/startDate");
            map.put("orderby.sort", "asc");
        } else if ("startDate_desc".equals(sortBy)) {
            map.put("orderby", "@jcr:content/startDate");
            map.put("orderby.sort", "desc");
        } else if ("title_asc".equals(sortBy)) {
            map.put("orderby", "@jcr:content/jcr:title");
            map.put("orderby.sort", "asc");
        } else if ("title_desc".equals(sortBy)) {
            map.put("orderby", "@jcr:content/jcr:title");
            map.put("orderby.sort", "desc");
        }

        map.put("p.limit", maxItems > 0 ? String.valueOf(maxItems) : "-1");
        return map;
    }

    // -------------------------------------------------------------------------

    public static class CourseItem {
        private final String title;
        private final String startDate;
        private final String formattedStartDate;
        private final String abstractText;
        private final String link;
        private final String fileReference;
        private final List<String> tags;

        public CourseItem(String title, String startDate, String formattedStartDate,
                          String abstractText, String link, String fileReference, List<String> tags) {
            this.title              = title;
            this.startDate          = startDate;
            this.formattedStartDate = formattedStartDate;
            this.abstractText       = abstractText;
            this.link               = link;
            this.fileReference      = fileReference;
            this.tags               = tags;
        }

        public String getTitle()               { return title; }
        public String getStartDate()           { return startDate; }
        public String getFormattedStartDate()  { return formattedStartDate; }
        @JsonProperty("abstract")
        public String getAbstract()            { return abstractText; }
        public String getLink()                { return link; }
        public String getFileReference()       { return fileReference; }
        public List<String> getTags()          { return tags; }
    }
}
