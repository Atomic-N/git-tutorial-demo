package com.aitef.forum.util;

public final class HtmlUtil {
    private HtmlUtil() {
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public static String toCookedContent(String raw) {
        return MarkdownUtil.render(raw);
    }

    public static String excerpt(String raw, int length) {
        String normalized = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= length) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, length - 1)) + "...";
    }
}
