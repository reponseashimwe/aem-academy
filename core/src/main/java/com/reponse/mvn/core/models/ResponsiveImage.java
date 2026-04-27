package com.reponse.mvn.core.models;

import java.util.Map;

import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface ResponsiveImage {

    Map<String, RenditionSource> getRenditions();

    String getAlt();

    String getFallbackSrc();

    boolean hasRenditions();

    interface RenditionSource {
        String getSrc1x();

        String getSrc2x();

        String getMedia();

        String getSrcset();

        boolean hasSrcset();
    }
}
