package com.codebrawl.auth;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignore) {
            System.err.println("[DB] org.sqlite.JDBC not found yet. If you run with Maven exec or a shaded jar, it will be available at runtime.");
        }
        String root = System.getProperty("user.dir");
        File dataDir = new File(root, "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        DB_FILE = new File(dataDir, "codebrawl.db").getAbsolutePath();
        System.out.println("[DB] Using " + DB_FILE);
    }


    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + DB_FILE;
        return DriverManager.getConnection(url);
    }
}
