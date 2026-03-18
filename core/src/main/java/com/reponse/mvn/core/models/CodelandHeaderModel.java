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
public class CodelandHeaderModel {

    @ChildResource(name = "utilLinks")
    private Resource utilLinks;

    public List<UtilLink> getUtilLinks() {
        if (utilLinks == null) {
            return Collections.emptyList();
        }

        List<UtilLink> links = new ArrayList<>();
        Iterator<Resource> children = utilLinks.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            ValueMap vm = child.getValueMap();
            String label = vm.get("label", String.class);
            String url = vm.get("url", String.class);
            String icon = vm.get("icon", String.class);
            if (label != null && !label.isBlank()) {
                links.add(new UtilLink(label, url, icon));
            }
        }
        return links;
    }

    public static final class UtilLink {
        private final String label;
        private final String url;
        private final String icon;

        public UtilLink(String label, String url) {
            this(label, url, null);
        }

        public UtilLink(String label, String url, String icon) {
            this.label = label;
            this.url = url;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getUrl() {
            return url;
        }

        public String getIcon() {
            return icon;
        }
    }
}

