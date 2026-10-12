package com.aitef.forum.tools;

import com.aitef.forum.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class LinuxDoImportInspector {
    private static final String SOURCE_TAG = "Linux.do";

    private LinuxDoImportInspector() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection conn = Database.getConnection()) {
            printCount(conn, "imported_topics",
                    """
                    SELECT COUNT(DISTINCT t.id)
                    FROM topics t
                    JOIN topic_tags tt ON tt.topic_id = t.id
                    JOIN tags tg ON tg.id = tt.tag_id
                    WHERE tg.name = 'Linux.do' OR tg.slug = 'linux-do'
                    """);
            printCount(conn, "imported_users",
                    "SELECT COUNT(*) FROM users WHERE username LIKE 'linuxdo\\\\_%'");

            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT t.id, t.title, c.name AS category_name, t.replies_count
                    FROM topics t
                    JOIN categories c ON c.id = t.category_id
                    JOIN topic_tags tt ON tt.topic_id = t.id
                    JOIN tags tg ON tg.id = tt.tag_id
                    WHERE tg.name = ? OR tg.slug = ?
                    ORDER BY t.id DESC
                    LIMIT 12
                    """)) {
                ps.setString(1, SOURCE_TAG);
                ps.setString(2, "linux-do");
                try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getLong("id") + " | " + rs.getString("category_name") + " | " + rs.getInt("replies_count") + " | " + rs.getString("title"));
                }
                }
            }
        }
    }

    private static void printCount(Connection conn, String label, String sql) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println(label + "=" + rs.getLong(1));
            }
        }
    }
}
