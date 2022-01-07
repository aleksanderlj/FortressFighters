package model;

import java.awt.*;

public class Player extends Rectangle.Double {
	public static final double WIDTH = 80;
	public static final double HEIGHT = 80;
	public static final int SPEED = 200;
	public int id;
	public boolean team;
	public boolean disconnected = false;
	public int wood = 1;
	public int iron = 2;

	public Player() {}
	public Player(double x, double y, int id, boolean team) {
		super(x, y, WIDTH, HEIGHT);
		this.id = id;
		this.team = team;
	}
}