package com.codebrawl.model;

public class Warrior extends Fighter {
    public Warrior() {
        super(
                "warrior",
                120, 40,
                160,
                16,
                new Move(MoveType.BASIC, 22, 85, 280),
                new Move(MoveType.SPECIAL, 40, 100, 1400)
        );
    }
}
