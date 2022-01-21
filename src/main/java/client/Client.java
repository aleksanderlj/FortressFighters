package client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import game.GameFrame;
import game.Server;
import game.Settings;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import model.*;

public class Client {

	public static final double S_BETWEEN_UPDATES = 0.01;
	private GameFrame frame;
	private String address;
	private Player[] players = new Player[0];
	private Cannon[] cannons = new Cannon[0];
	private Wall[] walls = new Wall[0];
	private Bullet[] bullets = new Bullet[0];
	private Fortress[] fortresses = new Fortress[0];
    private Resource[] resources = new Resource[0];
    private Orb[] orbs = new Orb[0];
    private OrbHolder[] orbHolders = new OrbHolder[0];
	private RemoteSpace centralSpace;
	private RemoteSpace playerPositionsSpace;
	private RemoteSpace playerMovementSpace;
	private RemoteSpace cannonSpace;
	private RemoteSpace bulletSpace;
	private RemoteSpace wallSpace;
	private RemoteSpace fortressSpace;
	private RemoteSpace resourceSpace;
	private RemoteSpace orbSpace;
	private RemoteSpace channelFromServer;
	private RemoteSpace channelToServer;
	public static int id;
	private GamePanel panel;
	private boolean createCannonKeyDown = false;
	private boolean createWallKeyDown = false;
	private boolean dropOrbKeyDown = false;

	private boolean gameStarted = false;
	private boolean gameOver = false;
	private boolean gamePaused = false;
	private boolean windowClosed = false;
	private String winningTeam = "";
	private String name;

