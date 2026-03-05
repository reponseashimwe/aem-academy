package com.reponse.mvn.core.models;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Optional;

@Model(
    adaptables = Resource.class,
    resourceType = "academy-codenova/components/content/styled-header",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class AcademyLearningDemo {

    @Inject
    @Named("jcr:title")
    private String title;

    @Inject
    @Optional
    private String customDescription;

    @Inject
    @Optional
    private String subtitle;

    @Inject
    @Optional
    private String position;

    @PostConstruct
    protected void init() {
        if (customDescription != null && !customDescription.isEmpty()) {
            customDescription = customDescription.toUpperCase();
        } else {
            customDescription = "Not Available";
        }
    }

    public String getTitle() {
        return (title != null && !title.isEmpty()) ? title : "CodeNova";
    }

    public String getCustomDescription() {
        return customDescription;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getPosition() {
        return (position != null && !position.isEmpty()) ? position : "start";
    }

    public String getAlignmentClass() {
        return "text-" + getPosition();
    }
}