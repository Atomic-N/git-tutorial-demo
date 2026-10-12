package com.aitef.forum.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库连接测试工具
 * 用于快速诊断数据库连接问题
 */
public class DatabaseConnectionTest {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/ai_tef?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = System.getenv().getOrDefault("AI_TEF_DB_PASSWORD", "425603");

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("AI TEF 数据库连接测试");
        System.out.println("========================================\n");

        // 测试1: 加载驱动
        System.out.println("[1] 测试 MySQL 驱动加载...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL 驱动加载成功\n");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL 驱动未找到: " + e.getMessage());
            System.err.println("请确保 pom.xml 中包含 mysql-connector-j 依赖\n");
            return;
        }

        // 测试2: 建立连接
        System.out.println("[2] 测试数据库连接...");
        System.out.println("URL: " + JDBC_URL);
        System.out.println("用户名: " + USERNAME);

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            System.out.println("✓ 数据库连接成功\n");

            // 测试3: 检查数据库版本
            System.out.println("[3] 检查 MySQL 版本...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT VERSION() AS version")) {
                if (rs.next()) {
                    System.out.println("MySQL 版本: " + rs.getString("version") + "\n");
                }
            }

            // 测试4: 检查私信相关表
            System.out.println("[4] 检查私信系统表...");
            String[] tables = {
                "direct_message_threads",
                "direct_message_participants",
                "direct_messages",
                "direct_message_thread_meta"
            };

            for (String table : tables) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM " + table)) {
                    if (rs.next()) {
                        System.out.println("✓ " + table + " 表存在，记录数: " + rs.getInt("cnt"));
                    }
                } catch (Exception e) {
                    System.err.println("✗ " + table + " 表检查失败: " + e.getMessage());
                }
            }
            System.out.println();

            // 测试5: 检查 users 表
            System.out.println("[5] 检查用户表...");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM users")) {
                if (rs.next()) {
                    System.out.println("✓ users 表存在，用户数: " + rs.getInt("cnt") + "\n");
                }
            }

            // 测试6: 测试私信查询
            System.out.println("[6] 测试私信列表查询...");
            String testQuery = """
                SELECT COUNT(*) AS thread_count
                FROM direct_message_threads
                """;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(testQuery)) {
                if (rs.next()) {
                    System.out.println("✓ 私信会话总数: " + rs.getInt("thread_count") + "\n");
                }
            }

            System.out.println("========================================");
            System.out.println("✓ 所有测试通过！数据库配置正常");
            System.out.println("========================================");

        } catch (java.sql.SQLException e) {
            System.err.println("✗ 数据库连接失败\n");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("\n可能的原因:");
            System.err.println("1. MySQL 服务未启动");
            System.err.println("2. 数据库 'ai_tef' 不存在");
            System.err.println("3. 用户名或密码错误");
            System.err.println("4. 端口 3306 被占用或防火墙阻止");
            System.err.println("\n建议操作:");
            System.err.println("- 运行 diagnose-db.bat 进行诊断");
            System.err.println("- 检查 src/main/resources/db.properties 配置");
        }
    }
}
