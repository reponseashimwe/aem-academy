package com.reponse.mvn.core.servlets;

import java.util.List;
import java.util.Objects;
import java.util.Calendar;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = SlingPostProcessor.class,
        property = Constants.SERVICE_RANKING + ":Integer=2000")
public class CoursePageFieldSyncPostProcessor implements SlingPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CoursePageFieldSyncPostProcessor.class);

    private static final String COURSE_PAGE_RT = "academy-codenova/components/structure/course-page";

    @Override
    public void process(SlingHttpServletRequest request, List<Modification> changes) {
        try {
            doProcess(request, changes);
        } catch (Exception e) {
            LOG.warn("CoursePageFieldSyncPostProcessor: {}", e.getMessage(), e);
        }
    }

    private void doProcess(SlingHttpServletRequest request, List<Modification> changes) {
        Resource requestResource = request.getResource();
        Resource jcrContent = findContainingCoursePageContent(requestResource);
        if (jcrContent == null) {
            return;
        }

        Resource titleRes = jcrContent.getChild("title");
        Resource metaRes = jcrContent.getChild("meta");
        Resource abstractRes = jcrContent.getChild("abstract");

        boolean titleChildTouched = false;
        boolean metaTouched = false;
        boolean abstractTouched = false;
        if (changes != null) {
            for (Modification m : changes) {
                if (m == null || m.getType() == ModificationType.DELETE) {
                    continue;
                }
                String src = m.getSource();
                if (src == null) {
                    continue;
                }
                if (isUnderTitleNode(src)) {
                    titleChildTouched = true;
                }
                if (isUnderMetaNode(src)) {
                    metaTouched = true;
                }
                if (isUnderAbstractNode(src)) {
                    abstractTouched = true;
                }
            }
        }

        ModifiableValueMap pageMap = jcrContent.adaptTo(ModifiableValueMap.class);
        if (pageMap == null) {
            return;
        }

        if (titleRes != null) {
            String pageTitle = jcrContent.getValueMap().get("jcr:title", "");
            if (pageTitle == null) {
                pageTitle = "";
            }
            ModifiableValueMap titleMap = titleRes.adaptTo(ModifiableValueMap.class);
            if (titleMap != null) {
                String compTitle = firstNonBlank(
                        titleRes.getValueMap().get("jcr:title", ""),
                        titleRes.getValueMap().get("text", ""));
                if (compTitle == null) {
                    compTitle = "";
                }

                String merged;
                if (titleChildTouched) {
                    merged = firstNonBlank(compTitle, pageTitle);
                } else {
                    merged = firstNonBlank(pageTitle, compTitle);
                }

                boolean touched = false;
                if (!Objects.equals(pageTitle, merged)) {
                    pageMap.put("jcr:title", merged);
                    touched = true;
                }
                if (!Objects.equals(compTitle, merged)) {
                    titleMap.put("jcr:title", merged);
                    titleMap.put("text", merged);
                    touched = true;
                }
                if (touched) {
                    LOG.debug("Synced course page title to/from '{}'", merged);
                }
            }
        }

        // Keep page tags and meta tags mirrored. When meta was saved, meta wins; otherwise page wins.
        if (metaRes != null) {
            ModifiableValueMap metaMap = metaRes.adaptTo(ModifiableValueMap.class);
            if (metaMap != null) {
                boolean pageHasTags = jcrContent.getValueMap().containsKey("cq:tags");
                boolean metaHasTags = metaRes.getValueMap().containsKey("cq:tags");
                String[] pageTags = jcrContent.getValueMap().get("cq:tags", new String[0]);
                String[] metaTags = metaRes.getValueMap().get("cq:tags", new String[0]);
                if (pageTags == null) {
                    pageTags = new String[0];
                }
                if (metaTags == null) {
                    metaTags = new String[0];
                }

                String[] mergedTags;
                if (metaTouched) {
                    mergedTags = metaHasTags ? metaTags : new String[0];
                } else if (pageHasTags) {
                    mergedTags = pageTags;
                } else {
                    mergedTags = metaHasTags ? metaTags : new String[0];
                }

                if (!sameTags(pageTags, mergedTags)) {
                    pageMap.put("cq:tags", mergedTags);
                }
                if (!sameTags(metaTags, mergedTags)) {
                    metaMap.put("cq:tags", mergedTags);
                }

                Calendar pageStartDate = jcrContent.getValueMap().get("startDate", Calendar.class);
                Calendar metaStartDate = metaRes.getValueMap().get("startDate", Calendar.class);
                Calendar mergedStartDate = metaTouched
                        ? firstNonNull(metaStartDate, pageStartDate)
                        : firstNonNull(pageStartDate, metaStartDate);
                if (!sameCalendar(pageStartDate, mergedStartDate)) {
                    pageMap.put("startDate", mergedStartDate);
                }
                if (!sameCalendar(metaStartDate, mergedStartDate)) {
                    metaMap.put("startDate", mergedStartDate);
                }
            }
        }

        if (abstractRes != null) {
            ModifiableValueMap abstractMap = abstractRes.adaptTo(ModifiableValueMap.class);
            if (abstractMap != null) {
                String pageAbstract = trimToEmpty(jcrContent.getValueMap().get("abstract", ""));
                String componentAbstract = trimToEmpty(abstractRes.getValueMap().get("text", ""));
                String mergedAbstract = abstractTouched
                        ? firstNonBlank(componentAbstract, pageAbstract)
                        : firstNonBlank(pageAbstract, componentAbstract);

                if (!Objects.equals(pageAbstract, mergedAbstract)) {
                    pageMap.put("abstract", mergedAbstract);
                }
                if (!Objects.equals(componentAbstract, mergedAbstract)) {
                    abstractMap.put("text", mergedAbstract);
                }
            }
        }
    }

    private static boolean isUnderTitleNode(String path) {
        return path.endsWith("/title")
                || path.contains("/jcr:content/title/");
    }

    private static boolean isUnderMetaNode(String path) {
        return path.endsWith("/meta")
                || path.contains("/jcr:content/meta/");
    }

    private static boolean isUnderAbstractNode(String path) {
        return path.endsWith("/abstract")
                || path.contains("/jcr:content/abstract/");
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return "";
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private static boolean sameCalendar(Calendar a, Calendar b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.getTimeInMillis() == b.getTimeInMillis();
    }

    private static boolean sameTags(String[] a, String[] b) {
        if (a == null) {
            a = new String[0];
        }
        if (b == null) {
            b = new String[0];
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    private static Resource findContainingCoursePageContent(Resource start) {
        Resource r = start;
        while (r != null) {
            if ("jcr:content".equals(r.getName())
                    && COURSE_PAGE_RT.equals(r.getValueMap().get("sling:resourceType", String.class))) {
                return r;
            }
            r = r.getParent();
        }
        return null;
    }
}
