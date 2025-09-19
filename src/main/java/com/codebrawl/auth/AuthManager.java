package com.codebrawl.auth;

import java.nio.file.Paths;
import java.sql.SQLException;

public class AuthManager {
    private final DatabaseManager db;
    private volatile String lastError = null;

    public AuthManager() {
        String abs = Paths.get("data", "codebrawl.db").toAbsolutePath().toString();
        this.db = new DatabaseManager("jdbc:sqlite:" + abs);
        initSchema();
        System.out.println("[Auth] DB=" + abs);
    }

    public String getLastError() { return lastError; }

    private void initSchema() {
        try (var conn = db.getConnection(); var st = conn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id            INTEGER PRIMARY KEY AUTOINCREMENT,
                  username      TEXT    NOT NULL UNIQUE,
                  password_hash TEXT    NOT NULL,
                  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """);
            st.executeUpdate("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);
            """);
        } catch (SQLException e) {
            System.out.println("[Auth] initSchema error: " + e.getMessage());
        }
    }

    public boolean register(String username, String password) {
        lastError = null;
        if (!isValid(username)) { lastError = "invalid_username"; return false; }

        final String sql = "INSERT OR IGNORE INTO users(username, password_hash) VALUES(?, ?)";
        try (var conn = db.getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            int rows = ps.executeUpdate();
            if (rows == 1) return true;
            lastError = "user_exists";
            return false;
        } catch (SQLException e) {
            System.out.println("[Auth] register error: " + e.getMessage());
            lastError = "db_error";
            return false;
        }
    }

    public boolean login(String username, String password) {
        lastError = null;
        final String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (var conn = db.getConnection(); var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) { lastError = "bad_credentials"; return false; }
                String hash = rs.getString(1);
                boolean ok = PasswordUtil.verify(password, hash);
                if (!ok) lastError = "bad_credentials";
                return ok;
            }
        } catch (SQLException e) {
            System.out.println("[Auth] login error: " + e.getMessage());
            lastError = "db_error";
            return false;
        }
    }

    private boolean isValid(String u) {
        return u != null && u.length() >= 3 && u.length() <= 24 && u.matches("[A-Za-z0-9_]+");
    }
}
