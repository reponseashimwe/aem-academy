package com.reponse.mvn.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ClListModel {

    @ChildResource(name = "items")
    private Resource items;

    public List<String> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        Iterator<Resource> children = items.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            ValueMap vm = child.getValueMap();
            String text = vm.get("text", String.class);
            if (text != null && !text.isBlank()) {
                list.add(text);
            }
        }
        return list;
    }
}
