package com.codebrawl.model;

public class Samurai extends Fighter {
    public Samurai() {
        super(
                "samurai",
                100, 50,
                180,              // speed px/s
                13,               // guard reduction
                new Move(MoveType.BASIC, 20, 90, 250),    // damage, range, cooldown
                new Move(MoveType.SPECIAL, 35, 110, 1200) // not used yet
        );
    }
}
