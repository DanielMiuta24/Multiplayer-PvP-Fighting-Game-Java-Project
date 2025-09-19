package com.codebrawl.auth;

public class AuthManager {
    private final DatabaseManager db;

    public AuthManager(DatabaseManager db) { this.db = db; }

    public boolean register(String username, String passwordPlain) throws Exception {
        String hash = PasswordUtil.sha256(passwordPlain);
        return db.createUser(username, hash);
    }

    public boolean login(String username, String passwordPlain) throws Exception {
        UserAccount ua = db.getUser(username);
        return ua != null && ua.getPasswordHash().equals(PasswordUtil.sha256(passwordPlain));
    }
}
