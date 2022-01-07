package model;

import java.awt.*;

public class Bullet extends Rectangle.Double {
    public static final double WIDTH = 10;
    public static final double HEIGHT = 5;
    private boolean team; // Also decides direction

    public Bullet(double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        this.team = team;
    }

    public boolean getTeam() {
        return team;
    }
}
