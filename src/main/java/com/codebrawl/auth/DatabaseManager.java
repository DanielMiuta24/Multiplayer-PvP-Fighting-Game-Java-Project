package com.codebrawl.auth;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found on classpath. Add org.xerial:sqlite-jdbc.", e);
        }
    }

    private final String jdbcUrl;

    public DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        ensureDir(jdbcUrl);
    }

    private void ensureDir(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) return;
        String path = jdbcUrl.substring("jdbc:sqlite:".length());
        File file = new File(path).getAbsoluteFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public String getJdbcUrl() { return jdbcUrl; }
}
