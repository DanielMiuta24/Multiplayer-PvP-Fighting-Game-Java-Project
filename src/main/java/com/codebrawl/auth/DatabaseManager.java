package com.codebrawl.auth;

import java.sql.*;

public class DatabaseManager {
    private final String url;

    public DatabaseManager(String dbFile) { this.url = "jdbc:sqlite:" + dbFile; }

    public void init() throws SQLException {
        try (Connection c = DriverManager.getConnection(url);
             Statement st = c.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users(
                      username TEXT PRIMARY KEY,
                      password_hash TEXT NOT NULL,
                      wins INTEGER DEFAULT 0,
                      losses INTEGER DEFAULT 0
                    )
                    """);
        }
    }

    public boolean createUser(String username, String passwordHash) throws SQLException {
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password_hash) VALUES(?,?)")) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            return ps.executeUpdate() == 1;
        }
    }

    public UserAccount getUser(String username) throws SQLException {
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement(
                     "SELECT username,password_hash,wins,losses FROM users WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserAccount(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
                }
                return null;
            }
        }
    }

    public void updateStats(String username, boolean win) throws SQLException {
        String col = win ? "wins" : "losses";
        try (Connection c = DriverManager.getConnection(url);
             PreparedStatement ps = c.prepareStatement("UPDATE users SET " + col + " = " + col + " + 1 WHERE username=?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
}
