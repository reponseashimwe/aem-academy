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
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class UtilityLinksModel {

    @ValueMapValue
    private String title;

    @ValueMapValue
    private Boolean allowTitle;

    @ValueMapValue
    private Boolean allowIcons;

    @ChildResource(name = "utilLinks")
    private Resource utilLinks;

    @ChildResource(name = "links")
    private Resource links;

    public String getTitle() {
        return title;
    }

    public boolean isShowTitle() {
        if (allowTitle != null) {
            return allowTitle;
        }
        return true;
    }

    public boolean isShowIcon() {
        if (allowIcons != null) {
            return allowIcons;
        }
        return true;
    }

    public List<LinkItem> getItems() {
        Resource linksResource = utilLinks != null ? utilLinks : links;
        if (linksResource == null) {
            return Collections.emptyList();
        }

        List<LinkItem> items = new ArrayList<>();
        Iterator<Resource> children = linksResource.listChildren();

        while (children.hasNext()) {
            Resource child = children.next();
            ValueMap vm = child.getValueMap();

            String label = vm.get("label", String.class);
            String url = vm.get("url", String.class);
            String icon = vm.get("icon", String.class);

            if (label != null && !label.isBlank()) {
                items.add(new LinkItem(label, url, icon));
            }
        }

        return items;
    }

    public static final class LinkItem {
        private final String label;
        private final String url;
        private final String icon;

        public LinkItem(String label, String url, String icon) {
            this.label = label;
            this.url = url;
            this.icon = icon;
        }

        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public String getIcon() { return icon; }
    }
}
