package model;

import java.awt.*;

import org.jspace.Space;

public class Player extends Rectangle.Double {
	public static final double WIDTH = 80;
	public static final double HEIGHT = 80;
	public static final int SPEED = 200;
	public int id;
	public String name;
	public boolean team;
	public boolean hasOrb = false;
	public int wood = 10;
	public int iron = 20;
	public double stunned = 0;
	public Space serverToClient;
	public Space clientToServer;
	
	public Player() {}
	public Player(double x, double y, int id, boolean team, String name) {
		super(x, y, WIDTH, HEIGHT);
		this.id = id;
		this.team = team;
		this.name = name;
	}

}