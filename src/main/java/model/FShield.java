package model;

import java.awt.*;

public class FShield extends Rectangle.Double{
    public static final double WIDTH = 20;
    public static final double HEIGHT = 600;
    public static final int MAX_HEALTH = 13;
    public static final int WOOD_COST = 5;
    public static final int IRON_COST = 2;
    public static int idCounter = 0;
    private int id;
    private boolean team;
    private int health;

    public FShield(double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        id = idCounter++;
        this.team = team;
        this.health = MAX_HEALTH;
    }

    public FShield(int id, int health, double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        this.id = id;
        this.health = health;
        this.team = team;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isTeam() {
        return team;
    }

    public boolean getTeam(){
        return team;
    }

    public void setTeam(boolean team) {
        this.team = team;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }
}
