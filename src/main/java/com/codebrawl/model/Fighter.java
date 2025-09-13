package com.codebrawl.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Fighter {
    protected final String name;
    protected int health;
    protected int strength;
    protected int defense;
    protected int energy;
    protected final List<Move> moves = new ArrayList<>();

    protected Fighter(String name, int health, int strength, int defense, int energy) {
        this.name = name;
        this.health = health;
        this.strength = strength;
        this.defense = defense;
        this.energy = energy;
        initMoves();
    }

    protected abstract void initMoves();

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getStrength() { return strength; }
    public int getDefense() { return defense; }
    public int getEnergy() { return energy; }
    public List<Move> getMoves() { return moves; }
    public boolean isAlive() { return health > 0; }

    public boolean canUse(Move m) { return !m.onCooldown() && energy >= m.getEnergyCost(); }
    public void consumeEnergy(int amount) { energy = Math.max(0, energy - amount); }
    public void regenEnergy(int amount) { energy = Math.min(100, energy + amount); }
    public void receiveDamage(int dmg) { health = Math.max(0, health - Math.max(0, dmg)); }

    public double critChance() { return 0.10; }
    public double dodgeChance() { return 0.05; }
}
