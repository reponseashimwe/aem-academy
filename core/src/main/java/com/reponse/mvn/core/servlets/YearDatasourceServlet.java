package com.reponse.mvn.core.servlets;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=academy-codenova/components/datasource/year-selector",
        "sling.servlet.methods=GET"
    }
)
public class YearDatasourceServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(
            SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        ResourceResolver resolver = request.getResourceResolver();

        List<Resource> resources = new ArrayList<>();

        int currentYear = Year.now().getValue();

        for (int year = currentYear; year >= 1970; year--) {

            ValueMap vm = new ValueMapDecorator(new HashMap<>());

            String yearString = String.valueOf(year);

            vm.put("text", yearString);
            vm.put("value", yearString);

            Resource resource = new SyntheticResource(
                    resolver,
                    "",
                    "nt:unstructured"
            ) {
                @Override
                public ValueMap getValueMap() {
                    return vm;
                }
            };

            resources.add(resource);
        }

        DataSource ds = new SimpleDataSource(resources.iterator());

        request.setAttribute(DataSource.class.getName(), ds);
    }
}