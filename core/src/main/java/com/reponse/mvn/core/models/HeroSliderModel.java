package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    resourceType = "academy-codenova/components/content/hero-slider",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HeroSliderModel {

    @Self
    @Inject
    private Resource resource;

    @ValueMapValue
    @Optional
    @Default(booleanValues = true)
    private boolean autoplay;

    @ValueMapValue
    @Optional
    @Default(intValues = 5000)
    private int autoplayDelay;

    @ValueMapValue
    @Optional
    private boolean loop;

    @ValueMapValue
    @Optional
    @Default(booleanValues = true)
    private boolean showPagination;

    @ValueMapValue
    @Optional
    @Default(booleanValues = true)
    private boolean showNavigation;

    public static class Slide {

        private final String image;
        private final String pretitle;
        private final String title;
        private final String subtitle;
        private final String ctaLabel;
        private final String ctaLink;
        private final String ctaVariant;

        public Slide(Resource slideResource) {
            ValueMap vm = slideResource.getValueMap();
            this.image = vm.get("image", String.class);
            this.pretitle = vm.get("pretitle", String.class);
            this.title = vm.get("title", String.class);
            this.subtitle = vm.get("subtitle", String.class);
            this.ctaLabel = vm.get("ctaLabel", String.class);
            this.ctaLink = vm.get("ctaLink", String.class);
            this.ctaVariant = vm.get("ctaVariant", String.class);
        }

        public String getImage() {
            return image;
        }

        public String getPretitle() {
            return pretitle;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getCtaLabel() {
            return ctaLabel;
        }

        public String getCtaLink() {
            return ctaLink;
        }

        public String getCtaVariant() {
            return ctaVariant;
        }
    }

    private List<Slide> slides;

    @PostConstruct
    protected void init() {
        slides = new ArrayList<>();
        if (resource != null) {
            Resource slidesParent = resource.getChild("slides");
            if (slidesParent != null) {
                for (Resource slideResource : slidesParent.getChildren()) {
                    slides.add(new Slide(slideResource));
                }
            }
        }
    }

    public List<Slide> getSlides() {
        return slides;
    }

    public boolean isHasSlides() {
        return slides != null && !slides.isEmpty();
    }

    public int getTotalSlides() {
        return slides != null ? slides.size() : 0;
    }

    public boolean isAutoplay() {
        return autoplay;
    }

    public int getAutoplayDelay() {
        return autoplayDelay > 0 ? autoplayDelay : 5000;
    }

    public boolean isLoop() {
        return loop;
    }

    public boolean isShowPagination() {
        return showPagination;
    }

    public boolean isShowNavigation() {
        return showNavigation;
    }
}
