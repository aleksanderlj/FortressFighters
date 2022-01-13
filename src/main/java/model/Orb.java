package model;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Orb extends Rectangle.Double {
    public static final double WIDTH = 35;
    public static final double HEIGHT = 35;

    public Orb(double x, double y) {
        super(x, y, WIDTH, HEIGHT);
    }

}