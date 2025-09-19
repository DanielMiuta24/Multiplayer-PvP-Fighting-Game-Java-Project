package com.codebrawl.auth;

public class UserAccount {
    private final String username;
    private final String passwordHash;
    private int wins;
    private int losses;

    public UserAccount(String username, String passwordHash, int wins, int losses) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.wins = wins;
        this.losses = losses;
    }

    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }

    public void addWin() { wins++; }
    public void addLoss() { losses++; }
}
