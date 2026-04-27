package com.reponse.mvn.core.servlets;

import java.io.IOException;
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
import com.reponse.mvn.core.models.ImageRenditionConstants;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=academy-codenova/components/datasource/image-renditions",
        "sling.servlet.methods=GET"
    }
)
public class ImageRenditionsDatasourceServlet extends SlingSafeMethodsServlet {
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> resources = new ArrayList<>();
        for (String transformName : ImageRenditionConstants.AUTHORABLE_BASE_TRANSFORMS) {
            ValueMap vm = new ValueMapDecorator(new HashMap<>());
            vm.put("value", transformName);
            vm.put("text", ImageRenditionConstants.toAuthorableTransformText(transformName));
            resources.add(new SyntheticResource(
                    resolver,
                    "/apps/datasource/image-renditions/" + transformName,
                    "nt:unstructured") {
                @Override
                public ValueMap getValueMap() {
                    return vm;
                }
            });
        }

        request.setAttribute(DataSource.class.getName(), new SimpleDataSource(resources.iterator()));
    }
}

