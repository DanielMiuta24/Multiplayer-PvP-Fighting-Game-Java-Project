package com.codebrawl.model;

public class Assassin extends Fighter {
    public Assassin(String name) { super(name, 95, 13, 8, 70); }

    @Override protected void initMoves() {
        moves.add(new Move("Stab", MoveType.ATTACK, 5, 1.1, 0));
        moves.add(new Move("Smoke", MoveType.BLOCK, 4, 0.0, 2));
        moves.add(new Move("Backstab", MoveType.SPECIAL, 18, 2.2, 3));
    }

    @Override public double critChance() { return 0.25; }
    @Override public double dodgeChance() { return 0.12; }
}
