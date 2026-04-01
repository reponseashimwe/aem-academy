package com.reponse.mvn.core.servlets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;


@Component(service = SlingPostProcessor.class)
public class HeroSlidesPostProcessor implements SlingPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HeroSlidesPostProcessor.class);

    private static final String RESOURCE_TYPE       = "academy-codenova/components/atomic/hero-slides";
    private static final String SLIDE_DEFAULTS_PATH = "/apps/academy-codenova/components/atomic/hero-slide/cq:template";

    @Override
    public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception {
        try {
            doProcess(request);
        } catch (Exception e) {
            LOG.warn("[HeroSlidesPostProcessor] Unexpected error: {}", e.getMessage(), e);
        }
    }

    private void doProcess(SlingHttpServletRequest request) throws Exception {
        String path = request.getResource().getPath();
        ResourceResolver resolver = request.getResourceResolver();
        Resource component = resolver.getResource(path);

        if (component == null || ResourceUtil.isNonExistingResource(component)) {
            return;
        }

        String actualType = getResourceTypeSafe(component);

        if (!RESOURCE_TYPE.equals(actualType)) {
            LOG.info("[HeroSlidesPostProcessor] Skipped — resource type does not match.");
            return;
        }

        Resource itemsContainer = component.getChild("items");
        if (itemsContainer == null) {
            return;
        }


        Set<String> validIds = new HashSet<>();
        int index = 0;

        for (Resource item : itemsContainer.getChildren()) {
            String slideId = item.getValueMap().get("slideId", String.class);
            boolean isNew  = (slideId == null || slideId.isEmpty());

            if (isNew) {
                slideId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                try {
                    Node node = item.adaptTo(Node.class);
                    if (node != null) {
                        node.setProperty("slideId", slideId);
                    } else {
                        LOG.warn("[HeroSlidesPostProcessor] Could not adapt item to Node: {}", item.getPath());
                    }
                } catch (RepositoryException e) {
                    LOG.error("[HeroSlidesPostProcessor] Failed to set slideId on {}", item.getPath(), e);
                }
                seedSlideContent(component, slideId, resolver);
            }

            validIds.add(slideId);
            index++;
        }


        // Remove content nodes for deleted slides.
        for (Resource child : component.getChildren()) {
            String name = child.getName();
            if (name.equals("items")
                    || name.startsWith("jcr:")
                    || name.startsWith("cq:")
                    || name.startsWith("rep:")) continue;
            if (validIds.contains(name)) continue;
            try {
                Node node = child.adaptTo(Node.class);
                if (node != null) node.remove();
            } catch (RepositoryException e) {
                LOG.error("[HeroSlidesPostProcessor] Failed to remove orphaned node {}", child.getPath(), e);
            }
        }

    }
    private String getResourceTypeSafe(Resource resource) {
        try {
            Node node = resource.adaptTo(Node.class);
            if (node == null || !node.hasProperty("sling:resourceType")) {
                return resource.getResourceType();
            }
            Property p = node.getProperty("sling:resourceType");
            if (p.isMultiple()) {
                Value[] vals = p.getValues();
                String first = vals.length > 0 ? vals[0].getString() : "";
                node.setProperty("sling:resourceType", first);
                return first;
            }
            return p.getString();
        } catch (RepositoryException e) {
            return "";
        }
    }

    private void seedSlideContent(Resource component, String slideId, ResourceResolver resolver) {
        if (component.getChild(slideId) != null) {
            return;
        }

        Resource template = resolver.getResource(SLIDE_DEFAULTS_PATH);
        if (template == null) {
            LOG.warn("[HeroSlidesPostProcessor] Template not found at {}", SLIDE_DEFAULTS_PATH);
            return;
        }

        Node templateNode  = template.adaptTo(Node.class);
        Node componentNode = component.adaptTo(Node.class);

        if (templateNode == null || componentNode == null) {
            LOG.warn("[HeroSlidesPostProcessor] Cannot adapt template or component to Node — seed aborted.");
            return;
        }

        try {
            JcrUtil.copy(templateNode, componentNode, slideId);
        } catch (RepositoryException e) {
            LOG.error("[HeroSlidesPostProcessor] Failed to seed content node '{}'", slideId, e);
        }
    }
}
