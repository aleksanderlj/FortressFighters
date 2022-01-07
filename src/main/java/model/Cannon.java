package model;

import java.awt.*;

public class Cannon extends Rectangle.Double {
    public static final double WIDTH = 60;
    public static final double HEIGHT = 45;
    public static final int IRON_COST = 2;
    private boolean team;
    private boolean active;

    public Cannon(double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        this.team = team;
        this.active = true;
    }

    public boolean getTeam() {
        return team;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
