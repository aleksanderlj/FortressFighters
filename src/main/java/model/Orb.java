package model;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Orb extends Rectangle.Double {
    public static final double WIDTH = 50;
    public static final double HEIGHT = 50;

    public Orb(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
    }

}