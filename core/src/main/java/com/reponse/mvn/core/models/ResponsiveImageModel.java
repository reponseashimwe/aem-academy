package com.reponse.mvn.core.models;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.cq.wcm.core.components.models.Image;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = {ResponsiveImage.class},
    resourceType = "academy-codenova/components/atomic/image",
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ResponsiveImageModel implements ResponsiveImage {
    private static final String[] FALLBACK_RENDITION_ORDER = {"large", "desktop", "laptop", "tablet", "mobile"};

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Self
    private Image coreImage;

    private Map<String, ResponsiveImage.RenditionSource> renditions = new LinkedHashMap<>();
    private String alt;
    private String fileReference;

    @PostConstruct
    protected void init() {
        Resource resource = request.getResource();
        ValueMap properties = resource.getValueMap();

        fileReference = properties.get("fileReference", String.class);
        if (StringUtils.isBlank(fileReference) && resource.getChild("file") != null) {
            fileReference = resource.getPath() + "/file";
        }
        alt = coreImage != null ? coreImage.getAlt() : "";

        if (isSvg(fileReference)) {
            return;
        }

        Resource renditionsRes = resource.getChild("renditions");
        if (renditionsRes != null) {
            ValueMap renditionProps = renditionsRes.getValueMap();
            buildRenditions(
                    renditionProps.get("mobile", String.class),
                    renditionProps.get("tablet", String.class),
                    renditionProps.get("laptop", String.class),
                    renditionProps.get("desktop", String.class),
                    renditionProps.get("large", String.class));
        }
    }

    private void buildRenditions(String mobile, String tablet, String laptop, String desktop, String large) {
        if (StringUtils.isBlank(fileReference)) {
            return;
        }

        String resolvedLarge1x = resolve1x(large, null);
        String resolvedLarge2x = resolve2xTransform(resolvedLarge1x);

        String resolvedDesktop1x = resolve1x(desktop, resolvedLarge1x);
        String resolvedDesktop2x = resolve2x(desktop, resolvedLarge2x);

        String resolvedLaptop1x = resolve1x(laptop, resolvedDesktop1x);
        String resolvedLaptop2x = resolve2x(laptop, resolvedDesktop2x);

        String resolvedTablet1x = resolve1x(tablet, resolvedLaptop1x);
        String resolvedTablet2x = resolve2x(tablet, resolvedLaptop2x);

        String resolvedMobile1x = resolve1x(mobile, resolvedTablet1x);
        String resolvedMobile2x = resolve2x(mobile, resolvedTablet2x);

        if (StringUtils.isNotBlank(resolvedLarge1x)) {
            renditions.put("large", new RenditionSource(
                buildTransformUrl(fileReference, resolvedLarge1x),
                buildTransformUrlIfPresent(fileReference, resolvedLarge2x),
                "(min-width: 1440px)"
            ));
        }

        if (StringUtils.isNotBlank(resolvedDesktop1x)) {
            renditions.put("desktop", new RenditionSource(
                buildTransformUrl(fileReference, resolvedDesktop1x),
                buildTransformUrlIfPresent(fileReference, resolvedDesktop2x),
                "(min-width: 1280px) and (max-width: 1439px)"
            ));
        }

        if (StringUtils.isNotBlank(resolvedLaptop1x)) {
            renditions.put("laptop", new RenditionSource(
                buildTransformUrl(fileReference, resolvedLaptop1x),
                buildTransformUrlIfPresent(fileReference, resolvedLaptop2x),
                "(min-width: 1024px) and (max-width: 1279px)"
            ));
        }

        if (StringUtils.isNotBlank(resolvedTablet1x)) {
            renditions.put("tablet", new RenditionSource(
                buildTransformUrl(fileReference, resolvedTablet1x),
                buildTransformUrlIfPresent(fileReference, resolvedTablet2x),
                "(min-width: 768px) and (max-width: 1023px)"
            ));
        }

        if (StringUtils.isNotBlank(resolvedMobile1x)) {
            renditions.put("mobile", new RenditionSource(
                buildTransformUrl(fileReference, resolvedMobile1x),
                buildTransformUrlIfPresent(fileReference, resolvedMobile2x),
                "(max-width: 767px)"
            ));
        }
    }

    private String resolve1x(String current, String fallback) {
        return StringUtils.isNotBlank(current) ? current : fallback;
    }

    private String resolve2x(String current1x, String fallback2x) {
        return StringUtils.isNotBlank(current1x) ? resolve2xTransform(current1x) : fallback2x;
    }

    private String buildTransformUrlIfPresent(String assetPath, String transformName) {
        if (StringUtils.isBlank(transformName)) {
            return null;
        }
        return buildTransformUrl(assetPath, transformName);
    }

    private String resolve2xTransform(String oneXTransform) {
        if (StringUtils.isBlank(oneXTransform)) {
            return null;
        }
        String derived2x = ImageRenditionConstants.to2xTransformName(oneXTransform);
        if (StringUtils.isNotBlank(derived2x) && ImageRenditionConstants.KNOWN_TRANSFORM_SET.contains(derived2x)) {
            return derived2x;
        }
        return null;
    }

    private String buildTransformUrl(String assetPath, String transformName) {
        if (StringUtils.isBlank(assetPath) || StringUtils.isBlank(transformName)) {
            return null;
        }
        String ext = getFileExtension(assetPath);
        if (StringUtils.isBlank(ext)) {
            return null;
        }
        String url = assetPath + ".transform/" + transformName + "/image." + ext;
        return mapUrl(url);
    }

    private String getFileExtension(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String clean = path.split("\\?")[0];
        int dot = clean.lastIndexOf('.');
        if (dot > -1 && dot < clean.length() - 1) {
            return clean.substring(dot + 1);
        }
        return null;
    }

    private String mapUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        try {
            String mapped = resourceResolver != null ? resourceResolver.map(path) : path;
            return mapped != null ? mapped : path;
        } catch (Exception e) {
            return path;
        }
    }

    @Override
    public Map<String, ResponsiveImage.RenditionSource> getRenditions() {
        return renditions;
    }

    @Override
    public String getAlt() {
        return alt != null ? alt : "";
    }

    @Override
    public String getFallbackSrc() {
        for (String renditionKey : FALLBACK_RENDITION_ORDER) {
            ResponsiveImage.RenditionSource renditionSource = renditions.get(renditionKey);
            if (renditionSource != null && StringUtils.isNotBlank(renditionSource.getSrc1x())) {
                return renditionSource.getSrc1x();
            }
        }
        if (coreImage != null && StringUtils.isNotBlank(coreImage.getSrc())) {
            return coreImage.getSrc();
        } else if (isSvg(fileReference)) {
            return mapUrl(fileReference);
        } else if (StringUtils.isNotBlank(fileReference)) {
            return mapUrl(fileReference);
        }
        return "";
    }

    @Override
    public boolean hasRenditions() {
        return !renditions.isEmpty();
    }

    private boolean isSvg(String path) {
        return StringUtils.isNotBlank(path) && path.toLowerCase().endsWith(".svg");
    }

    public static class RenditionSource implements ResponsiveImage.RenditionSource {
        private final String src1x;
        private final String src2x;
        private final String media;

        public RenditionSource(String src1x, String src2x, String media) {
            this.src1x = src1x;
            this.src2x = src2x;
            this.media = media;
        }

        public String getSrc1x() {
            return src1x;
        }

        public String getSrc2x() {
            return src2x;
        }

        public String getMedia() {
            return media;
        }

        public String getSrcset() {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotBlank(src1x)) {
                sb.append(src1x).append(" 1x");
            }
            if (StringUtils.isNotBlank(src2x)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(src2x).append(" 2x");
            }
            return sb.toString();
        }

        public boolean hasSrcset() {
            return StringUtils.isNotBlank(src1x) || StringUtils.isNotBlank(src2x);
        }
    }
}
