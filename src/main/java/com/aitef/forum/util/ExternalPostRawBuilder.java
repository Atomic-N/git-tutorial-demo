package com.aitef.forum.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.Locale;

public final class ExternalPostRawBuilder {
    private ExternalPostRawBuilder() {
    }

    public static String fromCookedHtml(String cookedHtml) {
        if (cookedHtml == null || cookedHtml.isBlank()) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(cookedHtml);
        document.outputSettings().prettyPrint(false);

        for (Element element : document.select("br")) {
            element.before("\n");
        }
        for (Element element : document.select("p, div, section, article, aside, blockquote, pre, ul, ol")) {
            element.append("\n");
        }
        for (Element element : document.select("li")) {
            element.prepend("- ");
            element.append("\n");
        }
        for (Element element : document.select("img")) {
            String alt = blankToFallback(element.attr("alt"), "图片");
            String src = element.attr("src");
            if (!src.isBlank()) {
                element.replaceWith(new TextNode("![" + alt + "](" + src + ")\n"));
            }
        }
        for (Element element : document.select("a")) {
            String href = element.attr("href");
            String text = element.text().trim();
            if (!href.isBlank()) {
                String label = text.isBlank() ? href : text;
                element.replaceWith(new TextNode("[" + label + "](" + href + ")"));
            }
        }
        for (Element element : document.select("pre")) {
            Element code = element.selectFirst("code");
            String codeText = code != null ? code.text() : element.text();
            String language = "";
            if (code != null) {
                for (String className : code.classNames()) {
                    if (className.startsWith("language-") || className.startsWith("lang-")) {
                        language = className.substring(className.indexOf('-') + 1).trim().toLowerCase(Locale.ROOT);
                        break;
                    }
                }
            }
            String fenced = "```" + language + "\n" + codeText.stripTrailing() + "\n```\n";
            element.replaceWith(new TextNode(fenced));
        }
        for (Element element : document.select("code")) {
            if (element.parent() != null && "pre".equals(element.parent().tagName())) {
                continue;
            }
            element.replaceWith(new TextNode("`" + element.text() + "`"));
        }

        StringBuilder builder = new StringBuilder();
        appendNodeText(document.body(), builder);
        return normalize(builder.toString());
    }

    private static void appendNodeText(Node node, StringBuilder builder) {
        if (node instanceof TextNode textNode) {
            builder.append(textNode.text());
            return;
        }
        for (Node child : node.childNodes()) {
            appendNodeText(child, builder);
        }
    }

    private static String normalize(String text) {
        String normalized = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ');
        normalized = normalized.replaceAll("[ \\t]+\\n", "\n");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private static String blankToFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
