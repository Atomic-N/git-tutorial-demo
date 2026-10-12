package com.aitef.forum.tools;

import com.aitef.forum.util.Database;
import com.aitef.forum.util.ExternalPostRawBuilder;
import com.aitef.forum.util.HtmlUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinuxDoImportedPostRepair {
    private static final String SOURCE_PREFIX = "SOURCE_LINK::https://linux.do/";
    private static final String SOURCE_DOMAIN = "https://linux.do";
    private static final Pattern TOPIC_ID_PATTERN = Pattern.compile("/(\\d+)$");
    private static final Path CACHE_DIR = cacheDir();

    private LinuxDoImportedPostRepair() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int repaired = repair(conn);
                conn.commit();
                System.out.println("repaired_posts=" + repaired);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private static int repair(Connection conn) throws Exception {
        Map<Long, CachedTopic> cachedTopicsByLocalTopicId = loadTopicCacheIndex(conn);
        int repaired = 0;
        try (PreparedStatement select = conn.prepareStatement("""
                SELECT p.id, p.post_number, p.raw_content, p.cooked_content, t.id AS topic_id, t.title
                FROM posts p
                JOIN topics t ON t.id = p.topic_id
                JOIN users u ON u.id = t.user_id
                WHERE u.username LIKE ?
                ORDER BY t.id, p.post_number
                """);
             PreparedStatement update = conn.prepareStatement("""
                UPDATE posts
                SET raw_content = ?, cooked_content = ?
                WHERE id = ?
                """)) {
            select.setString(1, "linuxdo\\_%");
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    long postId = rs.getLong("id");
                    long topicId = rs.getLong("topic_id");
                    int postNumber = rs.getInt("post_number");
                    String cookedContent = rs.getString("cooked_content");
                    String rawContent = rs.getString("raw_content");
                    String topicTitle = rs.getString("title");
                    CachedTopic cachedTopic = cachedTopicsByLocalTopicId.get(topicId);
                    RebuiltPost rebuilt = rebuildPost(postNumber, topicTitle, rawContent, cookedContent, cachedTopic);
                    if (!rebuilt.rawContent().equals(rawContent == null ? "" : rawContent)
                            || !rebuilt.cookedContent().equals(cookedContent == null ? "" : cookedContent)) {
                        update.setString(1, rebuilt.rawContent());
                        update.setString(2, rebuilt.cookedContent());
                        update.setLong(3, postId);
                        update.addBatch();
                        repaired++;
                    }
                }
            }
            if (repaired > 0) {
                update.executeBatch();
            }
        }
        return repaired;
    }

    private static Map<Long, CachedTopic> loadTopicCacheIndex(Connection conn) throws Exception {
        Map<Long, CachedTopic> cachedTopicsByLocalTopicId = new HashMap<>();
        if (CACHE_DIR == null || !Files.isDirectory(CACHE_DIR)) {
            return cachedTopicsByLocalTopicId;
        }

        Map<String, CachedTopic> cachedByTopicId = new HashMap<>();
        Map<String, CachedTopic> cachedByTitle = new HashMap<>();
        try (var stream = Files.list(CACHE_DIR)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            CachedTopic cachedTopic = parseCachedTopic(path);
                            if (cachedTopic != null) {
                                cachedByTopicId.put(cachedTopic.topicId(), cachedTopic);
                                cachedByTitle.put(normalizeTitleKey(cachedTopic.title()), cachedTopic);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT t.id, t.title, p.raw_content
                FROM topics t
                JOIN users u ON u.id = t.user_id
                JOIN posts p ON p.topic_id = t.id AND p.post_number = 1
                WHERE u.username LIKE ?
                """)) {
            ps.setString(1, "linuxdo\\_%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long topicId = rs.getLong("id");
                    String title = rs.getString("title");
                    String rawContent = rs.getString("raw_content");
                    String sourceTopicId = extractTopicIdFromRaw(rawContent);
                    CachedTopic cachedTopic = sourceTopicId == null ? null : cachedByTopicId.get(sourceTopicId);
                    if (cachedTopic == null) {
                        cachedTopic = cachedByTitle.get(normalizeTitleKey(title));
                    }
                    if (cachedTopic != null) {
                        cachedTopicsByLocalTopicId.put(topicId, cachedTopic);
                    }
                }
            }
        }
        return cachedTopicsByLocalTopicId;
    }

    private static CachedTopic parseCachedTopic(Path path) throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray posts = root.getAsJsonObject("post_stream").getAsJsonArray("posts");
        if (posts == null || posts.isEmpty()) {
            return null;
        }
        Map<Integer, CachedRemotePost> postMap = new HashMap<>();
        for (JsonElement element : posts) {
            JsonObject post = element.getAsJsonObject();
            int postType = post.has("post_type") && !post.get("post_type").isJsonNull() ? post.get("post_type").getAsInt() : 1;
            if (postType != 1) {
                continue;
            }
            int postNumber = post.get("post_number").getAsInt();
            String username = post.has("username") && !post.get("username").isJsonNull() ? post.get("username").getAsString() : "";
            String displayName = post.has("display_username") && !post.get("display_username").isJsonNull() ? post.get("display_username").getAsString() : "";
            String cooked = normalizeCookedHtml(post.has("cooked") && !post.get("cooked").isJsonNull() ? post.get("cooked").getAsString() : "");
            postMap.put(postNumber, new CachedRemotePost(postNumber, username, displayName, cooked));
        }
        String topicId = root.has("id") && !root.get("id").isJsonNull() ? root.get("id").getAsString() : stripJsonSuffix(path.getFileName().toString());
        String title = root.has("title") && !root.get("title").isJsonNull() ? root.get("title").getAsString() : "";
        return new CachedTopic(topicId, title, postMap);
    }

    private static RebuiltPost rebuildPost(int postNumber, String topicTitle, String rawContent, String cookedContent, CachedTopic cachedTopic) {
        CachedRemotePost cachedRemotePost = cachedTopic == null ? null : cachedTopic.posts().get(postNumber);
        String cookedSource = cachedRemotePost == null ? cookedContent : cachedRemotePost.cookedHtml();
        String rebuiltBody = ExternalPostRawBuilder.fromCookedHtml(cookedSource);
        if (postNumber != 1) {
            return new RebuiltPost(rebuiltBody, cookedSource == null ? "" : cookedSource);
        }

        String sourceLink = extractSourceLink(rawContent);
        if (sourceLink == null && cachedTopic != null) {
            sourceLink = SOURCE_DOMAIN + "/t/topic/" + cachedTopic.topicId();
        }
        String sourceBlock = buildSourceBlock(sourceLink, cachedRemotePost, rawContent, topicTitle);
        String rebuiltRaw = sourceBlock.isBlank() ? rebuiltBody : sourceBlock + "\n\n" + rebuiltBody;
        String rebuiltCooked = cachedRemotePost == null
                ? cookedContent
                : buildMainPostCooked(sourceLink, cachedRemotePost, cachedRemotePost.cookedHtml());
        return new RebuiltPost(rebuiltRaw, rebuiltCooked == null ? "" : rebuiltCooked);
    }

    private static String buildSourceBlock(String sourceLink, CachedRemotePost cachedRemotePost, String rawContent, String topicTitle) {
        String sourceBlock = extractSourceBlock(rawContent);
        if (!sourceBlock.isBlank()) {
            return sourceBlock;
        }
        String displayName = cachedRemotePost == null ? "" : displayName(cachedRemotePost.displayName(), cachedRemotePost.username());
        if (sourceLink == null || sourceLink.isBlank()) {
            return topicTitle == null || topicTitle.isBlank() ? "" : "搬运自 Linux.do";
        }
        return String.join("\n",
                "搬运自 Linux.do",
                "原作者：" + (displayName.isBlank() ? "未知作者" : displayName),
                "来源链接：" + sourceLink,
                "SOURCE_LINK::" + sourceLink
        ).trim();
    }

    private static String extractSourceBlock(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }
        int markerIndex = rawContent.indexOf(SOURCE_PREFIX);
        if (markerIndex < 0) {
            return "";
        }
        int afterMarker = rawContent.indexOf('\n', markerIndex);
        if (afterMarker < 0) {
            return rawContent.trim();
        }
        return rawContent.substring(0, afterMarker).trim();
    }

    private static String extractSourceLink(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }
        int markerIndex = rawContent.indexOf(SOURCE_PREFIX);
        if (markerIndex >= 0) {
            int end = rawContent.indexOf('\n', markerIndex);
            return (end >= 0 ? rawContent.substring(markerIndex + "SOURCE_LINK::".length(), end) : rawContent.substring(markerIndex + "SOURCE_LINK::".length())).trim();
        }
        Matcher matcher = Pattern.compile("来源链接：(https://linux\\.do/\\S+)").matcher(rawContent);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String extractTopicIdFromRaw(String rawContent) {
        String sourceLink = extractSourceLink(rawContent);
        if (sourceLink == null) {
            return null;
        }
        Matcher matcher = TOPIC_ID_PATTERN.matcher(sourceLink);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String normalizeTitleKey(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripJsonSuffix(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private static Path cacheDir() {
        String configured = System.getProperty("linuxdo.cache.dir");
        if (configured == null || configured.isBlank()) {
            return Path.of("data", "linuxdo-json-cache");
        }
        return Path.of(configured);
    }

    private static String normalizeCookedHtml(String cookedHtml) {
        if (cookedHtml == null || cookedHtml.isBlank()) {
            return "";
        }
        return cookedHtml
                .replace("src=\"//", "src=\"https://")
                .replace("href=\"//", "href=\"https://")
                .replace("src=\"/", "src=\"" + SOURCE_DOMAIN + "/")
                .replace("href=\"/", "href=\"" + SOURCE_DOMAIN + "/")
                .replace("src='//", "src='https://")
                .replace("href='//", "href='https://")
                .replace("src='/", "src='" + SOURCE_DOMAIN + "/")
                .replace("href='/", "href='" + SOURCE_DOMAIN + "/");
    }

    private static String buildMainPostCooked(String sourceLink, CachedRemotePost post, String cookedHtml) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"external-source-note\">")
                .append("<p><strong>来源：</strong><a href=\"").append(sourceLink).append("\" target=\"_blank\" rel=\"noopener noreferrer\">Linux.do 原帖</a></p>")
                .append("<p><strong>原作者：</strong>").append(HtmlUtil.escape(displayName(post.displayName(), post.username()))).append("</p>")
                .append("</div>");
        builder.append(cookedHtml == null ? "" : cookedHtml);
        return builder.toString();
    }

    private static String displayName(String displayName, String username) {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }

    private record CachedTopic(String topicId, String title, Map<Integer, CachedRemotePost> posts) {
    }

    private record CachedRemotePost(int postNumber, String username, String displayName, String cookedHtml) {
    }

    private record RebuiltPost(String rawContent, String cookedContent) {
    }
}
