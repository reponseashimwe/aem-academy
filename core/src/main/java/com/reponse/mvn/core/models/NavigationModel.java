package com.reponse.mvn.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(adaptables = Resource.class)
public class NavigationModel {

    @Self
    private Resource resource;

    public Page getRootPage() {

        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page currentPage = pageManager.getContainingPage(resource);

        Page root = currentPage;

        while (root.getParent() != null && !root.getParent().getPath().equals("/content")) {
            root = root.getParent();
        }

        return root;
    }

    public boolean isHomePage() {
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) {
            return false;
        }

        Page currentPage = pageManager.getContainingPage(resource);
        Page rootPage = getRootPage();

        if (currentPage == null || rootPage == null) {
            return false;
        }

        String currentPath = currentPage.getPath();
        String rootPath = rootPage.getPath();

        if (currentPath.equals(rootPath)) {
            return true;
        }
        
        if (currentPath.startsWith(rootPath + "/")
                && currentPage.getDepth() == rootPage.getDepth() + 2) {
            return true;
        }

        return false;
    }
}