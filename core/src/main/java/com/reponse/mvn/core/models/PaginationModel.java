package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.wcm.core.components.models.ListItem;

@Model(
    adaptables = SlingHttpServletRequest.class,
    resourceType = "academy-codenova/components/content/page-grid",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class PaginationModel {
    
    @Self
    private SlingHttpServletRequest request;

    @Self
    @Optional
    private com.adobe.cq.wcm.core.components.models.List list;

    @ValueMapValue
    private int columns;

    private List<ListItem> paginatedItems = Collections.emptyList();
    private int currentPage = 1;
    private int totalPages = 1;

    @PostConstruct
    protected void init () {
        if (list == null) {
            paginatedItems = Collections.emptyList();
            currentPage = 1;
            totalPages = 1;
            return;
        }

        List<ListItem> allItems = new ArrayList<>(list.getListItems());

        if (columns <= 0) {
            columns = 3;
        }

        int totalItems = allItems.size();
        totalPages = (int) Math.ceil((double) totalItems / columns);

        currentPage = getPageFromSelector();

        int start = (currentPage - 1) * columns;
        int end = Math.min(start + columns, allItems.size());

        if (start < totalItems) {
            paginatedItems = allItems.subList(start, end);
        }
    }

    protected int getPageFromSelector() {
        RequestPathInfo pathInfo = request.getRequestPathInfo();
        String[] selectors = pathInfo.getSelectors();

        for (String selector : selectors) {
            if (selector.startsWith("page")) {
                try {
                    return Integer.parseInt(selector.substring(4));
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }

        return 1;
    }

    public List<ListItem> getPaginatedItems() {
        return paginatedItems;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<Integer> getPageNumbers() {
        List<Integer> pageNumbers = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNumbers.add(i);
        }
        return pageNumbers;
    }

    public int getPreviousPage() { 
       return currentPage > 1 ? currentPage - 1 : 1;
    }

    public int getNextPage() {
        return currentPage < totalPages ? currentPage + 1 : totalPages;
    }
}   
