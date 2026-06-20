package com.warehouse.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quản lý kết nối tới MySQL thông qua connection pool (HikariCP).
 * Tất cả DAO lấy Connection từ đây thay vì tự mở/đóng kết nối thủ công,
 * tránh tốn chi phí tạo connection mới cho mỗi request và tránh leak connection
 * khi nhiều thread (mỗi client 1 thread) truy cập DB đồng thời.
 */
public final class DatabaseManager {

    private static volatile HikariDataSource dataSource;

    private DatabaseManager() {
    }

    public static synchronized void init(String jdbcUrl, String username, String password) {
        if (dataSource != null) {
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Cấu hình pool - phù hợp cho app nhiều client kết nối đồng thời qua socket
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(10_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("warehouse-pool");

        // Một số option giúp tăng tốc cho MySQL Connector/J
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseManager chưa được init(). Gọi DatabaseManager.init(...) khi khởi động server.");
        }
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
