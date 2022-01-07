package model;

import java.awt.*;

public class Wall extends Rectangle.Double {
    public static final double WIDTH = 10;
    public static final double HEIGHT = 60;
    public static final int MAX_HEALTH = 5;
    public static final int WOOD_COST = 1;
    private boolean team;
    private int health;

    public Wall(double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        this.team = team;
        this.health = MAX_HEALTH;
    }

    public boolean getTeam() {
        return team;
    }
}
