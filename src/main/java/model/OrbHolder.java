package model;

import java.awt.Rectangle;

import game.Server;

public class OrbHolder extends Rectangle.Double {
	public static final double WIDTH = 100;
	public static final double HEIGHT = 100;
	public boolean hasOrb = false;
	public boolean team;
	public boolean top;
	
	public OrbHolder(boolean team, boolean top, boolean hasOrb) {
		super(team ? Server.SCREEN_WIDTH-(Fortress.WIDTH/2)-(WIDTH/2) : (Fortress.WIDTH/2)-(WIDTH/2)+2, top ? (Server.SCREEN_HEIGHT/2)-(Fortress.HEIGHT/2) : (Server.SCREEN_HEIGHT/2)+(Fortress.HEIGHT/2)-HEIGHT-15, WIDTH, HEIGHT);
		this.team = team;
		this.top = top;
		this.hasOrb = hasOrb;
	}
}
