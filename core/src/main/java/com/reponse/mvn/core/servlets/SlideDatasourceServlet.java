package com.reponse.mvn.core.servlets;

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
        "sling.servlet.resourceTypes=academy-codenova/components/datasource/slides-selector",
        "sling.servlet.methods=GET"
    }
)
public class SlideDatasourceServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> resources  = new ArrayList<>();

        String suffix = request.getRequestPathInfo().getSuffix();
        if (suffix != null) {
            Resource componentResource = resolver.getResource(suffix);
            if (componentResource != null) {
                // Multifield items live under ./items/item0, item1, …
                Resource itemsContainer = componentResource.getChild("items");
                if (itemsContainer != null) {
                    int index = 0;
                    for (Resource child : itemsContainer.getChildren()) {
                        String label = child.getValueMap().get("label", "").trim();
                        if (label.isEmpty()) label = "Slide " + (index + 1);

                        final String text  = label;
                        final String value = String.valueOf(index);
                        ValueMap vm = new ValueMapDecorator(new HashMap<>());
                        vm.put("text",  text);
                        vm.put("value", value);
                        resources.add(new SyntheticResource(resolver, "", "nt:unstructured") {
                            @Override public ValueMap getValueMap() { return vm; }
                        });
                        index++;
                    }
                }
            }
        }

        if (resources.isEmpty()) {
            ValueMap vm = new ValueMapDecorator(new HashMap<>());
            vm.put("text",  "Slide 1");
            vm.put("value", "0");
            resources.add(new SyntheticResource(resolver, "", "nt:unstructured") {
                @Override public ValueMap getValueMap() { return vm; }
            });
        }

        request.setAttribute(DataSource.class.getName(), new SimpleDataSource(resources.iterator()));
    }
}
