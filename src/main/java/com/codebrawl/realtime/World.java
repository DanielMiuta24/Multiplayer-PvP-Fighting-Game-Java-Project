package com.codebrawl.realtime;

import com.codebrawl.auth.AuthManager;
import com.codebrawl.model.Fighter;
import com.codebrawl.model.Move;
import com.codebrawl.net.ClientSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {

    private static final int WORLD_W = 1920;

    private static final int FLOOR_Y = 820;

    private static class Player {
        String name;
        String clazz;
        Fighter fighter;
        int x, y;
        int hp = 100;
        int mp = 0;
        boolean facingRight = true;
        boolean guarding = false;
        boolean alive = true;
        boolean up, down, left, right;
        boolean wantAttack;
        long lastBasicMs = 0;
    }

    private final Map<ClientSession, Player> players = new HashMap<>();
    private final AuthManager auth;

    public World(AuthManager auth) {
        this.auth = auth;
    }

    public void spawnPlayer(ClientSession s, String name, String clazz) {
        Player p = new Player();
        p.name = name;
        p.clazz = clazz;
        p.fighter = Fighter.create(clazz);
        p.hp = p.fighter.getMaxHp();
        p.mp = 0;
        p.alive = true;
        p.x = 500 + (int)(Math.random() * 300);
        p.y = FLOOR_Y;
        players.put(s, p);
        broadcastSnapshot();
    }

    public void onInput(ClientSession s, String line) {
        Player p = players.get(s);
        if (p == null || !p.alive) return;
        String[] a = line.split("\\s+");
        if (a.length < 10) return;
        p.up    = "1".equals(a[2]);
        p.down  = "1".equals(a[3]);
        p.left  = "1".equals(a[4]);
        p.right = "1".equals(a[5]);
        p.wantAttack = "1".equals(a[6]);
        boolean jumpPressed = "1".equals(a[7]);
        p.guarding   = "1".equals(a[9]);
        if (p.left ^ p.right) p.facingRight = p.right;
    }

    public void requestRespawn(ClientSession s) {
        Player p = players.get(s);
        if (p == null) return;
        if (p.alive) { s.send("RESPAWN_OK " + s.getId()); return; }
        p.hp = p.fighter.getMaxHp();
        p.alive = true;
        p.x = 600; p.y = FLOOR_Y;
        s.send("RESPAWN_OK " + s.getId());
        broadcastSnapshot();
    }

    public void onDisconnect(ClientSession s) {
        players.remove(s);
        broadcastSnapshot();
    }

    public void tick(double dt) {
        for (Player p : players.values()) {
            if (!p.alive) continue;
            int speed = p.fighter.getMoveSpeedPxPerSec();
            int dx = (p.right ? 1 : 0) - (p.left ? 1 : 0);
            int dy = (p.down  ? 1 : 0) - (p.up   ? 1 : 0);
            if (dx != 0 && dy != 0) {
                double inv = 1.0 / Math.sqrt(2);
                p.x += (int)Math.round(dx * speed * inv * dt);
                p.y += (int)Math.round(dy * speed * inv * dt);
            } else {
                p.x += (int)Math.round(dx * speed * dt);
                p.y += (int)Math.round(dy * speed * dt);
            }
            p.x = Math.max(48, Math.min(WORLD_W - 48, p.x));
            p.y = Math.max(96, Math.min(FLOOR_Y, p.y));
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<ClientSession, Player> eA : players.entrySet()) {
            ClientSession aS = eA.getKey();
            Player a = eA.getValue();
            if (!a.alive) continue;

            Move basic = a.fighter.getBasicMove();
            if (!a.wantAttack) continue;
            if (now - a.lastBasicMs < basic.getCooldownMs()) continue;

            a.lastBasicMs = now;
            a.wantAttack = false;

            int range = basic.getRangePx();
            int baseDamage = basic.getDamage();
            int minDamage = 1;

            for (Map.Entry<ClientSession, Player> eB : players.entrySet()) {
                if (eA == eB) continue;
                ClientSession bS = eB.getKey();
                Player b = eB.getValue();
                if (!b.alive) continue;

                int dx = b.x - a.x;
                int dy = b.y - a.y;
                int dist2 = dx*dx + dy*dy;
                if (dist2 > range*range) continue;

                boolean targetRight = dx >= 0;
                if (a.facingRight != targetRight) continue;

                int dmg = baseDamage - (b.guarding ? b.fighter.getGuardReduction() : 0);
                if (dmg < minDamage) dmg = minDamage;

                int oldHp = b.hp;
                b.hp = Math.max(0, b.hp - dmg);

                int kb = 12;
                if (targetRight) b.x += kb; else b.x -= kb;

                if (oldHp > 0 && b.hp == 0) {
                    b.alive = false;
                    broadcast("KO " + aS.getId() + " " + bS.getId());
                    try {
                        if (aS.getCharacterId() != null) {
                            auth.addKill(aS.getCharacterId());
                            int newKills = auth.getKills(aS.getCharacterId());
                            aS.send("SCORES " + newKills);
                        }
                        if (bS.getCharacterId() != null) {
                            auth.addDeath(bS.getCharacterId());
                        }
                    } catch (Exception ignored) {}
                    broadcastTop(10);
                }
            }
        }

        broadcastSnapshot();
    }


    public void sendTopTo(ClientSession s, int limit) {
        s.send(buildTopLine(limit));
    }

    public void broadcastTop(int limit) {
        String line = buildTopLine(limit);
        broadcast(line);
    }

    private String buildTopLine(int limit) {
        try {
            List<AuthManager.CharacterRow> top = auth.topKillers(limit);
            StringBuilder sb = new StringBuilder("TOP ").append(top.size());
            for (var r : top) {
                sb.append(' ')
                        .append(r.name.replace(' ','_')).append(' ')
                        .append(r.kills).append(' ')
                        .append(r.deaths);
            }
            return sb.toString();
        } catch (Exception e) {
            return "TOP 0";
        }
    }

    private void broadcastSnapshot() {
        StringBuilder sb = new StringBuilder("SNAPSHOT ");
        sb.append(System.currentTimeMillis()).append(' ').append(players.size());
        for (Map.Entry<ClientSession, Player> e : players.entrySet()) {
            ClientSession s = e.getKey();
            Player p = e.getValue();
            sb.append(' ')
                    .append(s.getId())
                    .append(' ').append(p.name.replace(' ','_'))
                    .append(' ').append(p.clazz)
                    .append(' ').append(p.x)
                    .append(' ').append(p.y)
                    .append(' ').append(p.hp)
                    .append(' ').append(p.mp)
                    .append(' ').append(p.facingRight ? "1" : "0")
                    .append(' ').append(p.guarding ? "1" : "0");
        }
        broadcast(sb.toString());
    }

    private void broadcast(String msg) {
        for (ClientSession s : players.keySet()) s.send(msg);
    }
}