	public Client(String address, GameFrame frame, String name) {
		this.frame = frame;
		this.name = name;
		frame.addWindowListener(new java.awt.event.WindowAdapter() {public void windowClosing(java.awt.event.WindowEvent windowEvent) {windowClosed = true;}});
		panel = new GamePanel(this);
		frame.setPanel(panel);
		frame.setVisible(true);
		try {
			centralSpace = new RemoteSpace("tcp://" + address + ":9001/central?keep");
			centralSpace.put("joined", name, Settings.preferredTeam);
			id = (Integer)centralSpace.get(new FormalField(Integer.class))[0];
			connectToServer(address);
			checkGameStarted();
			new Thread(new Timer()).start();
			new Thread(new ServerCheckReader()).start();
			new Thread(new NewHostReader()).start();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void update() {
		try {
			if (gamePaused) {
				return;
			}
			Object[] tuple = centralSpace.queryp(new ActualField("game over"), new FormalField(String.class));
			if (tuple == null) {
				gameOver = false;
				if (gameStarted) {
					// Get the updated status of each object from the server
					updatePlayers();
					updateCannons();
					updateBullets();
					updateWalls();
					updateFortresses();
					updateResources();
					updateOrbs();
				}
				else {
					checkGameStarted();
				}
			}
			else {
				winningTeam = (String) tuple[1];
				gameOver = true;
			}
		} catch (InterruptedException e) {System.out.println("Interrupted");}
		panel.updatePanel();
	}
	
	private void connectToServer(String address) {
		this.address = address;
		try {
			centralSpace = new RemoteSpace("tcp://" + address + ":9001/central?keep");
			channelFromServer = new RemoteSpace("tcp://" + address + ":9001/serverclient"+id+"?keep");
			channelToServer = new RemoteSpace("tcp://" + address + ":9001/client"+id+"server?keep");
			playerPositionsSpace = new RemoteSpace("tcp://" + address + ":9001/playerpositions?keep");
			playerMovementSpace = new RemoteSpace("tcp://" + address + ":9001/playermovement?keep");
			cannonSpace = new RemoteSpace("tcp://" + address + ":9001/cannon?keep");
			bulletSpace = new RemoteSpace("tcp://" + address + ":9001/bullet?keep");
			wallSpace = new RemoteSpace("tcp://" + address + ":9001/wall?keep");
			fortressSpace = new RemoteSpace("tcp://" + address + ":9001/fortress?keep");
			resourceSpace = new RemoteSpace("tcp://" + address + ":9001/resource?keep");
			orbSpace = new RemoteSpace("tcp://" + address + ":9001/orb?keep");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void disconnectFromServer() {
		try {
			centralSpace.close();
			channelFromServer.close();
			channelToServer.close();
			playerPositionsSpace.close();
			playerMovementSpace.close();
			cannonSpace.close();
			bulletSpace.close();
			wallSpace.close();
			fortressSpace.close();
			resourceSpace.close();
			orbSpace.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void checkGameStarted() {
		try {
			if (centralSpace.queryp(new ActualField("started")) != null) {
				gameStarted = true;
			}
		} catch (InterruptedException e) {e.printStackTrace();}
	}

	public void updatePlayers() throws InterruptedException {
		if (playerPositionsSpace.queryp(new ActualField("players")) != null) {
			List<Object[]> playersTuples = playerPositionsSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(String.class));
			players = new Player[playersTuples.size()];
			for (int i = 0; i < playersTuples.size(); i++) {
				Object[] tuple = playersTuples.get(i);
				players[i] = new Player((double)tuple[0], (double)tuple[1], (int)tuple[2], (boolean)tuple[3], (String)tuple[7]);
				players[i].wood = (int)tuple[4];
				players[i].iron = (int)tuple[5];
				players[i].hasOrb = (boolean)tuple[6];
			}	
		}
	}

	public void updateCannons() throws InterruptedException {
		List<Object[]> cannonTuples = cannonSpace.queryAll(new ActualField("cannon"), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		cannons = new Cannon[cannonTuples.size()];
		for (int i = 0; i < cannonTuples.size(); i++) {
			Object[] tuple = cannonTuples.get(i);
			cannons[i] = new Cannon((double)tuple[1], (double)tuple[2], (boolean)tuple[3]);
		}
	}

	public void updateBullets() throws InterruptedException {
		List<Object[]> bulletTuples = bulletSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		bullets = new Bullet[bulletTuples.size()];
		for (int i = 0; i < bulletTuples.size(); i++) {
			Object[] tuple = bulletTuples.get(i);
			bullets[i] = new Bullet((double)tuple[0], (double)tuple[1], (boolean)tuple[2]);
		}
	}

	public void updateWalls() throws InterruptedException {
		List<Object[]> wallTuples = wallSpace.queryAll(
				new ActualField("wall"),
				new FormalField(Integer.class),
				new FormalField(Integer.class),
				new FormalField(Double.class),
				new FormalField(Double.class),
				new FormalField(Boolean.class)
		);
		walls = new Wall[wallTuples.size()];
		for (int i = 0; i < wallTuples.size(); i++) {
			Object[] tuple = wallTuples.get(i);
			// Dirty way of checking if shield
			if((int) tuple[2] > Wall.MAX_HEALTH){
				walls[i] = new Wall((double)tuple[3], (double)tuple[4], Wall.SHIELD_WIDTH, Wall.SHIELD_HEIGHT, (boolean)tuple[5]);
			} else {
				walls[i] = new Wall((int) tuple[1], (int) tuple[2], (double)tuple[3], (double)tuple[4], (boolean)tuple[5]);
			}
		}
	}

	public void updateFortresses() throws InterruptedException {
		List<Object[]> fortressTuples = fortressSpace.queryAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
		fortresses = new Fortress[fortressTuples.size()];
		for (int i = 0; i < fortressTuples.size(); i++) {
			Object[] tuple = fortressTuples.get(i);
			fortresses[i] = new Fortress((int) tuple[0], (int) tuple[1], (int) tuple[2], (boolean) tuple[3]);
		}
	}

	public void updateResources() throws InterruptedException {
        List<Object[]> tuples = resourceSpace.queryAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class));
        resources = new Resource[tuples.size()];
        for (int i = 0; i < tuples.size(); i++) {
            Object[] tuple = tuples.get(i);
            resources[i] = new Resource((int) tuple[0], (int) tuple[1], (int) tuple[2]);
        }
	}
	
	public void updateOrbs() throws InterruptedException {
        List<Object[]> orbTuples = orbSpace.queryAll(new FormalField(Integer.class), new FormalField(Integer.class));
        orbs = new Orb[orbTuples.size()];
        for (int i = 0; i < orbTuples.size(); i++) {
            Object[] tuple = orbTuples.get(i);
            orbs[i] = new Orb((int) tuple[0], (int) tuple[1]);
        }
        List<Object[]> orbHolderTuples = orbSpace.queryAll(new FormalField(Boolean.class), new FormalField(Boolean.class), new FormalField(Boolean.class));
        orbHolders = new OrbHolder[orbHolderTuples.size()];
        for (int i = 0; i < orbHolderTuples.size(); i++) {
            Object[] tuple = orbHolderTuples.get(i);
            orbHolders[i] = new OrbHolder((boolean) tuple[0], (boolean) tuple[1], (boolean) tuple[2]);
        }
	}

	private class Timer implements Runnable {
		public void run() {
			try {
				while (!gamePaused) {
					Thread.sleep((long)(S_BETWEEN_UPDATES*1000));
					update();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class NewHostReader implements Runnable {
		public void run() {
			try {
				while (true) {
					Object[] tuple = channelFromServer.get(new ActualField("newip"), new FormalField(String.class), new FormalField(String.class));
					String newAddress = (String)tuple[1];
					String disconnectedName = (String)tuple[2];
					gamePaused = true;
					panel.updatePanel();
					disconnectFromServer();
					Thread.sleep(2000);
					connectToServer(newAddress);
					gamePaused = false;
					panel.clientDisconnected(disconnectedName);
					new Thread(new Timer()).start();
					new Thread(new ServerCheckReader()).start();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class ServerCheckReader implements Runnable {
		public void run() {
			try {
				while (true) {
					String msg = (String) channelFromServer.get(new FormalField(String.class))[0];
					if (msg.equals("check") && !windowClosed) {
						channelToServer.put("acknowledged");
					}
					else if (msg.equals("stop") && !frame.isHost) {
						System.exit(0);
					}
					else if (msg.equals("clientdisconnected")) {
						String playerName = (String) channelFromServer.get(new FormalField(String.class))[0];
						panel.clientDisconnected(playerName);
					}
					else if (msg.equals("clientconnected")) {
						String playerName = (String) channelFromServer.get(new FormalField(String.class))[0];
						panel.clientConnected(playerName);
					}
					else if (msg.equals("host")) {
						Server newServer = new Server(false);
						if (fortresses[0].getTeam()) {
							newServer.switchHostInitialize(address,
									new ArrayList<>(Arrays.asList(cannons)),
									new ArrayList<>(Arrays.asList(resources)),
									new ArrayList<>(Arrays.asList(orbs)),
									new ArrayList<>(Arrays.asList(orbHolders)),
									new ArrayList<>(Arrays.asList(bullets)),
									fortresses[1], fortresses[0]);
						}
						else {
							newServer.switchHostInitialize(address,
									new ArrayList<>(Arrays.asList(cannons)),
									new ArrayList<>(Arrays.asList(resources)),
									new ArrayList<>(Arrays.asList(orbs)),
									new ArrayList<>(Arrays.asList(orbHolders)),
									new ArrayList<>(Arrays.asList(bullets)),
									fortresses[0], fortresses[1]);
						}
						channelToServer.put("done", Server.getIP());
						Thread.sleep(1500);
						frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						newServer.switchHostInitializeSpaces();
					}
					else if (msg.equals("closethread")) {
						return;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**********************
	 * Controls
	 **********************/
	public void keyDown_Move(String input) throws InterruptedException {
		if (playerMovementSpace.queryp(new ActualField(id), new ActualField(input)) == null) {
			playerMovementSpace.put(id, input);
		}
	}

	public void keyUp_Move(String input) throws InterruptedException {
		playerMovementSpace.getp(new ActualField(id), new ActualField(input));
	}

	public void keyDown_Cannon(String input) throws InterruptedException {
		if(!createCannonKeyDown && cannonSpace.queryp(new ActualField(id), new ActualField(input)) == null){
			cannonSpace.put(id, input);
		}
		createCannonKeyDown = true;
	}

	public void keyUp_Cannon(){
		createCannonKeyDown = false;
	}

	public void keyDownWall(String input) throws InterruptedException {
		if(!createWallKeyDown && wallSpace.queryp(new ActualField(id), new ActualField(input)) == null){
			wallSpace.put(id, input);
		}
		createWallKeyDown = true;
	}

	public void keyUp_Wall(){
		createWallKeyDown = false;
	}

	public void keyDown_DropOrb(String input) throws InterruptedException {
		if (!dropOrbKeyDown) {
			orbSpace.put(id, input);
		}
		dropOrbKeyDown = true;
	}

	public void keyUp_DropOrb(){
		dropOrbKeyDown = false;
	}


	/**********************
	 * Getters and setters
	 **********************/
	public boolean isGameOver() {
		return gameOver;
	}

	public boolean isGameStarted() {
		return gameStarted;
	}

	public boolean isGamePaused() {
		return gamePaused;
	}

	public String getWinningTeam() {
		return winningTeam;
	}

	public Player[] getPlayers() {
		return players;
	}

	public Cannon[] getCannons() {
		return cannons;
	}

	public Wall[] getWalls() {
		return walls;
	}

	public Bullet[] getBullets() {
		return bullets;
	}

	public Fortress[] getFortresses() {
		return fortresses;
	}

	public Resource[] getResources() {
		return resources;
	}

	public Orb[] getOrbs() {
		return orbs;
	}

	public OrbHolder[] getOrbHolders() {
		return orbHolders;
	}

	public static int getId() {
		return id;
	}

	public RemoteSpace getChannelFromServer() {
		return channelFromServer;
	}
}