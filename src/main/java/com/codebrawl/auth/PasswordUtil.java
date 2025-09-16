package com.codebrawl.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordUtil {
    public static String hash(String raw) { return sha256(raw); }
    public static boolean verify(String raw, String stored) { return sha256(raw).equals(stored); }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
