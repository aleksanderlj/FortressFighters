package model;

import java.awt.*;

public class Player extends Rectangle.Double {
	public Player() {}
	public Player(double x, double y, double width, double height, int id, boolean team) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.id = id;
		this.team = team;
	}
    public int id;
    public boolean team;
}