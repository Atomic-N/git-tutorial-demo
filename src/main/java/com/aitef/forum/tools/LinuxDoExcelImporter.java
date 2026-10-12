package com.aitef.forum.tools;

import com.aitef.forum.util.Database;
import com.aitef.forum.util.ExternalPostRawBuilder;
import com.aitef.forum.util.HtmlUtil;
import com.aitef.forum.util.PasswordUtil;
import com.aitef.forum.util.SlugUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinuxDoExcelImporter {
    private static final String SHEET_NAME = "精选视图";
    private static final String SOURCE_MARKER = "SOURCE_LINK::";
    private static final String LEGACY_IMPORT_TAG = "精选导入";
    private static final String SOURCE_DOMAIN = "https://linux.do";
    private static final String SOURCE_TAG = "Linux.do";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final Pattern TAG_SPLITTER = Pattern.compile("\\s*/\\s*");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SOURCE_MARKER_PATTERN = Pattern.compile("(?m)^" + SOURCE_MARKER + "(\\S+)\\s*$");
    private static final Gson GSON = new Gson();
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ROOT);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(Integer.getInteger("linuxdo.request.timeout.seconds", 30));
    private static final Path CACHE_DIR = cacheDir();
    private static final boolean CACHE_ONLY = Boolean.getBoolean("linuxdo.cache.only");
    private static final String DEFAULT_AVATAR_PATH = "/static/images/default-avatar.svg";
    private static final String AVATAR_PUBLIC_DIR = "/static/uploads/avatars";
    private static final String PUBLIC_CONTEXT_PATH = normalizeContextPath(System.getProperty("linuxdo.public.context.path", ""));
    private static final String DEFAULT_AVATAR_URL = PUBLIC_CONTEXT_PATH + DEFAULT_AVATAR_PATH;
    private static final Path AVATAR_STORAGE_DIR = Path.of(System.getProperty(
            "linuxdo.avatar.storage.dir",
            "src/main/webapp/static/uploads/avatars"
    ));
    private static final long AVATAR_MAX_BYTES = Long.getLong("linuxdo.avatar.max.bytes", 2L * 1024L * 1024L);
    private static final Map<String, String> AVATAR_EXTENSIONS = Map.ofEntries(
            Map.entry("image/png", ".png"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/svg+xml", ".svg"),
            Map.entry("image/avif", ".avif")
    );

    private final HttpClient httpClient;
    private final String cookieHeader;
    private final String userAgent;
    private final int maxReplies;

    private final Map<String, Long> categoryIds = new HashMap<>();
    private final Map<String, Long> tagIds = new HashMap<>();
    private final Map<String, Long> importedUserIds = new HashMap<>();
    private final Map<String, String> avatarUrlCache = new HashMap<>();
    private final Set<String> existingSourceLinks = new LinkedHashSet<>();
    private final Set<Long> touchedCategoryIds = new LinkedHashSet<>();
    private final Set<Long> touchedTagIds = new LinkedHashSet<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: LinuxDoExcelImporter <excelPath> <base64CookieHeader> <base64UserAgent> [maxReplies]");
            System.exit(1);
        }

        Path excelPath = Path.of(args[0]);
        String cookieHeader = new String(Base64.getDecoder().decode(args[1]), StandardCharsets.UTF_8);
        String userAgent = new String(Base64.getDecoder().decode(args[2]), StandardCharsets.UTF_8);
        int maxReplies = args.length >= 4 ? Math.max(0, Integer.parseInt(args[3])) : 5;

        LinuxDoExcelImporter importer = new LinuxDoExcelImporter(cookieHeader, userAgent, maxReplies);
        importer.run(excelPath);
    }

    private LinuxDoExcelImporter(String cookieHeader, String userAgent, int maxReplies) {
        this.cookieHeader = cookieHeader;
        this.userAgent = userAgent;
        this.maxReplies = maxReplies;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private void run(Path excelPath) throws Exception {
        int limit = Integer.getInteger("linuxdo.import.limit", Integer.MAX_VALUE);
        List<ExcelRow> rows = readRows(excelPath);
        if (rows.size() > limit) {
            rows = new ArrayList<>(rows.subList(0, limit));
        }
        System.out.println("Excel rows ready: " + rows.size());
        if (CACHE_DIR != null) {
            System.out.println("Using cache dir: " + CACHE_DIR);
        }
        System.out.println("Avatar storage dir: " + AVATAR_STORAGE_DIR.toAbsolutePath().normalize());
        System.out.println("Avatar public base: " + PUBLIC_CONTEXT_PATH + AVATAR_PUBLIC_DIR);

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                cleanupLegacySummaryImports(conn);
                preloadCaches(conn);
                repairImportedAvatars(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            int imported = 0;
            int skipped = 0;
            int failed = 0;

            for (ExcelRow row : rows) {
                String sourceLink = normalizeSourceLink(row.sourceLink());
                if (sourceLink == null) {
                    skipped++;
                    System.out.println("Skip row without valid source link: " + row.title());
                    continue;
                }
                if (existingSourceLinks.contains(sourceLink)) {
                    skipped++;
                    System.out.println("Skip existing topic: " + row.title());
                    continue;
                }

                try {
                    System.out.println("Fetching: " + row.title() + " -> " + sourceLink);
                    RemoteTopic remoteTopic = fetchRemoteTopic(sourceLink);
                    conn.setAutoCommit(false);
                    try {
                        importTopic(conn, row, remoteTopic);
                        conn.commit();
                        existingSourceLinks.add(sourceLink);
                        imported++;
                        System.out.println("Imported: " + remoteTopic.title());
                    } catch (Exception e) {
                        conn.rollback();
                        failed++;
                        System.err.println("Failed: " + row.title() + " -> " + e.getMessage());
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (Exception e) {
                    failed++;
                    System.err.println("Fetch failed: " + row.title() + " -> " + e);
                    e.printStackTrace(System.err);
                }
            }

            conn.setAutoCommit(false);
            try {
                refreshAllCounts(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            System.out.println("Import completed. imported=" + imported + ", skipped=" + skipped + ", failed=" + failed);
        }
    }

    private List<ExcelRow> readRows(Path excelPath) throws Exception {
        if (!Files.exists(excelPath)) {
            throw new IllegalArgumentException("Excel file not found: " + excelPath);
        }

        List<ExcelRow> rows = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet not found: " + SHEET_NAME);
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                headers.put(FORMATTER.formatCellValue(cell).trim(), cell.getColumnIndex());
            }

            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String title = readCell(row, headers.get("标题"));
                String themes = readCell(row, headers.get("主题"));
                String author = readCell(row, headers.get("作者"));
                String publishedAt = readCell(row, headers.get("发布时间"));
                String sourceLink = readHyperlink(row, headers.get("链接"));
                if (title.isBlank() || sourceLink == null || sourceLink.isBlank()) {
                    continue;
                }
                rows.add(new ExcelRow(themes, title, author, publishedAt, sourceLink));
            }
        }
        return rows;
    }

    private String readCell(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return "";
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
    }

    private String readHyperlink(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        if (cell.getHyperlink() != null && cell.getHyperlink().getAddress() != null) {
            return cell.getHyperlink().getAddress().trim();
        }
        String value = FORMATTER.formatCellValue(cell).trim();
        return value.startsWith("http://") || value.startsWith("https://") ? value : null;
    }

    private void cleanupLegacySummaryImports(Connection conn) throws Exception {
        String findLegacyTopicsSql = """
                SELECT DISTINCT t.id
                FROM topics t
                JOIN topic_tags tt ON tt.topic_id = t.id
                JOIN tags tg ON tg.id = tt.tag_id
                WHERE tg.slug = ? OR tg.name = ?
                """;
        List<Long> legacyTopicIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(findLegacyTopicsSql)) {
            ps.setString(1, SlugUtil.fromTitle(LEGACY_IMPORT_TAG));
            ps.setString(2, LEGACY_IMPORT_TAG);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    legacyTopicIds.add(rs.getLong(1));
                }
            }
        }

        if (legacyTopicIds.isEmpty()) {
            return;
        }

        String placeholders = placeholders(legacyTopicIds.size());
        deleteByTopicTarget(conn, "DELETE FROM reactions WHERE target_type = 'TOPIC' AND target_id IN (" + placeholders + ")", legacyTopicIds);
        deleteByTopicTarget(conn, "DELETE FROM bookmarks WHERE target_type = 'TOPIC' AND target_id IN (" + placeholders + ")", legacyTopicIds);
        deleteByTopicTarget(conn, "DELETE FROM notifications WHERE target_type = 'TOPIC' AND target_id IN (" + placeholders + ")", legacyTopicIds);
        deleteByTopicTarget(conn, "DELETE FROM reports WHERE target_type = 'TOPIC' AND target_id IN (" + placeholders + ")", legacyTopicIds);

        List<Long> legacyPostIds = new ArrayList<>();
        String findLegacyPostsSql = "SELECT id FROM posts WHERE topic_id IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(findLegacyPostsSql)) {
            bindIds(ps, legacyTopicIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    legacyPostIds.add(rs.getLong(1));
                }
            }
        }
        if (!legacyPostIds.isEmpty()) {
            String postPlaceholders = placeholders(legacyPostIds.size());
            deleteByTopicTarget(conn, "DELETE FROM reactions WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", legacyPostIds);
            deleteByTopicTarget(conn, "DELETE FROM bookmarks WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", legacyPostIds);
            deleteByTopicTarget(conn, "DELETE FROM notifications WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", legacyPostIds);
            deleteByTopicTarget(conn, "DELETE FROM reports WHERE target_type = 'POST' AND target_id IN (" + postPlaceholders + ")", legacyPostIds);
        }

        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM topics WHERE id IN (" + placeholders + ")")) {
            bindIds(ps, legacyTopicIds);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM users
                WHERE username LIKE ?
                  AND NOT EXISTS (SELECT 1 FROM topics t WHERE t.user_id = users.id)
                  AND NOT EXISTS (SELECT 1 FROM posts p WHERE p.user_id = users.id)
                """)) {
            ps.setString(1, "linuxdo\\_%");
            ps.executeUpdate();
        }

        System.out.println("Cleaned legacy summary imports: " + legacyTopicIds.size());
    }

    private void deleteByTopicTarget(Connection conn, String sql, List<Long> ids) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bindIds(ps, ids);
            ps.executeUpdate();
        }
    }

    private void preloadCaches(Connection conn) throws Exception {
        categoryIds.clear();
        tagIds.clear();
        importedUserIds.clear();
        existingSourceLinks.clear();

        try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM categories");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categoryIds.put(rs.getString("name"), rs.getLong("id"));
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM tags");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tagIds.put(rs.getString("name"), rs.getLong("id"));
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT id, username FROM users WHERE username LIKE ?")) {
            ps.setString(1, "linuxdo\\_%");
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                importedUserIds.put(rs.getString("username"), rs.getLong("id"));
            }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT p.raw_content
                FROM posts p
                WHERE p.post_number = 1 AND p.raw_content LIKE ?
                """)) {
            ps.setString(1, "%" + SOURCE_MARKER + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String source = extractSourceMarker(rs.getString(1));
                    if (source != null) {
                        existingSourceLinks.add(source);
                    }
                }
            }
        }
    }

    private RemoteTopic fetchRemoteTopic(String sourceLink) throws Exception {
        String cachedJson = readCachedTopicJson(sourceLink);
        if (cachedJson != null) {
            return parseRemoteTopic(sourceLink, cachedJson);
        }
        if (CACHE_ONLY) {
            throw new IllegalStateException("Cache miss for " + sourceLink);
        }

        String jsonUrl = sourceLink.endsWith(".json") ? sourceLink : sourceLink + ".json";
        HttpRequest request = HttpRequest.newBuilder(URI.create(jsonUrl))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json,text/plain,*/*")
                .header("Cookie", cookieHeader)
                .header("Referer", sourceLink)
                .header("User-Agent", userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return parseRemoteTopic(sourceLink, response.body());
    }

    private RemoteTopic parseRemoteTopic(String sourceLink, String jsonText) {
        JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
        JsonObject postStream = root.getAsJsonObject("post_stream");
        JsonArray postArray = postStream.getAsJsonArray("posts");
        if (postArray == null || postArray.isEmpty()) {
            throw new IllegalStateException("No posts in remote topic");
        }

        List<RemotePost> posts = new ArrayList<>();
        for (JsonElement element : postArray) {
            JsonObject post = element.getAsJsonObject();
            if (readInt(post, "post_type", 1) != 1) {
                continue;
            }
            posts.add(new RemotePost(
                    readInt(post, "post_number", 0),
                    readString(post, "username"),
                    readOptionalString(post, "display_username"),
                    normalizeAvatar(readOptionalString(post, "avatar_template")),
                    normalizeCookedHtml(readString(post, "cooked")),
                    toLocalDateTime(readString(post, "created_at")),
                    post.has("reply_to_post_number") && !post.get("reply_to_post_number").isJsonNull() ? post.get("reply_to_post_number").getAsInt() : null
            ));
        }

        List<String> remoteTags = new ArrayList<>();
        JsonArray tags = root.getAsJsonArray("tags");
        if (tags != null) {
            for (JsonElement element : tags) {
                String tag;
                if (element.isJsonPrimitive()) {
                    tag = element.getAsString();
                } else if (element.isJsonObject()) {
                    JsonObject tagObject = element.getAsJsonObject();
                    tag = readOptionalString(tagObject, "name");
                    if (tag == null) {
                        tag = readOptionalString(tagObject, "slug");
                    }
                } else {
                    tag = null;
                }
                if (tag != null && !tag.isBlank()) {
                    remoteTags.add(tag.trim());
                }
            }
        }

        return new RemoteTopic(
                normalizeSourceLink(sourceLink),
                readString(root, "title"),
                readInt(root, "views", 0),
                posts,
                remoteTags
        );
    }

    private String readCachedTopicJson(String sourceLink) throws Exception {
        if (CACHE_DIR == null) {
            return null;
        }
        String topicId = extractTopicId(sourceLink);
        if (topicId == null) {
            System.out.println("Cache skip: no topic id for " + sourceLink);
            return null;
        }
        Path cacheFile = CACHE_DIR.resolve(topicId + ".json");
        System.out.println("Cache probe: " + cacheFile);
        if (!Files.exists(cacheFile)) {
            System.out.println("Cache miss file: " + cacheFile);
            return null;
        }
        System.out.println("Cache hit: " + cacheFile);
        return Files.readString(cacheFile, StandardCharsets.UTF_8);
    }

    private void importTopic(Connection conn, ExcelRow row, RemoteTopic remoteTopic) throws Exception {
        if (remoteTopic.posts().isEmpty()) {
            throw new IllegalStateException("Remote topic has no visible posts");
        }

        RemotePost mainPost = remoteTopic.posts().get(0);
        Long categoryId = ensureCategory(conn, resolvePrimaryCategory(row.themes()));
        Long authorId = ensureImportedUser(conn, mainPost.username(), mainPost.displayName(), mainPost.avatarUrl());

        Long topicId = insertTopic(
                conn,
                authorId,
                categoryId,
                remoteTopic.title(),
                HtmlUtil.excerpt(buildMainPostRaw(remoteTopic, mainPost), 180),
                remoteTopic.views(),
                mainPost.createdAt()
        );

        Map<Integer, Long> localPostIdsByNumber = new HashMap<>();
        Long firstPostId = insertPost(
                conn,
                topicId,
                authorId,
                null,
                mainPost.postNumber(),
                buildMainPostRaw(remoteTopic, mainPost),
                buildMainPostCooked(remoteTopic, mainPost),
                mainPost.createdAt()
        );
        localPostIdsByNumber.put(mainPost.postNumber(), firstPostId);

        int importedReplies = 0;
        LocalDateTime latestPostTime = mainPost.createdAt();
        Long lastPostId = firstPostId;
        for (int i = 1; i < remoteTopic.posts().size() && importedReplies < maxReplies; i++) {
            RemotePost reply = remoteTopic.posts().get(i);
            Long replyUserId = ensureImportedUser(conn, reply.username(), reply.displayName(), reply.avatarUrl());
            Long replyToPostId = reply.replyToPostNumber() == null ? null : localPostIdsByNumber.get(reply.replyToPostNumber());
            Long postId = insertPost(
                    conn,
                    topicId,
                    replyUserId,
                    replyToPostId,
                    reply.postNumber(),
                    buildReplyRaw(reply),
                    reply.cookedHtml(),
                    reply.createdAt()
            );
            localPostIdsByNumber.put(reply.postNumber(), postId);
            importedReplies++;
            if (reply.createdAt().isAfter(latestPostTime)) {
                latestPostTime = reply.createdAt();
                lastPostId = postId;
            } else if (reply.postNumber() > mainPost.postNumber()) {
                lastPostId = postId;
            }
        }

        updateTopicAfterImport(conn, topicId, firstPostId, lastPostId, latestPostTime);
        attachTags(conn, topicId, buildTags(row.themes(), remoteTopic.remoteTags()));
        touchedCategoryIds.add(categoryId);
    }

    private Long ensureCategory(Connection conn, String categoryName) throws Exception {
        if (categoryName == null || categoryName.isBlank()) {
            categoryName = "资源荟萃";
        }
        Long existingId = categoryIds.get(categoryName);
        if (existingId != null) {
            return existingId;
        }

        CategorySpec spec = categorySpec(categoryName);
        try (PreparedStatement find = conn.prepareStatement("SELECT id FROM categories WHERE name = ? OR slug = ?")) {
            find.setString(1, spec.name());
            find.setString(2, spec.slug());
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    categoryIds.put(spec.name(), id);
                    return id;
                }
            }
        }

        int position = 1;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(position), 0) + 1 FROM categories");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                position = rs.getInt(1);
            }
        }

        String sql = """
                INSERT INTO categories (name, slug, description, color, text_color, icon, position, min_trust_level)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, spec.name());
            ps.setString(2, spec.slug());
            ps.setString(3, spec.description());
            ps.setString(4, spec.color());
            ps.setString(5, spec.textColor());
            ps.setString(6, spec.icon());
            ps.setInt(7, position);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    categoryIds.put(spec.name(), id);
                    return id;
                }
            }
        }
        throw new IllegalStateException("Failed to create category: " + categoryName);
    }

    private Long ensureImportedUser(Connection conn, String remoteUsername, String remoteDisplayName, String avatarUrl) throws Exception {
        String localUsername = buildImportedUsername(remoteUsername);
        Long cachedId = importedUserIds.get(localUsername);
        String displayName = (remoteDisplayName == null || remoteDisplayName.isBlank()) ? remoteUsername : remoteDisplayName;
        String finalAvatar = resolveImportedAvatar(avatarUrl, localUsername);

        if (cachedId != null) {
            updateImportedUser(conn, cachedId, displayName, finalAvatar);
            return cachedId;
        }

        try (PreparedStatement find = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            find.setString(1, localUsername);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    importedUserIds.put(localUsername, id);
                    updateImportedUser(conn, id, displayName, finalAvatar);
                    return id;
                }
            }
        }

        String sql = """
                INSERT INTO users (username, email, password_hash, display_name, avatar_url, bio, trust_level, role, status)
                VALUES (?, ?, ?, ?, ?, ?, 0, 'USER', 'ACTIVE')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, localUsername);
            ps.setString(2, localUsername + "@linuxdo.import.local");
            ps.setString(3, PasswordUtil.hash("linuxdo-import-" + remoteUsername));
            ps.setString(4, displayName);
            ps.setString(5, finalAvatar);
            ps.setString(6, "搬运自 Linux.do 的外部用户资料。");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    importedUserIds.put(localUsername, id);
                    return id;
                }
            }
        }
        throw new IllegalStateException("Failed to create imported user: " + remoteUsername);
    }

    private void repairImportedAvatars(Connection conn) throws Exception {
        List<UserAvatarRepair> repairs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT id, username, avatar_url
                FROM users
                WHERE username LIKE ?
                """)) {
            ps.setString(1, "linuxdo\\_%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String avatarUrl = rs.getString("avatar_url");
                    if (shouldRepairAvatar(avatarUrl)) {
                        repairs.add(new UserAvatarRepair(
                                rs.getLong("id"),
                                rs.getString("username"),
                                avatarUrl
                        ));
                    }
                }
            }
        }

        int repaired = 0;
        for (UserAvatarRepair repair : repairs) {
            String localAvatar = resolveImportedAvatar(repair.avatarUrl(), repair.username());
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET avatar_url = ? WHERE id = ?")) {
                ps.setString(1, localAvatar);
                ps.setLong(2, repair.userId());
                repaired += ps.executeUpdate();
            }
        }
        if (!repairs.isEmpty()) {
            System.out.println("Repaired imported avatars: " + repaired + "/" + repairs.size());
        }
    }

    private boolean shouldRepairAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return true;
        }
        String normalized = avatarUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("https://linux.do/")
                || normalized.startsWith("http://linux.do/")
                || normalized.startsWith("//linux.do/")
                || normalized.startsWith("https://cdn.ldstatic.com/")
                || normalized.startsWith("http://cdn.ldstatic.com/")
                || normalized.startsWith("https://api.dicebear.com/")
                || normalized.startsWith("http://api.dicebear.com/");
    }

    private String resolveImportedAvatar(String avatarUrl, String localUsername) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return DEFAULT_AVATAR_URL;
        }
        String normalized = normalizeAvatar(avatarUrl);
        if (normalized == null || normalized.isBlank()) {
            return DEFAULT_AVATAR_URL;
        }
        if (!isRemoteUrl(normalized)) {
            return normalized;
        }

        String cached = avatarUrlCache.get(normalized);
        if (cached != null) {
            return cached;
        }

        String localAvatar = localizeAvatar(normalized, localUsername);
        avatarUrlCache.put(normalized, localAvatar);
        return localAvatar;
    }

    private String localizeAvatar(String avatarUrl, String localUsername) {
        try {
            URI uri = URI.create(avatarUrl);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Cookie", cookieHeader)
                    .header("Referer", SOURCE_DOMAIN + "/")
                    .header("User-Agent", userAgent)
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }

            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (contentLength > AVATAR_MAX_BYTES) {
                throw new IllegalStateException("avatar is too large: " + contentLength);
            }

            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > AVATAR_MAX_BYTES) {
                throw new IllegalStateException("invalid avatar size: " + (body == null ? 0 : body.length));
            }

            String contentType = normalizeContentType(response.headers().firstValue("Content-Type").orElse(""));
            if (!contentType.isBlank() && !contentType.startsWith("image/")) {
                throw new IllegalStateException("unexpected avatar content type: " + contentType);
            }

            String extension = avatarExtension(contentType, response.uri());
            String filename = safeAvatarPrefix(localUsername) + "-" + sha256Hex(avatarUrl).substring(0, 20) + extension;
            Files.createDirectories(AVATAR_STORAGE_DIR);
            Path target = AVATAR_STORAGE_DIR.resolve(filename).normalize();
            Path root = AVATAR_STORAGE_DIR.toAbsolutePath().normalize();
            if (!target.toAbsolutePath().normalize().startsWith(root)) {
                throw new IllegalStateException("invalid avatar target path");
            }
            Files.write(target, body);
            return PUBLIC_CONTEXT_PATH + AVATAR_PUBLIC_DIR + "/" + filename;
        } catch (Exception e) {
            System.err.println("Avatar localize failed: " + avatarUrl + " -> " + e.getMessage());
            return DEFAULT_AVATAR_URL;
        }
    }

    private void updateImportedUser(Connection conn, Long userId, String displayName, String avatarUrl) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE users
                SET display_name = ?, avatar_url = ?, bio = ?
                WHERE id = ?
                """)) {
            ps.setString(1, displayName);
            ps.setString(2, avatarUrl);
            ps.setString(3, "搬运自 Linux.do 的外部用户资料。");
            ps.setLong(4, userId);
            ps.executeUpdate();
        }
    }

    private Long insertTopic(Connection conn, Long userId, Long categoryId, String title, String excerpt, int viewsCount, LocalDateTime createdAt) throws Exception {
        String sql = """
                INSERT INTO topics (user_id, category_id, title, slug, excerpt, views_count, likes_count, bookmarks_count,
                                    status, pinned, solved, last_post_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, 0, 'NORMAL', FALSE, FALSE, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp timestamp = Timestamp.valueOf(createdAt);
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.setString(3, title);
            ps.setString(4, SlugUtil.fromTitle(title));
            ps.setString(5, excerpt);
            ps.setInt(6, Math.max(0, viewsCount));
            ps.setTimestamp(7, timestamp);
            ps.setTimestamp(8, timestamp);
            ps.setTimestamp(9, timestamp);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new IllegalStateException("Failed to create topic");
    }

    private Long insertPost(Connection conn, Long topicId, Long userId, Long replyToPostId, int postNumber, String rawContent, String cookedContent, LocalDateTime createdAt) throws Exception {
        String sql = """
                INSERT INTO posts (topic_id, user_id, reply_to_post_id, post_number, raw_content, cooked_content, likes_count, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, 'NORMAL', ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            Timestamp timestamp = Timestamp.valueOf(createdAt);
            ps.setLong(1, topicId);
            ps.setLong(2, userId);
            if (replyToPostId == null) {
                ps.setNull(3, java.sql.Types.BIGINT);
            } else {
                ps.setLong(3, replyToPostId);
            }
            ps.setInt(4, postNumber);
            ps.setString(5, rawContent);
            ps.setString(6, cookedContent);
            ps.setTimestamp(7, timestamp);
            ps.setTimestamp(8, timestamp);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new IllegalStateException("Failed to create post");
    }

    private void updateTopicAfterImport(Connection conn, Long topicId, Long firstPostId, Long lastPostId, LocalDateTime lastPostAt) throws Exception {
        String sql = """
                UPDATE topics t
                SET replies_count = GREATEST(0, (SELECT COUNT(*) FROM posts p WHERE p.topic_id = t.id AND p.status <> 'DELETED') - 1),
                    last_post_id = ?,
                    last_post_at = ?,
                    updated_at = ?,
                    solution_post_id = NULL,
                    solved = FALSE
                WHERE t.id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp lastTimestamp = Timestamp.valueOf(lastPostAt);
            ps.setLong(1, lastPostId == null ? firstPostId : lastPostId);
            ps.setTimestamp(2, lastTimestamp);
            ps.setTimestamp(3, lastTimestamp);
            ps.setLong(4, topicId);
            ps.executeUpdate();
        }
    }

    private void attachTags(Connection conn, Long topicId, Set<String> names) throws Exception {
        for (String name : names) {
            Long tagId = ensureTag(conn, name);
            try (PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO topic_tags (topic_id, tag_id) VALUES (?, ?)")) {
                ps.setLong(1, topicId);
                ps.setLong(2, tagId);
                ps.executeUpdate();
            }
            touchedTagIds.add(tagId);
        }
    }

    private Long ensureTag(Connection conn, String name) throws Exception {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name is blank");
        }
        Long cachedId = tagIds.get(name);
        if (cachedId != null) {
            return cachedId;
        }

        String slug = SlugUtil.fromTitle(name).toLowerCase(Locale.ROOT);
        try (PreparedStatement find = conn.prepareStatement("SELECT id, name FROM tags WHERE name = ? OR slug = ?")) {
            find.setString(1, name);
            find.setString(2, slug);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    tagIds.put(name, id);
                    return id;
                }
            }
        }

        String sql = """
                INSERT INTO tags (name, slug, tag_group_id, group_name, color, icon)
                VALUES (?, ?, (SELECT id FROM tag_groups WHERE name = '工程实践' LIMIT 1), '工程实践', '#64748b', 'hash')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, slug);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    tagIds.put(name, id);
                    return id;
                }
            }
        }
        throw new IllegalStateException("Failed to create tag: " + name);
    }

    private Set<String> buildTags(String themes, List<String> remoteTags) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(SOURCE_TAG);
        for (String theme : splitThemes(themes)) {
            tags.add(theme);
        }
        for (String remoteTag : remoteTags) {
            if (tags.size() >= 8) {
                break;
            }
            tags.add(remoteTag);
        }
        return tags;
    }

    private String resolvePrimaryCategory(String themes) {
        List<String> split = splitThemes(themes);
        return split.isEmpty() ? "资源荟萃" : split.get(0);
    }

    private List<String> splitThemes(String themes) {
        List<String> values = new ArrayList<>();
        if (themes == null || themes.isBlank()) {
            return values;
        }
        for (String part : TAG_SPLITTER.split(themes.trim())) {
            String value = part == null ? "" : part.trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private CategorySpec categorySpec(String categoryName) {
        return switch (categoryName) {
            case "Agent 技巧" -> new CategorySpec("Agent 技巧", "agent-techniques", "收集 Agent、CLI、工作流自动化与提效实践。", "#2563eb", "#ffffff", "workflow");
            case "AI 技巧" -> new CategorySpec("AI 技巧", "ai-techniques", "聚焦模型用法、提示、评测和落地技巧。", "#7c3aed", "#ffffff", "brain");
            case "有趣项目" -> new CategorySpec("有趣项目", "interesting-projects", "搬运和沉淀值得关注的项目、脚本与工具链。", "#0f766e", "#ffffff", "package-open");
            case "故障排查" -> new CategorySpec("故障排查", "troubleshooting", "收集真实排障案例、兼容问题和修复方案。", "#dc2626", "#ffffff", "wrench");
            default -> new CategorySpec("资源荟萃", "resources", "工具、数据集、教程、开源项目和可复用资料分享。", "#12a89d", "#ffffff", "share-2");
        };
    }

    private String buildMainPostRaw(RemoteTopic remoteTopic, RemotePost mainPost) {
        String bodyText = ExternalPostRawBuilder.fromCookedHtml(mainPost.cookedHtml());
        return String.join("\n",
                "搬运自 Linux.do",
                "原作者：" + displayName(mainPost.displayName(), mainPost.username()),
                "来源链接：" + remoteTopic.sourceLink(),
                SOURCE_MARKER + remoteTopic.sourceLink(),
                "",
                bodyText
        ).trim();
    }

    private String buildReplyRaw(RemotePost reply) {
        return ExternalPostRawBuilder.fromCookedHtml(reply.cookedHtml());
    }

    private String buildMainPostCooked(RemoteTopic remoteTopic, RemotePost mainPost) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"external-source-note\">")
                .append("<p><strong>来源：</strong><a href=\"").append(remoteTopic.sourceLink()).append("\" target=\"_blank\" rel=\"noopener noreferrer\">Linux.do 原帖</a></p>")
                .append("<p><strong>原作者：</strong>").append(HtmlUtil.escape(displayName(mainPost.displayName(), mainPost.username()))).append("</p>")
                .append("</div>");
        builder.append(mainPost.cookedHtml());
        return builder.toString();
    }

    private String displayName(String displayName, String username) {
        return (displayName == null || displayName.isBlank()) ? username : displayName;
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = HTML_TAG_PATTERN.matcher(html).replaceAll(" ");
        text = text.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&#x27;", "'");
        return text.replaceAll("\\s+", " ").trim();
    }

    private String normalizeAvatar(String avatarTemplate) {
        if (avatarTemplate == null || avatarTemplate.isBlank()) {
            return null;
        }
        String value = avatarTemplate.replace("{size}", "120");
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        if (value.startsWith("/")) {
            return SOURCE_DOMAIN + value;
        }
        return value;
    }

    private boolean isRemoteUrl(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private String normalizeContentType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).split(";")[0].trim();
    }

    private String avatarExtension(String contentType, URI uri) {
        String extension = AVATAR_EXTENSIONS.get(contentType);
        if (extension != null) {
            return extension;
        }
        String path = uri == null ? "" : uri.getPath();
        int dot = path == null ? -1 : path.lastIndexOf('.');
        if (dot >= 0 && dot < path.length() - 1) {
            String suffix = path.substring(dot).toLowerCase(Locale.ROOT);
            if (suffix.matches("\\.(png|jpe?g|gif|webp|svg|avif)")) {
                return suffix.equals(".jpeg") ? ".jpg" : suffix;
            }
        }
        return ".img";
    }

    private String safeAvatarPrefix(String username) {
        String value = username == null ? "linuxdo_user" : username.toLowerCase(Locale.ROOT);
        value = value.replaceAll("[^a-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
        if (value.isBlank()) {
            value = "linuxdo_user";
        }
        return value.length() <= 48 ? value : value.substring(0, 48);
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String normalizeContextPath(String value) {
        if (value == null || value.isBlank() || "/".equals(value.trim())) {
            return "";
        }
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeCookedHtml(String cookedHtml) {
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

    private String buildImportedUsername(String remoteUsername) {
        String sanitized = remoteUsername == null ? "user" : remoteUsername.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        sanitized = sanitized.replaceAll("^_+|_+$", "");
        if (sanitized.isBlank()) {
            sanitized = "user";
        }
        if (sanitized.length() > 18) {
            sanitized = sanitized.substring(0, 18);
        }
        String suffix = Integer.toHexString(Math.abs((remoteUsername == null ? "user" : remoteUsername).hashCode()));
        if (suffix.length() > 6) {
            suffix = suffix.substring(0, 6);
        }
        return "linuxdo_" + sanitized + "_" + suffix;
    }

    private String normalizeSourceLink(String sourceLink) {
        if (sourceLink == null || sourceLink.isBlank()) {
            return null;
        }
        URI uri = URI.create(sourceLink.trim());
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - 5);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return SOURCE_DOMAIN + path;
    }

    private String extractTopicId(String sourceLink) {
        String normalized = normalizeSourceLink(sourceLink);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("/(\\d+)$").matcher(URI.create(normalized).getPath());
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractSourceMarker(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }
        Matcher matcher = SOURCE_MARKER_PATTERN.matcher(rawContent);
        return matcher.find() ? normalizeSourceLink(matcher.group(1)) : null;
    }

    private LocalDateTime toLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(ZONE_ID);
        }
        return LocalDateTime.ofInstant(Instant.parse(value), ZONE_ID);
    }

    private String readString(JsonObject jsonObject, String field) {
        return jsonObject.has(field) && !jsonObject.get(field).isJsonNull() ? jsonObject.get(field).getAsString() : "";
    }

    private String readOptionalString(JsonObject jsonObject, String field) {
        String value = readString(jsonObject, field);
        return value.isBlank() ? null : value;
    }

    private int readInt(JsonObject jsonObject, String field, int defaultValue) {
        return jsonObject.has(field) && !jsonObject.get(field).isJsonNull() ? jsonObject.get(field).getAsInt() : defaultValue;
    }

    private void refreshAllCounts(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE users u
                SET topics_count = (SELECT COUNT(*) FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'),
                    posts_count = (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'),
                    likes_received_count = (
                        SELECT COUNT(*)
                        FROM reactions r
                        WHERE (r.target_type = 'TOPIC' AND r.target_id IN (SELECT t.id FROM topics t WHERE t.user_id = u.id AND t.status <> 'DELETED'))
                           OR (r.target_type = 'POST' AND r.target_id IN (SELECT p.id FROM posts p WHERE p.user_id = u.id AND p.status <> 'DELETED'))
                    )
                """)) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE categories c
                SET topic_count = (SELECT COUNT(*) FROM topics t WHERE t.category_id = c.id AND t.status <> 'DELETED'),
                    post_count = (
                        SELECT COUNT(*)
                        FROM posts p
                        JOIN topics t ON t.id = p.topic_id
                        WHERE t.category_id = c.id AND t.status <> 'DELETED' AND p.status <> 'DELETED'
                    )
                """)) {
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE tags
                SET topic_count = (SELECT COUNT(*) FROM topic_tags tt WHERE tt.tag_id = tags.id)
                """)) {
            ps.executeUpdate();
        }
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }

    private void bindIds(PreparedStatement ps, List<Long> ids) throws Exception {
        for (int i = 0; i < ids.size(); i++) {
            ps.setLong(i + 1, ids.get(i));
        }
    }

    private static Path cacheDir() {
        String configured = System.getProperty("linuxdo.cache.dir");
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return Path.of(configured);
    }

    private record ExcelRow(String themes, String title, String author, String publishedAt, String sourceLink) {
    }

    private record RemoteTopic(String sourceLink, String title, int views, List<RemotePost> posts, List<String> remoteTags) {
    }

    private record RemotePost(int postNumber, String username, String displayName, String avatarUrl,
                              String cookedHtml, LocalDateTime createdAt, Integer replyToPostNumber) {
    }

    private record UserAvatarRepair(long userId, String username, String avatarUrl) {
    }

    private record CategorySpec(String name, String slug, String description, String color, String textColor, String icon) {
    }
}
