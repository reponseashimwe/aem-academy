package com.reponse.mvn.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(adaptables = Resource.class)
public class NavigationModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationModel.class);

    @Self
    private Resource resource;

    public Page getRootPage() {

        LOGGER.info("NavigationModel executed for resource: {}", resource.getPath());

        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        Page currentPage = pageManager.getContainingPage(resource);

        Page root = currentPage;

        while (root.getParent() != null && !root.getParent().getPath().equals("/content")) {
            root = root.getParent();
        }

        return root;
    }
}