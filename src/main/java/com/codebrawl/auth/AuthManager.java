package com.codebrawl.auth;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuthManager {

    private final DatabaseManager db;
    private String lastError = null;


    public static class CharacterRow {
        public final int id;
        public final String name;
        public final String clazz;
        public final int kills;
        public final int deaths;

        public CharacterRow(int id, String name, String clazz, int kills, int deaths) {
            this.id = id;
            this.name = name;
            this.clazz = clazz;
            this.kills = kills;
            this.deaths = deaths;
        }
    }

    public AuthManager(DatabaseManager databaseManager) {
        this.db = new DatabaseManager();
        initSchema();
    }

    public String getLastError() { return lastError; }



    public boolean register(String username, String password) throws SQLException {
        lastError = null;
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            lastError = "empty_credentials";
            return false;
        }
        try (Connection c = db.getConnection()) {

            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { lastError = "exists"; return false; }
                }
            }

            String hash = PasswordUtil.hash(password);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(username, password_hash) VALUES(?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.executeUpdate();
            }
            return true;
        }
    }

    public int login(String username, String password) throws SQLException {
        lastError = null;
        if (username == null || password == null) { lastError = "bad_args"; return -1; }
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) { lastError = "not_found"; return -1; }
                int id = rs.getInt("id");
                String hash = rs.getString("password_hash");
                if (PasswordUtil.verify(password, hash)) return id;
                lastError = "invalid_password";
                return -1;
            }
        }
    }

    public List<CharacterRow> listCharacters(int userId) throws SQLException {
        List<CharacterRow> out = new ArrayList<>();
        String sql = """
            SELECT c.id, c.name, c.clazz,
                   COALESCE(s.kills,0)  AS kills,
                   COALESCE(s.deaths,0) AS deaths
            FROM characters c
            LEFT JOIN character_stats s ON s.character_id = c.id
            WHERE c.user_id = ?
            ORDER BY c.id
        """;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CharacterRow(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("clazz"),
                            rs.getInt("kills"),
                            rs.getInt("deaths")
                    ));
                }
            }
        }
        return out;
    }


    public int createCharacter(int userId, String name, String clazz) throws SQLException {
        if (name == null || name.isBlank()) name = "Hero";
        if (clazz == null || clazz.isBlank()) clazz = "samurai";

        try (Connection c = db.getConnection()) {

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM characters WHERE user_id=?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) >= 3) {
                        throw new SQLException("char_limit_reached");
                    }
                }
            }


            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM characters WHERE user_id=? AND LOWER(name)=LOWER(?)")) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new SQLException("duplicate_character_name");
                }
            }

            int newId;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO characters(user_id, name, clazz) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, name);
                ps.setString(3, clazz.toLowerCase());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("no_key");
                    newId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO character_stats(character_id, kills, deaths) VALUES(?,0,0)")) {
                ps.setInt(1, newId);
                ps.executeUpdate();
            }
            return newId;
        }
    }

    public void addKill(int characterId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE character_stats SET kills = kills + 1 WHERE character_id=?")) {
            ps.setInt(1, characterId);
            int n = ps.executeUpdate();
            if (n == 0) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO character_stats(character_id, kills, deaths) VALUES(?,1,0)")) {
                    ins.setInt(1, characterId);
                    ins.executeUpdate();
                }
            }
        }
    }

    public void addDeath(int characterId) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE character_stats SET deaths = deaths + 1 WHERE character_id=?")) {
            ps.setInt(1, characterId);
            int n = ps.executeUpdate();
            if (n == 0) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO character_stats(character_id, kills, deaths) VALUES(?,0,1)")) {
                    ins.setInt(1, characterId);
                    ins.executeUpdate();
                }
            }
        }
    }



    private void initSchema() {
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
              id            INTEGER PRIMARY KEY AUTOINCREMENT,
              username      TEXT NOT NULL COLLATE NOCASE UNIQUE,
              password_hash TEXT NOT NULL,
              created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
            );
        """);

            st.executeUpdate("""
            CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username
            ON users(username);
        """);

            st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS characters (
              id         INTEGER PRIMARY KEY AUTOINCREMENT,
              user_id    INTEGER NOT NULL,
              name       TEXT NOT NULL COLLATE NOCASE,
              clazz      TEXT NOT NULL,
              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
              UNIQUE(user_id, name),
              FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """);

            st.executeUpdate("""
            CREATE TRIGGER IF NOT EXISTS trg_limit_chars
            BEFORE INSERT ON characters
            BEGIN
              SELECT CASE WHEN (
                (SELECT COUNT(*) FROM characters WHERE user_id = NEW.user_id) >= 3
              ) THEN RAISE(ABORT, 'char_limit_reached') END;
            END;
        """);

            st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS character_stats (
              character_id INTEGER PRIMARY KEY,
              kills  INTEGER NOT NULL DEFAULT 0,
              deaths INTEGER NOT NULL DEFAULT 0,
              FOREIGN KEY(character_id) REFERENCES characters(id) ON DELETE CASCADE
            );
        """);
        } catch (SQLException e) {
            System.out.println("[Auth] initSchema error: " + e.getMessage());
        }
    }

}
