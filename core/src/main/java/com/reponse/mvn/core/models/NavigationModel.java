package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(adaptables = Resource.class)
public class NavigationModel {

    @Self
    private Resource resource;

    public Page getRootPage() {
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) {
            return null;
        }
        Page currentPage = pageManager.getContainingPage(resource);
        if (currentPage == null) {
            return null;
        }

        Page root = currentPage;

        while (root.getParent() != null && !root.getParent().getPath().equals("/content")) {
            root = root.getParent();
        }

        return root;
    }

    public List<Page> getVisibleRootChildren() {
        Page root = getRootPage();
        if (root == null) {
            return Collections.emptyList();
        }

        List<Page> visible = new ArrayList<>();
        Iterator<Page> children = root.listChildren();
        while (children.hasNext()) {
            Page child = children.next();
            if (child == null) {
                continue;
            }
            if (child.isHideInNav()) {
                continue;
            }
            ValueMap vm = child.getProperties();
            if (vm != null && vm.get("hideInNavigation", false)) {
                continue;
            }
            visible.add(child);
        }
        return visible;
    }

    public boolean isHomePage() {
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page currentPage = pageManager != null ? pageManager.getContainingPage(resource) : null;
        Page rootPage = getRootPage();

        if (currentPage == null || rootPage == null) {
            return false;
        }
        return currentPage.getPath().equals(rootPage.getPath());
    }

    public boolean isContentPage() {
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page currentPage = pageManager != null ? pageManager.getContainingPage(resource) : null;
        return currentPage != null
            && currentPage.getPath() != null
            && currentPage.getPath().startsWith("/content/");
    }

    public String getRootPagePath() {
        Page rootPage = getRootPage();
        return rootPage != null && rootPage.getPath() != null ? rootPage.getPath() : "";
    }
}