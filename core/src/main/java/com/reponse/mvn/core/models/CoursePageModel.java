package com.reponse.mvn.core.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

@Model(
    adaptables = SlingHttpServletRequest.class,
    resourceType = "academy-codenova/components/structure/course-page",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class CoursePageModel {

    private static final SimpleDateFormat STORED_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

    @ScriptVariable
    private Page currentPage;

    private String formattedDate;
    private List<String> tagTitles;

    @PostConstruct
    protected void init() {
        ValueMap props = currentPage.getProperties();

        // --- Date ---
        Calendar cal = props.get("startDate", Calendar.class);
        if (cal != null) {
            formattedDate = DISPLAY_FORMAT.format(cal.getTime());
        } else {
            String raw = props.get("startDate", String.class);
            if (raw != null && !raw.isEmpty()) {
                try {
                    formattedDate = DISPLAY_FORMAT.format(STORED_FORMAT.parse(raw));
                } catch (ParseException e) {
                    formattedDate = raw;
                }
            }
        }

        // --- Tags ---
        String[] tagIds = props.get("cq:tags", String[].class);
        if (tagIds != null && tagIds.length > 0) {
            TagManager tm = currentPage.getContentResource()
                    .getResourceResolver().adaptTo(TagManager.class);
            tagTitles = new ArrayList<>();
            for (String id : tagIds) {
                if (tm != null) {
                    Tag tag = tm.resolve(id);
                    tagTitles.add(tag != null ? tag.getTitle() : lastSegment(id));
                } else {
                    tagTitles.add(lastSegment(id));
                }
            }
        } else {
            tagTitles = Collections.emptyList();
        }
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public List<String> getTagTitles() {
        return tagTitles;
    }

    private static String lastSegment(String tagId) {
        if (tagId == null) return "";
        int slash = tagId.lastIndexOf('/');
        String seg = slash >= 0 ? tagId.substring(slash + 1) : tagId;
        int colon = seg.lastIndexOf(':');
        return colon >= 0 ? seg.substring(colon + 1) : seg;
    }
}
