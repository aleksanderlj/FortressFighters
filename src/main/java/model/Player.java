package model;

import java.awt.*;

import org.jspace.Space;

public class Player extends Rectangle.Double {
	public static final double WIDTH = 62;
	public static final double HEIGHT = 83;
	public static final int SPEED = 130;
	public int id;
	public String name;
	public boolean team;
	public boolean hasOrb = false;
	public int wood = 0;
	public int iron = 0;
	public double stunned = 0;
	public Space serverToClient;
	public Space clientToServer;
	public String preferredTeam; // TODO: Isn't current sent at host switch
	
	public Player() {}
	public Player(double x, double y, int id, boolean team, String name) {
		super(x, y, WIDTH, HEIGHT);
		this.id = id;
		this.team = team;
		this.name = name;
	}

	public Player(double x, double y, int id, boolean team, String name, String preferredTeam) {
		super(x, y, WIDTH, HEIGHT);
		this.id = id;
		this.team = team;
		this.name = name;
		this.preferredTeam = preferredTeam;
	}
}