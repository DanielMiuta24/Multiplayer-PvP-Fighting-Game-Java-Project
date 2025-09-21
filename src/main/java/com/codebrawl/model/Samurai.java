package com.codebrawl.model;

public class Samurai extends Fighter {
    public Samurai() {
        super(
                "samurai",
                100, 50,
                180,
                13,
                new Move(MoveType.BASIC, 20, 90, 250),
                new Move(MoveType.SPECIAL, 35, 110, 1200)
        );
    }
}
