package com.codebrawl.net;

import com.codebrawl.auth.AuthManager;
import com.codebrawl.realtime.World;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

public class ClientSession implements Runnable {

    private final Socket socket;
    private final World world;
    private final RtServer server;
    private final AuthManager auth;

    private final BufferedReader in;
    private final PrintWriter out;

    private final String id;
    private volatile boolean running = true;

    private int userId = -1;
    private Integer characterId = null;
    private String characterClass = "samurai";
    private String displayName = "Hero";
    private boolean joined = false;

    public ClientSession(Socket socket, World world, RtServer server, AuthManager auth) throws IOException {
        this.socket = socket;
        this.world = world;
        this.server = server;
        this.auth = auth;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.id = UUID.randomUUID().toString().substring(0, 8);
    }

    public String getId() { return id; }
    public Integer getCharacterId() { return characterId; }
    public String getCharacterClass() { return characterClass; }
    public String getDisplayName() { return displayName; }

    private void log(String msg){ System.out.println("[Session " + id + "] " + msg); }
    private void sendLine(String s){ log(">> " + s); synchronized (out) { out.println(s); } }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                try {
                    handle(line.trim());
                } catch (Throwable t) {
                    System.err.println("[Session " + id + "] handle error: " + t);
                    t.printStackTrace(System.err);
                }
            }
        } catch (IOException ignored) {
        } finally {
            close();
        }
    }

    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
        world.onDisconnect(this);
    }

    private void handle(String line) {
        if (line.isEmpty()) return;

        if (line.startsWith("REGISTER ")) {
            String[] p = line.split("\\s+");
            if (p.length < 3) { sendLine("REGISTER_FAIL bad_args"); return; }
            String u = p[1], pw = p[2];
            try {
                boolean ok = auth.register(u, pw);
                // keep REGISTER_* as-is for client
                sendLine(ok ? "REGISTER_OK" : "REGISTER_FAIL exists");
            } catch (Exception e) {
                log("REGISTER error: " + e);
                String msg = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
                if (msg.contains("unique")) sendLine("REGISTER_FAIL exists");
                else                       sendLine("REGISTER_FAIL error");
            }
            return;
        }

        if (line.startsWith("LOGIN ")) {
            String[] p = line.split("\\s+");
            if (p.length < 3) { sendLine("AUTH_FAIL bad_args"); return; }
            String u = p[1], pw = p[2];
            try {
                int uid = auth.login(u, pw);
                log("login(" + u + ") -> uid=" + uid);
                if (uid > 0) {
                    this.userId = uid;
                    sendLine("AUTH_OK");
                } else {
                    sendLine("AUTH_FAIL invalid");
                }
            } catch (Exception e) {
                log("LOGIN error: " + e);
                sendLine("AUTH_FAIL error");
            }
            return;
        }

        if (userId <= 0) { sendLine("AUTH_FAIL login_required"); return; }

        // --- LIST_CHARS -> CHARS n id name cls ...
        if (line.equals("LIST_CHARS")) {
            try {
                var list = auth.listCharacters(userId);
                StringBuilder sb = new StringBuilder("CHARS ").append(list.size());
                for (var c : list) {
                    sb.append(' ')
                            .append(c.id)
                            .append(' ').append(c.name.replace(' ','_'))
                            .append(' ').append(c.clazz);
                }
                sendLine(sb.toString());
            } catch (SQLException e) {
                sendLine("CHARS 0");
            }
            return;
        }

        // --- CREATE_CHAR -> CHAR_CREATED | CHAR_FAIL <reason>
        if (line.startsWith("CREATE_CHAR ")) {
            String[] p = line.split("\\s+");
            if (p.length < 3) { sendLine("CHAR_FAIL bad_args"); return; }
            String name = p[1].replace('_',' ');
            String clazz = p[2].toLowerCase(Locale.ROOT);
            try {
                int cid = auth.createCharacter(userId, name, clazz);
                sendLine("CHAR_CREATED " + cid + " " + name.replace(' ','_') + " " + clazz);
            } catch (SQLException e) {
                String msg = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
                if (msg.contains("char_limit_reached")) {
                    sendLine("CHAR_FAIL limit");
                } else if (msg.contains("unique")) {
                    sendLine("CHAR_FAIL duplicate_name");
                } else {
                    sendLine("CHAR_FAIL error");
                }
            }
            return;
        }

        // --- SELECT_CHAR (kept for compatibility) ---
        if (line.startsWith("SELECT_CHAR ")) {
            String[] p = line.split("\\s+");
            if (p.length < 2) { sendLine("SELECT_CHAR_FAIL bad_args"); return; }
            int cid;
            try { cid = Integer.parseInt(p[1]); }
            catch (NumberFormatException e) { sendLine("SELECT_CHAR_FAIL bad_args"); return; }
            try {
                var list = auth.listCharacters(userId);
                boolean ok = false;
                for (var c : list) {
                    if (c.id == cid) {
                        ok = true;
                        this.characterId = cid;
                        this.characterClass = c.clazz;
                        this.displayName = c.name;
                        break;
                    }
                }
                if (!ok) { sendLine("SELECT_CHAR_FAIL not_owner"); return; }
                sendLine("SELECT_CHAR_OK " + cid);
            } catch (SQLException e) {
                sendLine("SELECT_CHAR_FAIL error");
            }
            return;
        }

        // --- JOIN_CHAR <id> : what the client sends (combines select + join)
        if (line.startsWith("JOIN_CHAR ")) {
            String[] p = line.split("\\s+");
            if (p.length < 2) { sendLine("AUTH_FAIL bad_args"); return; }
            int cid;
            try { cid = Integer.parseInt(p[1]); }
            catch (NumberFormatException e) { sendLine("AUTH_FAIL bad_args"); return; }
            try {
                var list = auth.listCharacters(userId);
                boolean ok = false;
                for (var c : list) {
                    if (c.id == cid) {
                        ok = true;
                        this.characterId = cid;
                        this.characterClass = c.clazz;
                        this.displayName = c.name;
                        break;
                    }
                }
                if (!ok) { sendLine("AUTH_FAIL not_owner"); return; }
                world.spawnPlayer(this, displayName, characterClass);
                joined = true;
                sendLine("WELCOME " + id);
            } catch (SQLException e) {
                sendLine("AUTH_FAIL error");
            }
            return;
        }

        // --- legacy JOIN (name class) still supported
        if (line.startsWith("JOIN")) {
            String[] p = line.split("\\s+");
            if (characterId == null && p.length >= 3) {
                this.displayName = p[1].replace('_',' ');
                this.characterClass = p[2].toLowerCase(Locale.ROOT);
            }
            world.spawnPlayer(this, displayName, characterClass);
            joined = true;
            sendLine("WELCOME " + id);
            return;
        }

        if (line.startsWith("INPUT ")) {
            if (!joined) return;
            world.onInput(this, line);
            return;
        }

        if (line.equals("RESPAWN")) {
            if (joined) world.requestRespawn(this);
        }
    }

    // --- FIX: actually deliver broadcast lines to the client
    public void send(String s) {
        sendLine(s);
    }
}
