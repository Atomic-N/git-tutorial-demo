package com.aitef.forum.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownUtil {
    private static final String CONTEXT_UPLOAD_PREFIX = "(?:/[\\w-]+)?/static/uploads/";
    private static final String ABSOLUTE_UPLOAD_PREFIX = "https?://[^\\s)]+?/static/uploads/";
    private static final String RELATIVE_UPLOAD_URL = CONTEXT_UPLOAD_PREFIX + "[^\\s)]+";
    private static final String ABSOLUTE_UPLOAD_URL = ABSOLUTE_UPLOAD_PREFIX + "[^\\s)]+";
    private static final String UPLOAD_URL = "(?:" + RELATIVE_UPLOAD_URL + "|" + ABSOLUTE_UPLOAD_URL + ")";
    private static final Pattern IMAGE_MARKDOWN_PATTERN = Pattern.compile("!\\[(.*?)]\\((" + UPLOAD_URL + "|https?://[^\\s)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)]\\((" + UPLOAD_URL + "|https?://[^\\s)]+)\\)");
    private static final Pattern UPLOAD_TEXT_IMAGE_PATTERN = Pattern.compile("([^:<\\n]+?):\\s*((" + CONTEXT_UPLOAD_PREFIX + "|" + ABSOLUTE_UPLOAD_PREFIX + ")[^\\s]+\\.(?:png|jpg|jpeg|gif|webp|svg))", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPLOAD_TEXT_LINK_PATTERN = Pattern.compile("([^:<\\n]+?):\\s*((" + CONTEXT_UPLOAD_PREFIX + "|" + ABSOLUTE_UPLOAD_PREFIX + ")[^\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");

    private MarkdownUtil() {
    }

    public static String render(String raw) {
        String source = raw == null ? "" : raw.trim();
        if (source.isBlank()) {
            return "";
        }
        String[] lines = source.split("\\r?\\n");
        StringBuilder out = new StringBuilder();
        boolean inList = false;
        boolean inQuote = false;
        List<String> paragraph = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                if (inQuote) {
                    out.append("</blockquote>");
                    inQuote = false;
                }
                flushParagraph(out, paragraph);
                continue;
            }

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                flushParagraph(out, paragraph);
                if (inQuote) {
                    out.append("</blockquote>");
                    inQuote = false;
                }
                if (!inList) {
                    out.append("<ul>");
                    inList = true;
                }
                out.append("<li>").append(inline(trimmed.substring(2).trim())).append("</li>");
                continue;
            }

            if (trimmed.startsWith(">")) {
                flushParagraph(out, paragraph);
                if (inList) {
                    out.append("</ul>");
                    inList = false;
                }
                if (!inQuote) {
                    out.append("<blockquote>");
                    inQuote = true;
                }
                out.append("<p>").append(inline(trimmed.substring(1).trim())).append("</p>");
                continue;
            }

            if (trimmed.startsWith("### ")) {
                flushParagraph(out, paragraph);
                closeOpen(out, inList, inQuote);
                inList = false;
                inQuote = false;
                out.append("<h3>").append(inline(trimmed.substring(4).trim())).append("</h3>");
                continue;
            }
            if (trimmed.startsWith("## ")) {
                flushParagraph(out, paragraph);
                closeOpen(out, inList, inQuote);
                inList = false;
                inQuote = false;
                out.append("<h2>").append(inline(trimmed.substring(3).trim())).append("</h2>");
                continue;
            }
            if (trimmed.startsWith("# ")) {
                flushParagraph(out, paragraph);
                closeOpen(out, inList, inQuote);
                inList = false;
                inQuote = false;
                out.append("<h1>").append(inline(trimmed.substring(2).trim())).append("</h1>");
                continue;
            }

            if (inList) {
                out.append("</ul>");
                inList = false;
            }
            if (inQuote) {
                out.append("</blockquote>");
                inQuote = false;
            }
            paragraph.add(trimmed);
        }

        if (inList) {
            out.append("</ul>");
        }
        if (inQuote) {
            out.append("</blockquote>");
        }
        flushParagraph(out, paragraph);
        return out.toString();
    }

    public static String normalizeCooked(String cooked) {
        if (cooked == null || cooked.isBlank()) {
            return "";
        }
        String normalized = cooked
                .replace("&lt;br&gt;", "\n")
                .replace("&lt;br/&gt;", "\n")
                .replace("&lt;br /&gt;", "\n")
                .replace("<br>", "\n")
                .replace("<br/>", "\n")
                .replace("<br />", "\n");
        normalized = replaceUploadTextImages(normalized);
        normalized = replaceMarkdownImages(normalized);
        normalized = replaceLinks(normalized);
        return normalized.replace("\n", "<br>");
    }

    private static void closeOpen(StringBuilder out, boolean inList, boolean inQuote) {
        if (inList) {
            out.append("</ul>");
        }
        if (inQuote) {
            out.append("</blockquote>");
        }
    }

    private static void flushParagraph(StringBuilder out, List<String> paragraph) {
        if (paragraph.isEmpty()) {
            return;
        }
        out.append("<p>").append(inline(String.join("<br>", paragraph))).append("</p>");
        paragraph.clear();
    }

    private static String inline(String text) {
        String escaped = HtmlUtil.escape(text);
        escaped = replaceAll(CODE_PATTERN, escaped, "<code>$1</code>");
        escaped = replaceAll(BOLD_PATTERN, escaped, "<strong>$1</strong>");
        escaped = replaceAll(ITALIC_PATTERN, escaped, "<em>$1</em>");
        escaped = replaceUploadTextImages(escaped);
        escaped = replaceMarkdownImages(escaped);
        return replaceLinks(escaped);
    }

    private static String replaceMarkdownImages(String text) {
        Matcher matcher = IMAGE_MARKDOWN_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<img src=\"" + url + "\" alt=\"" + alt + "\" loading=\"lazy\">"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceUploadTextImages(String text) {
        String withImages = replacePattern(text, UPLOAD_TEXT_IMAGE_PATTERN, true);
        return replacePattern(withImages, UPLOAD_TEXT_LINK_PATTERN, false);
    }

    private static String replacePattern(String text, Pattern pattern, boolean image) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String label = matcher.group(1).trim();
            String url = matcher.group(2).trim();
            String replacement = image
                    ? "<img src=\"" + url + "\" alt=\"" + label + "\" loading=\"lazy\">"
                    : "<a href=\"" + url + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + label + "</a>";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceLinks(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String label = matcher.group(1);
            String url = matcher.group(2);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("<a href=\"" + url + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + label + "</a>"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceAll(Pattern pattern, String text, String replacement) {
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(replacement);
    }
}
