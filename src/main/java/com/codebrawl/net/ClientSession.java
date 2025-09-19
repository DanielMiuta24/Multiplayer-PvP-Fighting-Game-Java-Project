package com.codebrawl.net;

import com.codebrawl.realtime.World;

import java.io.*;
import java.net.Socket;
import java.util.Set;

public class ClientSession implements Runnable {

    private final Socket socket;
    private final World world;
    private final RtServer serverRef;
    private final com.codebrawl.auth.AuthManager authRef;

    private BufferedReader in;
    private PrintWriter out;

    private String playerId;
    private String username;
    private boolean authenticated = false;
    private volatile boolean running = true;

    public ClientSession(Socket socket, World world, RtServer serverRef, com.codebrawl.auth.AuthManager authRef) {
        this.socket = socket;
        this.world = world;
        this.serverRef = serverRef;
        this.authRef = authRef;
    }

    @Override public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String line;
            while (running && (line = in.readLine()) != null) handle(line.trim());
        } catch (IOException ignored) {
        } finally {
            if (playerId != null) world.removePlayer(playerId);
            if (username != null && serverRef != null) serverRef.releaseUser(username);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handle(String line) {
        try {

            if (line.startsWith("REGISTER ")) {
                if (authRef == null) { send("REGISTER_FAIL auth_unavailable"); return; }
                String[] p = line.split("\\s+");
                if (p.length < 3) { send("REGISTER_FAIL bad_request"); return; }
                String u = p[1].trim(), pw = p[2];
                boolean ok = authRef.register(u, pw);
                if (ok) { send("REGISTER_OK"); }
                else {
                    String reason = authRef.getLastError();
                    if (reason == null || reason.isBlank()) reason = "db_error";
                    send("REGISTER_FAIL " + reason);
                }
                return;
            }


            if (line.startsWith("LOGIN ")) {
                if (authRef == null || serverRef == null) { send("AUTH_FAIL auth_unavailable"); return; }
                String[] p = line.split("\\s+");
                if (p.length < 3) { send("AUTH_FAIL bad_request"); return; }
                String u = p[1].trim(), pw = p[2];
                boolean ok = authRef.login(u, pw);
                if (!ok) {
                    String reason = authRef.getLastError();
                    if (reason == null || reason.isBlank()) reason = "bad_credentials";
                    send("AUTH_FAIL " + reason);
                    return;
                }
                boolean reserved = serverRef.reserveUser(u, this);
                if (!reserved) { send("AUTH_FAIL already_logged_in"); return; }
                username = u; authenticated = true; send("AUTH_OK");
                return;
            }


            if (line.startsWith("JOIN ")) {
                if (!authenticated) { send("AUTH_REQUIRED"); return; }
                String[] p = line.split("\\s+");
                String name  = (p.length >= 2) ? p[1].replace('_',' ') : "Hero";
                String clazz = (p.length >= 3) ? p[2].toLowerCase()    : "samurai";
                if (!Set.of("samurai","shinobi","warrior").contains(clazz)) clazz = "samurai";
                playerId = world.addPlayer(name, clazz);
                world.bindSession(playerId, this);
                send("WELCOME " + playerId);
                return;
            }

            if (line.startsWith("INPUT ")) {
                if (!authenticated || playerId == null) return;
                String[] p = line.split("\\s+");
                if (p.length < 10) return;
                boolean up    = "1".equals(p[2]);
                boolean down  = "1".equals(p[3]);
                boolean left  = "1".equals(p[4]);
                boolean right = "1".equals(p[5]);
                boolean atk   = "1".equals(p[6]);
                boolean sk1   = "1".equals(p[7]);
                boolean sk2   = "1".equals(p[8]);
                boolean guard = "1".equals(p[9]);
                world.setInput(playerId, up, down, left, right, atk, sk1, sk2, guard);
            }
        } catch (Exception e) {
            System.out.println("[ClientSession] error: " + e.getMessage());
        }
    }

    public void send(String s){ if (out != null) out.println(s); }

    public void stop() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
