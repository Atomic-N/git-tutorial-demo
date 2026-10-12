package com.aitef.forum.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {
    private SlugUtil() {
    }

    public static String fromTitle(String title) {
        String normalized = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "topic";
        }
        return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
}
