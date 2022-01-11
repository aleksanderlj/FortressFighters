package model;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Resource extends Rectangle.Double {
    int type; // 0 = wood, 1 = iron
    public static final double WIDTH = 50;
    public static final double HEIGHT = 50;

    public Resource(double x, double y, int type) {
        super(x, y, WIDTH, HEIGHT);
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}