package model;

import java.awt.*;

public class Cannon extends Rectangle.Double {
    public static final double WIDTH = 50;
    public static final double HEIGHT = 50;
    private boolean team;

    public Cannon(double x, double y, boolean team){
        super(x, y, WIDTH, HEIGHT);
        this.team = team;
    }

    public boolean getTeam() {
        return team;
    }
}
