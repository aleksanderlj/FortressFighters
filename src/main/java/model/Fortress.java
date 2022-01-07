package model;

import java.awt.*;
import game.Server;

public class Fortress extends Rectangle.Double {
	public static final double WIDTH = 300;
	public static final double HEIGHT = 360;
    private int wood;
    private int iron;
	private int hp;
	private boolean team;
	
	public Fortress(int wood, int iron, int hp, boolean team) {
		super(team ? Server.SCREEN_WIDTH-WIDTH-1 : 1, Server.SCREEN_HEIGHT/4, WIDTH, HEIGHT);
		this.wood = wood;
		this.iron = iron;
		this.hp = hp;
		this.team = team;
	}
	
	public boolean getTeam() {
		return team;
	}
	
	public int getWood() {
		return wood;
	}
	
	public int getIron() {
		return iron;
	}
	
	public int getHP() {
		return hp;
	}
}
