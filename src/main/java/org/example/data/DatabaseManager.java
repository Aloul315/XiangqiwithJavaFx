package org.example.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库管理类。
 * 负责管理 SQLite 数据库连接、初始化数据库架构以及处理架构迁移。
 */
public class DatabaseManager {
    private static final String DB_DIRECTORY = System.getProperty("user.home") + "/.xiangqi";
    private static final String DB_NAME = "accounts.db";

    private final Path databasePath;
    private final String jdbcUrl;

    public DatabaseManager() {
        this.databasePath = Paths.get(DB_DIRECTORY, DB_NAME);
        createDirectoryIfNeeded(databasePath.getParent());
        this.jdbcUrl = "jdbc:sqlite:" + databasePath.toString();
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    private void createDirectoryIfNeeded(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建数据库目录: " + directory, ex);
        }
    }

    private void initializeSchema() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "account TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "password_plain TEXT, " +
                    "nickname TEXT, " +
                    "avatar_path TEXT, " +
                    "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            ensureProfileColumns(connection);
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化数据库失败", ex);
        }
    }

    /**
     * 确保 accounts 表包含用户资料相关的列（nickname, avatar_path）。
     * 如果列不存在，则添加这些列。
     *
     * @param conn 数据库连接
     */
    private void ensureProfileColumns(Connection conn) {
        try (Statement statement = conn.createStatement()) {
            try {
                statement.executeUpdate("ALTER TABLE users ADD COLUMN nickname TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                statement.executeUpdate("ALTER TABLE users ADD COLUMN avatar_path TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            try {
                statement.executeUpdate("ALTER TABLE users ADD COLUMN password_plain TEXT");
            } catch (SQLException ignore) {
                // column already exists
            }
            statement.executeUpdate("UPDATE users SET password_plain = '' WHERE password_plain IS NULL");
            statement.executeUpdate("UPDATE users SET nickname = account WHERE nickname IS NULL OR nickname = ''");
        } catch (SQLException ex) {
            throw new IllegalStateException("升级用户资料字段失败", ex);
        }
    }
}
