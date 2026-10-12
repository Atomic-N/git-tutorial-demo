package com.aitef.forum.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static final HikariDataSource DATA_SOURCE = createDataSource();

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    private static HikariDataSource createDataSource() {
        Properties properties = new Properties();
        try (InputStream inputStream = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(value(properties, "jdbc.driver", "AI_TEF_JDBC_DRIVER", "com.mysql.cj.jdbc.Driver"));
        config.setJdbcUrl(value(properties, "jdbc.url", "AI_TEF_JDBC_URL", ""));
        config.setUsername(value(properties, "jdbc.username", "AI_TEF_DB_USER", "root"));
        config.setPassword(value(properties, "jdbc.password", "AI_TEF_DB_PASSWORD", ""));
        config.setMaximumPoolSize(intValue(properties, "jdbc.pool.maximumPoolSize", "AI_TEF_DB_POOL_MAX", 10));
        config.setMinimumIdle(intValue(properties, "jdbc.pool.minimumIdle", "AI_TEF_DB_POOL_MIN", 2));
        config.setPoolName("ai-tef-hikari");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return new HikariDataSource(config);
    }

    private static String value(Properties properties, String key, String envName, String defaultValue) {
        String system = System.getProperty(key);
        if (system != null && !system.isBlank()) {
            return system;
        }
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return properties.getProperty(key, defaultValue);
    }

    private static int intValue(Properties properties, String key, String envName, int defaultValue) {
        String value = value(properties, key, envName, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
