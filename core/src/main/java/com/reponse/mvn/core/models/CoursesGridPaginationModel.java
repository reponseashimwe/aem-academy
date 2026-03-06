package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.wcm.core.components.models.ListItem;

@Model(
    adaptables = SlingHttpServletRequest.class,
    resourceType = "academy-codenova/components/content/courses-grid",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class CoursesGridPaginationModel {

    private static final int ITEMS_PER_PAGE = 3;
    private static final Pattern PAGE_SELECTOR_PATTERN = Pattern.compile("^page(\\d+)$");

    @Self
    @Optional
    private com.adobe.cq.wcm.core.components.models.List list;

    @Self
    @Optional
    private SlingHttpServletRequest request;

    private List<ListItem> paginatedItems = Collections.emptyList();
    private int currentPage = 1;
    private int totalPages = 1;

    @PostConstruct
    protected void init() {
        if (list == null) {
            paginatedItems = Collections.emptyList();
            currentPage = 1;
            totalPages = 1;
            return;
        }

        List<ListItem> allItems = new ArrayList<>(list.getListItems());
        int totalItems = allItems.size();

        if (totalItems == 0) {
            paginatedItems = Collections.emptyList();
            currentPage = 1;
            totalPages = 0;
            return;
        }

        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        int resolvedCurrentPage = resolveCurrentPage(totalPages);
        currentPage = resolvedCurrentPage;

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;

        if (startIndex >= totalItems) {
            currentPage = 1;
            startIndex = 0;
        }

        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        paginatedItems = allItems.subList(startIndex, endIndex);
    }

    private int resolveCurrentPage(int maxPage) {
        int page = 1;

        if (request != null) {
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            if (pathInfo != null) {
                String[] selectors = pathInfo.getSelectors();
                if (selectors != null) {
                    for (String selector : selectors) {
                        Matcher matcher = PAGE_SELECTOR_PATTERN.matcher(selector);
                        if (matcher.matches()) {
                            try {
                                int parsed = Integer.parseInt(matcher.group(1));
                                if (parsed > 0) {
                                    page = parsed;
                                }
                            } catch (NumberFormatException ignored) {
                                // ignore malformed selector and keep default page = 1
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (page > maxPage) {
            page = maxPage;
        }

        return page;
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

    public int getPreviousPage() {
        if (currentPage <= 1) {
            return 1;
        }
        return currentPage - 1;
    }

    public int getNextPage() {
        if (currentPage >= totalPages) {
            return totalPages;
        }
        return currentPage + 1;
    }

    /**
     * Convenience list of page numbers (1..totalPages) for HTL iteration.
     */
    public List<Integer> getPageNumbers() {
        if (totalPages <= 0) {
            return Collections.emptyList();
        }

        List<Integer> pages = new ArrayList<>(totalPages);
        for (int i = 1; i <= totalPages; i++) {
            pages.add(i);
        }
        return pages;
    }
}

