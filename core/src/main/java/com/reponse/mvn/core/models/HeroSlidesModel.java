package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    resourceType = "academy-codenova/components/atomic/hero-slides",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HeroSlidesModel {

    @Self
    @Inject
    private Resource resource;

    @ValueMapValue
    private String currentSlideIndex;

    /** Coral select persists "true" / "false" strings; avoid Boolean coercion issues in ValueMap. */
    @ValueMapValue(name = "autoplay")
    private String autoplay;

    @ValueMapValue
    private String autoplayDelay;

    @ValueMapValue
    private Boolean loop;

    @ValueMapValue
    private Boolean hideDots;

    @ValueMapValue
    private Boolean hideNavigation;

    @ValueMapValue
    private String effect;

    private List<SlideEntry> slides;

    @PostConstruct
    protected void init() {
        slides = new ArrayList<>();

        int currentIdx = 0;
        if (currentSlideIndex != null && !currentSlideIndex.isEmpty()) {
            try { currentIdx = Integer.parseInt(currentSlideIndex); } catch (NumberFormatException e) { /* default 0 */ }
        }

        // Metadata lives under ./items/item0, item1, … (managed by multifield).
        // Content lives under ./item0/, ./item1/, … (siblings of items/, safe from multifield deletes).
        Resource itemsContainer = resource.getChild("items");
        if (itemsContainer == null) return;

        // Build slides first, then clamp currentIdx if the previously-selected slide was deleted.
        int index = 0;
        for (Resource metaChild : itemsContainer.getChildren()) {
            String label = metaChild.getValueMap().get("label", "").trim();
            if (label.isEmpty()) label = "Slide " + (index + 1);

            boolean hidden = metaChild.getValueMap().get("hidden", Boolean.FALSE);

            // Content lives as a sibling keyed by slideId — a stable UUID that never changes.
            // The multifield reorders by swapping property values (not moving nodes), so slideId
            // moves with its item's label, keeping content correctly associated after reorder.
            String slideId    = metaChild.getValueMap().get("slideId", metaChild.getName());
            String contentPath = resource.getPath() + "/" + slideId;

            slides.add(new SlideEntry(metaChild.getName(), contentPath, hidden, label, index + 1, false));
            index++;
        }

        // Clamp currentIdx to 0 if the previously-selected slide was deleted, then mark it.
        if (!slides.isEmpty()) {
            if (currentIdx >= slides.size()) currentIdx = 0;
            slides.get(currentIdx).setCurrent(true);
        }
    }

    public List<SlideEntry> getSlides() { return slides; }

    public String getCurrentSlideIndex() { return currentSlideIndex != null ? currentSlideIndex : "0"; }

    public boolean isAutoplay() {
        if (autoplay == null || autoplay.isEmpty()) {
            return true;
        }
        return !"false".equalsIgnoreCase(autoplay.trim());
    }
    public String  getAutoplayDelay() { return autoplayDelay != null && !autoplayDelay.isEmpty() ? autoplayDelay : "5000"; }
    public boolean isLoop()           { return !Boolean.FALSE.equals(loop); }
    public boolean isHideDots()       { return Boolean.TRUE.equals(hideDots); }
    public boolean isHideNavigation() { return Boolean.TRUE.equals(hideNavigation); }
    public String  getEffect()        { return effect != null && !effect.isEmpty() ? effect : "slide"; }

    public static class SlideEntry {
        private final String  name;
        private final String  contentPath;
        private final boolean hidden;
        private final String  label;
        private final int     number;
        private       boolean current;

        public SlideEntry(String name, String contentPath, boolean hidden, String label, int number, boolean current) {
            this.name        = name;
            this.contentPath = contentPath;
            this.hidden      = hidden;
            this.label       = label;
            this.number      = number;
            this.current     = current;
        }

        public String  getName()        { return name; }
        public String  getContentPath() { return contentPath; }
        public boolean isHidden()       { return hidden; }
        public String  getLabel()       { return label; }
        public int     getNumber()      { return number; }
        public boolean isCurrent()             { return current; }
        public void   setCurrent(boolean val) { this.current = val; }
        public boolean isExists()       { return true; }
        /** @deprecated use getContentPath() */
        public String  getPath()        { return contentPath; }
    }
}
