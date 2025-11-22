package org.example.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

/**
 * 账户服务类。
 * 负责处理用户认证、注册、资料获取、资料更新和账号删除等业务逻辑。
 */
public class AccountService {

    private final DatabaseManager dbManager;

    /**
     * 构造函数。
     *
     * @param dbManager 数据库管理器实例
     */
    public AccountService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * 验证用户登录凭据。
     *
     * @param account  账号
     * @param password 密码
     * @return 如果验证通过返回 true，否则返回 false
     * @throws IllegalStateException 如果数据库连接失败
     */
    public boolean authenticate(String account, String password) {
        String sql = "SELECT password_hash FROM users WHERE account = ?";
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                return storedHash.equals(hashPassword(password));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("验证账号时出错", ex);
        }
    }

    /**
     * 注册新用户。
     *
     * @param account    账号
     * @param nickname   昵称
     * @param password   密码
     * @param avatarPath 头像路径
     * @return 注册结果对象
     * @throws IllegalStateException 如果数据库连接失败
     */
    public RegistrationResult register(String account, String nickname, String password, String avatarPath) {
        if (account == null || account.isBlank()) {
            return new RegistrationResult(false, "账号不能为空");
        }
        if (nickname == null || nickname.isBlank()) {
            return new RegistrationResult(false, "昵称不能为空");
        }
        if (password == null || password.length() < 6) {
            return new RegistrationResult(false, "密码至少需要 6 位");
        }
        if (exists(account)) {
            return new RegistrationResult(false, "该账号已存在");
        }

        String trimmedAccount = account.trim();
        String trimmedNickname = nickname.trim();
        String trimmedAvatar = (avatarPath == null || avatarPath.isBlank()) ? null : avatarPath;

        String sql = "INSERT INTO users(account, password_hash, password_plain, nickname, avatar_path) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, trimmedAccount);
            stmt.setString(2, hashPassword(password));
            stmt.setString(3, password);
            stmt.setString(4, trimmedNickname);
            stmt.setString(5, trimmedAvatar);
            stmt.executeUpdate();
            return new RegistrationResult(true, "注册成功");
        } catch (SQLException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "注册失败";
            return new RegistrationResult(false, message);
        }
    }

    /**
     * 获取用户资料。
     *
     * @param account 账号
     * @return 用户资料对象，如果账号不存在则返回 null
     */
    public UserProfile getProfile(String account) {
        String sql = "SELECT account, nickname, avatar_path, password_plain FROM users WHERE account = ?";
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String nickname = rs.getString("nickname");
                if (nickname == null || nickname.isBlank()) {
                    nickname = account;
                }
                String avatarPath = rs.getString("avatar_path");
                String storedPassword = rs.getString("password_plain");
                return new UserProfile(account, nickname, avatarPath, storedPassword);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("加载用户资料失败", ex);
        }
    }

    /**
     * 更新用户资料。
     * 支持更新昵称、头像和密码。如果更新密码，需要验证旧密码。
     *
     * @param account     账号
     * @param newNickname 新昵称
     * @param newAvatar   新头像路径
     * @param oldPassword 旧密码（仅在修改密码时需要）
     * @param newPassword 新密码（仅在修改密码时需要）
     * @return 更新结果对象
     */
    public ProfileUpdateResult updateProfile(String account, String newNickname, String newAvatar, String oldPassword, String newPassword) {
        if (newNickname == null || newNickname.isBlank()) {
            return new ProfileUpdateResult(false, "昵称不能为空", null);
        }

        String trimmedNickname = newNickname.trim();
        String trimmedAvatar = newAvatar == null || newAvatar.isBlank() ? null : newAvatar;

        String selectSql = "SELECT password_hash, password_plain FROM users WHERE account = ?";
        try (Connection connection = dbManager.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, account);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (!rs.next()) {
                    return new ProfileUpdateResult(false, "账号不存在", null);
                }
                String storedHash = rs.getString("password_hash");
                String newHash = storedHash;
                String storedPlain = rs.getString("password_plain");
                String newPlain = storedPlain;

                boolean wantsPasswordChange = newPassword != null && !newPassword.isBlank();
                if (wantsPasswordChange) {
                    if (oldPassword == null || oldPassword.isBlank()) {
                        return new ProfileUpdateResult(false, "请输入原密码", null);
                    }
                    if (!storedHash.equals(hashPassword(oldPassword))) {
                        return new ProfileUpdateResult(false, "原密码不正确", null);
                    }
                    if (newPassword.length() < 6) {
                        return new ProfileUpdateResult(false, "新密码至少需要 6 位", null);
                    }
                    newHash = hashPassword(newPassword);
                    newPlain = newPassword;
                } else if (oldPassword != null && !oldPassword.isBlank()) {
                    return new ProfileUpdateResult(false, "请填写新密码", null);
                }

                String updateSql = "UPDATE users SET nickname = ?, avatar_path = ?, password_hash = ?, password_plain = ? WHERE account = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setString(1, trimmedNickname);
                    updateStmt.setString(2, trimmedAvatar);
                    updateStmt.setString(3, newHash);
                    updateStmt.setString(4, newPlain);
                    updateStmt.setString(5, account);
                    updateStmt.executeUpdate();
                }

                UserProfile profile = new UserProfile(account, trimmedNickname, trimmedAvatar, newPlain);
                return new ProfileUpdateResult(true, "资料更新成功", profile);
            }
        } catch (SQLException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "更新资料失败";
            return new ProfileUpdateResult(false, message, null);
        }
    }

    /**
     * 删除用户账号。
     *
     * @param account  账号
     * @param password 密码（用于验证身份）
     * @return 删除结果对象
     */
    public AccountDeletionResult deleteAccount(String account, String password) {
        if (account == null || account.isBlank()) {
            return new AccountDeletionResult(false, "账号不能为空");
        }
        if (password == null || password.isBlank()) {
            return new AccountDeletionResult(false, "请输入密码以确认操作");
        }

        String trimmedAccount = account.trim();
        try (Connection connection = dbManager.getConnection()) {
            String selectSql = "SELECT password_hash FROM users WHERE account = ?";
            String storedHash;
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setString(1, trimmedAccount);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        return new AccountDeletionResult(false, "账号不存在或已被删除");
                    }
                    storedHash = rs.getString("password_hash");
                }
            }
            if (!storedHash.equals(hashPassword(password))) {
                return new AccountDeletionResult(false, "密码不正确");
            }
            String deleteSql = "DELETE FROM users WHERE account = ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, trimmedAccount);
                deleteStmt.executeUpdate();
            }
            return new AccountDeletionResult(true, "账号已成功注销");
        } catch (SQLException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "注销账号失败";
            return new AccountDeletionResult(false, message);
        }
    }

    /**
     * 检查账号是否存在。
     *
     * @param account 账号
     * @return 如果存在返回 true，否则返回 false
     */
    public boolean exists(String account) {
        String sql = "SELECT 1 FROM users WHERE account = ?";
        try (Connection connection = dbManager.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("检查账号是否存在时出错", ex);
        }
    }

    /**
     * 对密码进行 SHA-256 哈希处理。
     *
     * @param password 原始密码
     * @return 哈希后的密码字符串
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("无法初始化密码加密算法", ex);
        }
    }

    /**
     * 注册结果记录类。
     */
    public record RegistrationResult(boolean success, String message) {}
    /**
     * 用户资料记录类。
     */
    public record UserProfile(String account, String nickname, String avatarPath, String password) {}
    /**
     * 资料更新结果记录类。
     */
    public record ProfileUpdateResult(boolean success, String message, UserProfile profile) {}
    /**
     * 账号删除结果记录类。
     */
    public record AccountDeletionResult(boolean success, String message) {}
}
