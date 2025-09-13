package com.codebrawl.model;

public class Move {
    private final String name;
    private final MoveType type;
    private final int energyCost;
    private final double multiplier;
    private final int baseCooldown;
    private int cooldown;

    public Move(String name, MoveType type, int energyCost, double multiplier, int baseCooldown) {
        this.name = name;
        this.type = type;
        this.energyCost = Math.max(0, energyCost);
        this.multiplier = Math.max(0.0, multiplier);
        this.baseCooldown = Math.max(0, baseCooldown);
        this.cooldown = 0;
    }

    public String getName() { return name; }
    public MoveType getType() { return type; }
    public int getEnergyCost() { return energyCost; }
    public double getMultiplier() { return multiplier; }
    public int getCooldown() { return cooldown; }
    public boolean onCooldown() { return cooldown > 0; }
    public void trigger() { this.cooldown = baseCooldown; }
    public void tick() { if (cooldown > 0) cooldown--; }

    @Override public String toString() { return name + "(" + type + ")"; }
}
