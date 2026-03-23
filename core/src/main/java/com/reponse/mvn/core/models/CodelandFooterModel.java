package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CodelandFooterModel {

    @ChildResource(name = "column-1")
    private Resource column1;

    @ChildResource(name = "column-2")
    private Resource column2;

    @ChildResource(name = "column-3")
    private Resource column3;

    @ChildResource(name = "column-4")
    private Resource column4;

    @ChildResource(name = "legal-links")
    private Resource legalLinksResource;

    @ChildResource(name = "copyright")
    private Resource copyrightResource;

    public String getCopyrightText() {
        if (copyrightResource == null) {
            return "";
        }
        return copyrightResource.getValueMap().get("text", "");
    }

    public List<FooterSection> getSections() {
        List<FooterSection> sections = new ArrayList<>();
        addSection(sections, column1);
        addSection(sections, column2);
        addSection(sections, column3);
        addSection(sections, column4);
        return sections;
    }

    public List<LegalLink> getLegalLinks() {
        if (legalLinksResource == null) {
            return Collections.emptyList();
        }

        Resource links = legalLinksResource.getChild("utilLinks");
        if (links == null) {
            return Collections.emptyList();
        }

        List<LegalLink> legalLinks = new ArrayList<>();
        Iterator<Resource> children = links.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            ValueMap vm = child.getValueMap();
            String label = vm.get("label", String.class);
            String url = vm.get("url", String.class);
            if (label != null && !label.isBlank()) {
                legalLinks.add(new LegalLink(label, url));
            }
        }

        return legalLinks;
    }

    private void addSection(List<FooterSection> sections, Resource columnResource) {
        if (columnResource == null) {
            return;
        }

        ValueMap vm = columnResource.getValueMap();
        String title = vm.get("title", "");
        List<String> items = new ArrayList<>();

        Resource links = columnResource.getChild("utilLinks");
        if (links != null) {
            Iterator<Resource> children = links.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                String label = child.getValueMap().get("label", String.class);
                if (label != null && !label.isBlank()) {
                    items.add(label);
                }
            }
        }

        if (!title.isBlank() || !items.isEmpty()) {
            sections.add(new FooterSection(title, items));
        }
    }

    public static final class FooterSection {
        private final String title;
        private final List<String> items;

        public FooterSection(String title, List<String> items) {
            this.title = title;
            this.items = items;
        }

        public String getTitle() { return title; }
        public List<String> getItems() { return items; }
    }

    public static final class LegalLink {
        private final String label;
        private final String url;

        public LegalLink(String label, String url) {
            this.label = label;
            this.url = url;
        }

        public String getLabel() { return label; }
        public String getUrl() { return url; }
    }
}
