package model;

import java.awt.Rectangle;

import game.Server;

public class OrbHolder extends Rectangle.Double {
	public static final double WIDTH = 50;
	public static final double HEIGHT = 75;
	public boolean hasOrb = false;
	public boolean team;
	public boolean top;
	
	public OrbHolder(boolean team, boolean top, boolean hasOrb) {
		super(team ? 50 : Server.SCREEN_WIDTH-50-WIDTH, top ? 50 : Server.SCREEN_HEIGHT-100-HEIGHT, WIDTH, HEIGHT);
		this.team = team;
		this.top = top;
		this.hasOrb = hasOrb;
	}
}
