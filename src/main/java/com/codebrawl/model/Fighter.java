package com.codebrawl.model;

public abstract class Fighter {
    private final String clazz;
    private final int maxHp;
    private final int maxMp;
    private final int moveSpeedPxPerSec;
    private final int guardReduction;

    private final Move basic;
    private final Move special;

    protected Fighter(String clazz, int maxHp, int maxMp, int speed, int guardReduction,
                      Move basic, Move special) {
        this.clazz = clazz;
        this.maxHp = maxHp;
        this.maxMp = maxMp;
        this.moveSpeedPxPerSec = speed;
        this.guardReduction = guardReduction;
        this.basic = basic;
        this.special = special;
    }

    public String getClazz()            { return clazz; }
    public int getMaxHp()               { return maxHp; }
    public int getMaxMp()               { return maxMp; }
    public int getMoveSpeedPxPerSec()   { return moveSpeedPxPerSec; }
    public int getGuardReduction()      { return guardReduction; }
    public Move getBasicMove()          { return basic; }
    public Move getSpecialMove()        { return special; }


    public static Fighter create(String clazz) {
        if (clazz == null) clazz = "samurai";
        switch (clazz.toLowerCase()) {
            case "shinobi": return new Shinobi();
            case "warrior": return new Warrior();
            default:        return new Samurai();
        }
    }
}
