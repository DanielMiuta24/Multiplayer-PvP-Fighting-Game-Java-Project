package com.codebrawl.model;

public class Shinobi extends Fighter {
    public Shinobi() {
        super(
                "shinobi",
                90, 60,
                210,
                10,
                new Move(MoveType.BASIC, 16, 95, 220),
                new Move(MoveType.SPECIAL, 30, 120, 1000)
        );
    }
}
