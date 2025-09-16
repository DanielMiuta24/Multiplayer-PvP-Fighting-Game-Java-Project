package com.codebrawl.realtime;

import com.codebrawl.model.Fighter;
import com.codebrawl.model.Move;
import com.codebrawl.net.ClientSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class World {

    public static class Player {
        public final String id;
        public String name;
        public Fighter fighter;

        public int x, y;
        public int hp, mp;

        public boolean facingRight = true;
        public boolean guarding    = false;


        public volatile boolean up, down, left, right, atk, sk1, sk2, guard;


        public boolean prevAtk = false;
        public boolean wantAttack = false;
        public long lastBasicMs = 0;

        public Player(String id, String name, Fighter f) {
            this.id = id;
            this.name = name;
            this.fighter = f;
            this.hp = f.getMaxHp();
            this.mp = f.getMaxMp();
            this.x = 480;
            this.y = 900;
        }
    }

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final Map<String, ClientSession> sessionByPlayer = new ConcurrentHashMap<>();
    private final List<ClientSession> sessions = Collections.synchronizedList(new ArrayList<>());

    public void addSession(ClientSession s){ sessions.add(s); }
    public void bindSession(String playerId, ClientSession s){ sessionByPlayer.put(playerId, s); }

    public String addPlayer(String name, String clazz) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Fighter f = Fighter.create(clazz);
        Player p = new Player(id, name, f);


        int n = players.size();
        p.x = 480 + (n%2==0 ? -200 : 200);
        p.y = 900;

        players.put(id, p);
        return id;
    }

    public void removePlayer(String id) {
        players.remove(id);
        sessionByPlayer.remove(id);
    }

    public void setInput(String id, boolean up, boolean down, boolean left, boolean right,
                         boolean atk, boolean sk1, boolean sk2, boolean guard) {
        Player p = players.get(id);
        if (p == null) return;

        p.up = up; p.down = down; p.left = left; p.right = right;
        p.atk = atk; p.sk1 = sk1; p.sk2 = sk2;

        if (right) p.facingRight = true;
        else if (left) p.facingRight = false;

        p.guarding = guard;

        if (atk && !p.prevAtk) p.wantAttack = true;
        p.prevAtk = atk;
    }


    public void tick(double dt) {

        for (Player p : players.values()) {
            int speed = p.fighter.getMoveSpeedPxPerSec();
            int dx = (p.right ? 1 : 0) - (p.left ? 1 : 0);
            int dy = (p.down  ? 1 : 0) - (p.up   ? 1 : 0);

            if (dx != 0 && dy != 0) {
                double inv = 1.0 / Math.sqrt(2);
                p.x += (int) Math.round(dx * speed * inv * dt);
                p.y += (int) Math.round(dy * speed * inv * dt);
            } else {
                p.x += (int) Math.round(dx * speed * dt);
                p.y += (int) Math.round(dy * speed * dt);
            }


            p.x = Math.max(48, Math.min(1920 - 48, p.x));
            p.y = Math.max(96, Math.min(1080 - 96, p.y));
        }


        long now = System.currentTimeMillis();
        for (Player a : players.values()) {
            Move basic = a.fighter.getBasicMove();

            if (!a.wantAttack) continue;
            if (now - a.lastBasicMs < basic.getCooldownMs()) continue;
            a.lastBasicMs = now;
            a.wantAttack = false;

            int range = basic.getRangePx();
            int baseDamage = basic.getDamage();
            int minDamage = 1;

            for (Player b : players.values()) {
                if (b == a) continue;

                int dx = b.x - a.x;
                int dy = b.y - a.y;
                int dist2 = dx*dx + dy*dy;
                if (dist2 > range*range) continue;

                boolean targetRight = dx >= 0;
                if (a.facingRight != targetRight) continue;

                int dmg = baseDamage - (b.guarding ? b.fighter.getGuardReduction() : 0);
                if (dmg < minDamage) dmg = minDamage;


                b.hp = Math.max(0, b.hp - dmg);


                int kb = 12;
                if (targetRight) b.x += kb; else b.x -= kb;
            }
        }

        broadcastSnapshot();
    }

    private void broadcastSnapshot() {

        StringBuilder sb = new StringBuilder();
        sb.append("SNAPSHOT ").append(System.currentTimeMillis()).append(' ')
                .append(players.size());

        for (Player p : players.values()) {
            sb.append(' ').append(p.id)
                    .append(' ').append(p.name.replace(' ','_'))
                    .append(' ').append(p.fighter.getClazz())
                    .append(' ').append(p.x)
                    .append(' ').append(p.y)
                    .append(' ').append(p.hp)
                    .append(' ').append(p.mp)
                    .append(' ').append(p.facingRight ? "1" : "0")
                    .append(' ').append(p.guarding ? "1" : "0");
        }

        String msg = sb.toString();
        synchronized (sessions) {
            for (var s : sessions) s.send(msg);
        }
    }
}
