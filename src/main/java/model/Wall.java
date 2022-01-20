package model;

import java.awt.*;

public class Wall extends Rectangle.Double {
    public static final double WIDTH = 10;
    public static final double HEIGHT = 60;
    public static final double SHIELD_WIDTH = 20;
    public static final double SHIELD_HEIGHT = Fortress.HEIGHT;
    public static final int MAX_HEALTH = 5;
    public static final int WOOD_COST = 2;
    public static int idCounter = 0;
    private int id;
    private boolean team;
    private int health;

    public Wall(double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        id = idCounter++;
        this.team = team;
        this.health = MAX_HEALTH;
    }

    // Shield constructor
    public Wall(double x, double y, double width, double height, boolean team){
        super(x, y, width, height);
        id = idCounter++;
        this.team = team;
        this.health = 999;
    }

    public Wall(int id, int health, double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        this.id = id;
        this.health = health;
        this.team = team;
    }

    public int getId() {
        return id;
    }

    public boolean getTeam() {
        return team;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }
}
