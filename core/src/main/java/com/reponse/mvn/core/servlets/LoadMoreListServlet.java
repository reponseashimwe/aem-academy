package com.reponse.mvn.core.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import org.json.JSONArray;
import org.json.JSONObject;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.resourceTypes=academy-codenova/components/content/load-more-list",
                "sling.servlet.selectors=loadmore",
                "sling.servlet.extensions=json",
                "sling.servlet.methods=" + HttpConstants.METHOD_GET
        }
)
@ServiceDescription("Load More List Servlet")
public class LoadMoreListServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LoadMoreListServlet.class);

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resolver = request.getResourceResolver();
        PageManager pageManager = resolver.adaptTo(PageManager.class);

        String parentPath = request.getResource().getValueMap().get("parentPage", String.class);

        int limit = request.getResource().getValueMap().get("itemsPerLoad", 4);
        int offset = getOffset(request);

        LOG.info("Parent page: {}", parentPath);
        LOG.info("Limit: {}, Offset: {}", limit, offset);

        Page parentPage = pageManager.getPage(parentPath);

        List<Page> pages = new ArrayList<>();

        if (parentPage != null) {

            Iterator<Page> children = parentPage.listChildren();

            while (children.hasNext()) {
                pages.add(children.next());
            }
        }

        int total = pages.size();

        int start = Math.min(offset, total);
        int end = Math.min(offset + limit, total);

        List<Page> subset = pages.subList(start, end);

        boolean hasMore = end < total;

        try {

            JSONObject result = new JSONObject();
            JSONArray items = new JSONArray();

            for (Page page : subset) {

                JSONObject obj = new JSONObject();

                obj.put("title", page.getTitle());
                obj.put("url", page.getPath() + ".html");
                obj.put("description", page.getDescription());

                items.put(obj);
            }

            result.put("items", items);

            JSONObject pagination = new JSONObject();
            pagination.put("offset", start);
            pagination.put("limit", limit);
            pagination.put("total", total);
            pagination.put("hasMore", hasMore);

            result.put("pagination", pagination);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(result.toString());

        } catch (Exception e) {

            LOG.error("JSON creation failed", e);

            response.sendError(
                    SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to build JSON response");
        }
    }

    private int getOffset(SlingHttpServletRequest request) {

        try {
            return Integer.parseInt(request.getParameter("offset"));
        } catch (Exception e) {
            return 0;
        }
    }
}