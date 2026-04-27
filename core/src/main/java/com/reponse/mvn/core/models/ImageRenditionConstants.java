package com.reponse.mvn.core.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageRenditionConstants {
    private static final Pattern SIZE_PATTERN = Pattern.compile(".*?(\\d+)x(\\d+)$");
    private static final String SIZE_TEXT_FORMAT = "%spx x %spx";

    public static final List<String> KNOWN_TRANSFORMS = Collections.unmodifiableList(Arrays.asList(
            "image-279x180",
            "image-358x262",
            "image-374x321",
            "image-400x400",
            "image-492x257",
            "image-558x360",
            "image-650x493",
            "image-716x524",
            "image-748x642",
            "image-800x800",
            "image-984x514",
            "image-1240x769",
            "image-1300x986",
            "image-2480x1538"));

    public static final Set<String> KNOWN_TRANSFORM_SET =
            Collections.unmodifiableSet(new HashSet<>(KNOWN_TRANSFORMS));

    // Only show base renditions that have a matching 2x transform.
    public static final List<String> AUTHORABLE_BASE_TRANSFORMS = Collections.unmodifiableList(Arrays.asList(
            "image-279x180",
            "image-358x262",
            "image-374x321",
            "image-400x400",
            "image-492x257",
            "image-650x493",
            "image-1240x769"));

    public static String toAuthorableTransformText(String transformName) {
        if (transformName == null) {
            return "";
        }
        Matcher matcher = SIZE_PATTERN.matcher(transformName);
        if (matcher.matches()) {
            return String.format(SIZE_TEXT_FORMAT, matcher.group(1), matcher.group(2));
        }
        return transformName;
    }

    public static String to2xTransformName(String transformName) {
        if (transformName == null) {
            return null;
        }
        Matcher matcher = SIZE_PATTERN.matcher(transformName);
        if (!matcher.matches()) {
            return transformName;
        }
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));
        String prefix = transformName.substring(0, matcher.start(1));
        return prefix + (width * 2) + "x" + (height * 2);
    }

    private ImageRenditionConstants() {
    }
}
