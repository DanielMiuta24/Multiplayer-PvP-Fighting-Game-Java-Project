package com.codebrawl.model;

public class Mage extends Fighter {
    public Mage(String name) { super(name, 90, 12, 8, 80); }

    @Override protected void initMoves() {
        moves.add(new Move("Firebolt", MoveType.ATTACK, 7, 1.2, 0));
        moves.add(new Move("Arcane Shield", MoveType.BLOCK, 6, 0.0, 2));
        moves.add(new Move("Meteor", MoveType.SPECIAL, 25, 2.0, 4));
    }

    @Override public double dodgeChance() { return 0.10; }
}
