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
}