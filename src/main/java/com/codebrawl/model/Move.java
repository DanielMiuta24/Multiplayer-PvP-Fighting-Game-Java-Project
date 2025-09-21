package com.codebrawl.model;

public class Move {
    private final MoveType type;
    private final int damage;
    private final int rangePx;
    private final long cooldownMs;

    public Move(MoveType type, int damage, int rangePx, long cooldownMs) {
        this.type = type;
        this.damage = damage;
        this.rangePx = rangePx;
        this.cooldownMs = cooldownMs;
    }


    public int getDamage()        { return damage; }
    public int getRangePx()       { return rangePx; }
    public long getCooldownMs()   { return cooldownMs; }

    @Override public String toString() {
        return "Move{" + type + ", dmg=" + damage + ", range=" + rangePx + ", cd="+cooldownMs+"}";
    }
}
