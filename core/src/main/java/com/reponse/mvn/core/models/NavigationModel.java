package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(
    adaptables = {SlingHttpServletRequest.class, Resource.class},
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class NavigationModel {
    private static final Logger LOG = LoggerFactory.getLogger(NavigationModel.class);
    private static final Pattern CONTENT_URI_PATTERN = Pattern.compile("^(/content/[^.]+)");

    @Self
    private SlingHttpServletRequest selfRequest;

    @Self
    private Resource selfResource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    private SlingHttpServletRequest getEffectiveRequest() {
        return request != null ? request : selfRequest;
    }

    private Resource getEffectiveResource() {
        SlingHttpServletRequest effectiveRequest = getEffectiveRequest();
        if (effectiveRequest != null && effectiveRequest.getResource() != null) {
            return effectiveRequest.getResource();
        }
        return selfResource;
    }

    private Page resolveCurrentContentPage(PageManager pageManager) {
        if (currentPage != null
            && currentPage.getPath() != null
            && currentPage.getPath().startsWith("/content/")) {
            return currentPage;
        }

        SlingHttpServletRequest effectiveRequest = getEffectiveRequest();
        if (effectiveRequest != null) {
            String requestUri = effectiveRequest.getRequestURI();
            if (requestUri != null) {
                Matcher matcher = CONTENT_URI_PATTERN.matcher(requestUri);
                if (matcher.find()) {
                    Page fromRequestUri = pageManager.getPage(matcher.group(1));
                    if (fromRequestUri != null) {
                        return fromRequestUri;
                    }
                }
            }

            String requestResourcePath = effectiveRequest.getRequestPathInfo() != null
                ? effectiveRequest.getRequestPathInfo().getResourcePath()
                : null;
            if (requestResourcePath != null) {
                Resource resolvedRequestResource = resourceResolver != null
                    ? resourceResolver.getResource(requestResourcePath)
                    : null;
                Page fromRequestPath = resolvedRequestResource != null
                    ? pageManager.getContainingPage(resolvedRequestResource)
                    : null;
                if (fromRequestPath != null
                    && fromRequestPath.getPath() != null
                    && fromRequestPath.getPath().startsWith("/content/")) {
                    return fromRequestPath;
                }
            }

            if (effectiveRequest.getResource() != null) {
                Page fromRequestResource = pageManager.getContainingPage(effectiveRequest.getResource());
                if (fromRequestResource != null
                    && fromRequestResource.getPath() != null
                    && fromRequestResource.getPath().startsWith("/content/")) {
                    return fromRequestResource;
                }
            }
        }

        Resource effectiveResource = getEffectiveResource();
        Page fromResource = effectiveResource != null ? pageManager.getContainingPage(effectiveResource) : null;
        if (fromResource != null && fromResource.getPath() != null && fromResource.getPath().startsWith("/content/")) {
            return fromResource;
        }

        return null;
    }

    public Page getRootPage() {
        try {
            if (resourceResolver == null) {
                LOG.error("NavigationModel: ResourceResolver injection failed");
                return null;
            }
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            if (pageManager == null) {
                LOG.error(
                    "NavigationModel: PageManager adaptation failed for requestResource {}",
                    getEffectiveResource() != null ? getEffectiveResource().getPath() : "n/a"
                );
                return null;
            }
            Page currentPage = resolveCurrentContentPage(pageManager);
            if (currentPage == null) {
                SlingHttpServletRequest effectiveRequest = getEffectiveRequest();
                LOG.debug(
                    "NavigationModel: No /content page resolved. modelResource={}, requestResource={}, requestPath={}",
                    selfResource != null ? selfResource.getPath() : "n/a",
                    getEffectiveResource() != null ? getEffectiveResource().getPath() : "n/a",
                    effectiveRequest != null && effectiveRequest.getRequestPathInfo() != null
                        ? effectiveRequest.getRequestPathInfo().getResourcePath()
                        : "n/a"
                );
                return null;
            }

            Page root = currentPage;
            while (root.getParent() != null && !root.getParent().getPath().equals("/content")) {
                root = root.getParent();
            }

            LOG.info(
                "NavigationModel: current page={}, found root page={}",
                currentPage.getPath(),
                root.getPath()
            );
            return root;
        } catch (RuntimeException ex) {
            LOG.error(
                "NavigationModel: Failed while resolving root page for requestResource {}",
                getEffectiveResource() != null ? getEffectiveResource().getPath() : "n/a",
                ex
            );
            return null;
        }
    }

    public List<Page> getVisibleRootChildren() {
        Page root = getRootPage();
        if (root == null) {
            LOG.error("NavigationModel: root page is null; cannot compute visible root children");
            return Collections.emptyList();
        }

        List<Page> visible = new ArrayList<>();
        try {
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
            LOG.info(
                "NavigationModel: root page={} has {} visible child pages: {}",
                root.getPath(),
                visible.size(),
                visible.stream().map(Page::getPath).toArray()
            );
        } catch (RuntimeException ex) {
            LOG.error("NavigationModel: Failed while listing child pages for root {}", root.getPath(), ex);
            return Collections.emptyList();
        }
        return visible;
    }

    public boolean isHomePage() {
        if (resourceResolver == null) {
            return false;
        }
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page currentPage = pageManager != null ? resolveCurrentContentPage(pageManager) : null;
        Page rootPage = getRootPage();

        if (currentPage == null || rootPage == null) {
            return false;
        }
        return currentPage.getPath().equals(rootPage.getPath());
    }

    public boolean isContentPage() {
        if (resourceResolver == null) {
            return false;
        }
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page currentPage = pageManager != null ? resolveCurrentContentPage(pageManager) : null;
        return currentPage != null
            && currentPage.getPath() != null
            && currentPage.getPath().startsWith("/content/");
    }

    public String getRootPagePath() {
        Page rootPage = getRootPage();
        return rootPage != null && rootPage.getPath() != null ? rootPage.getPath() : "";
    }

    private String findNearestContentPathWithNode(String nodeName) {
        if (resourceResolver == null) {
            return "";
        }
        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return "";
        }

        Page cursor = resolveCurrentContentPage(pageManager);
        while (cursor != null && cursor.getPath() != null && cursor.getPath().startsWith("/content/")) {
            Resource contentResource = cursor.getContentResource();
            if (contentResource != null && contentResource.getChild(nodeName) != null) {
                return cursor.getPath();
            }
            cursor = cursor.getParent();
        }
        return "";
    }

    public String getHeaderSourcePagePath() {
        return findNearestContentPathWithNode("codeland-header");
    }

    public String getFooterSourcePagePath() {
        return findNearestContentPathWithNode("codeland-footer");
    }
}