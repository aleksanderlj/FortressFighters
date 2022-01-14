package game;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import controller.*;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.QueueSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import model.*;

public class Server {

	public static final double S_BETWEEN_UPDATES = 0.01;
	public static final int SCREEN_WIDTH = 1280;
	public static final int SCREEN_HEIGHT = 720;
	private List<Player> players = new ArrayList<>();
	private List<Cannon> cannons = new ArrayList<>();
	private List<Resource> resources = new ArrayList<>();
	private List<Wall> walls = new ArrayList<>();
	private List<Orb> orbs = new ArrayList<>();
	private List<OrbHolder> orbHolders = new ArrayList<>();
	private Fortress fortress1;
	private Fortress fortress2;
	private List<Bullet> bullets = new ArrayList<>();
	private SpaceRepository repository;
	private Space centralSpace;
	private Space playerPositionsSpace;
	private Space playerMovementSpace;
	private Space cannonSpace;
	private Space bulletSpace;
	private Space wallSpace;
	private Space fortressSpace;
	private Space resourceSpace;
	private Space orbSpace;
	private Space buffSpace;
	private Space mutexSpace;
	public int numPlayers = 0; //Including disconnected players.
	public int numPlayersTeam1 = 0; //Excluding disconnected players.
	public int numPlayersTeam2 = 0; //Excluding disconnected players.
	private boolean gameStarted = false;
    private boolean gameOver = false;
	private PlayerController playerController;
	private CannonController cannonController;
	private WallController wallController;
	private FortressController fortressController;
	private ResourceController resourceController;
	private OrbController orbController;
	private BuffController buffController;

	public Server() {
		repository = new SpaceRepository();
		repository.addGate("tcp://" + getIP() + ":9001/?keep");
		centralSpace = new SequentialSpace();
		playerPositionsSpace = new SequentialSpace();
		playerMovementSpace = new SequentialSpace();
		cannonSpace = new SequentialSpace();
		bulletSpace = new SequentialSpace();
		wallSpace = new SequentialSpace();
		fortressSpace = new SequentialSpace();
		resourceSpace = new SequentialSpace();
		orbSpace = new SequentialSpace();
		buffSpace = new SequentialSpace();
		mutexSpace = new SequentialSpace();
		repository.add("central", centralSpace);
		repository.add("playerpositions", playerPositionsSpace);
		repository.add("playermovement", playerMovementSpace);
		repository.add("cannon", cannonSpace);
		repository.add("bullet", bulletSpace);
		repository.add("wall", wallSpace);
		repository.add("fortress", fortressSpace);
		repository.add("resource", resourceSpace);
		repository.add("orb", orbSpace);
		playerController = new PlayerController(this);
		cannonController = new CannonController(this);
		wallController = new WallController(this);
		fortressController = new FortressController(this);
		resourceController = new ResourceController(this);
		orbController = new OrbController(this);
		buffController = new BuffController(this);
		new Thread(new Timer()).start();
		new Thread(new DisconnectChecker()).start();
		new Thread(new JoinedReader()).start();
		try {
			mutexSpace.put("bulletsLock");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void startGame() {
		try {
			numPlayersTeam1 = 0;
			numPlayersTeam2 = 0;
			playerController.initializePlayers();
			cannonController.initializeCannons();
			wallController.initializeWalls();
			cannonController.initializeBullets();
			orbController.initializeOrbs();
			buffController.initializeBuffs();
			fortressController.initializeFortresses();
			resourceController.initializeResources();
			orbController.initializeOrbHolders();
			orbController.initializeOrbPetriNets();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void update() {
		try {
			if (gameStarted) {
				// Handle game logic for each object
				playerController.updatePlayers();
				cannonController.updateCannons();
				cannonController.updateBullets();
				wallController.updateWalls();
				fortressController.updateFortresses();
				resourceController.updateResources();
				orbController.updateOrbs();
				buffController.updateBuffs();
			}
			else {
				if (numPlayers >= 2) {
					centralSpace.put("started");
					startGame();
					gameStarted = true;
				}
			}
		} catch (InterruptedException e) {e.printStackTrace();}
	}
	
	public void gameOver(boolean winningTeam) {
		try {
			centralSpace.put("game over", winningTeam ? "blue" : "red");
			gameOver = true;
        	orbController.resetPetriNet();
			Thread.sleep(5000);
			centralSpace.get(new ActualField("game over"), new FormalField(String.class));
			startGame();
			gameOver = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private void createNewChannel(int id) {
		Space serverClient = new QueueSpace();
		Space clientServer = new QueueSpace();
		repository.add("serverclient"+id, serverClient);
		repository.add("client"+id+"server", clientServer);
		getPlayerWithID(id).clientToServer = clientServer;
		getPlayerWithID(id).serverToClient = serverClient;
		try {
			centralSpace.put(id, "serverclient"+id, "client"+id+"server");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public int getActualNumberOfPlayers() {
		//Get number of players excluding disconnected players.
		return players.size();
	}
	
	public Player getPlayerWithID(int id) {
		for (Player player : players) {
			if (player.id == id) {
				return player;
			}
		}
		return null;
	}

	public static String getIP(){
		String address = "localhost";
		try {
			address = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return address;
	}
	
	private class Timer implements Runnable {
		public void run() {
			try {
				while (true) {
					if (!gameOver) {
						Thread.sleep((long)(S_BETWEEN_UPDATES*1000));
						update();	
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class JoinedReader implements Runnable {
		
		public void run() {
			try {
				while (true) {
					System.out.println(players.size());
					centralSpace.get(new ActualField("joined"));
					playerController.addPlayer(numPlayers);
					createNewChannel(numPlayers);
					numPlayers++;
					System.out.println("Player joined.");
					System.out.println(players.size());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class DisconnectChecker implements Runnable {
		public void run() {
			//Protocol checking if players are still in the game.
			try {
				while (true) {
					for (int i = 0; i < getActualNumberOfPlayers(); i++) {
						players.get(i).serverToClient.put("check");
					}
					int numPlayersBefore = getActualNumberOfPlayers();
					Thread.sleep(2000);
					List<Player> newPlayers = new ArrayList<>();
					for (int i = 0; i < numPlayersBefore; i++) {
						if (players.get(i).clientToServer.getp(new ActualField("acknowledged")) == null) {
							//Client took too long to respond.
							if (i == 0) {
								//The host has disconnected.
								for (int j = 0; j < getActualNumberOfPlayers(); j++) {
									players.get(j).serverToClient.put("stop");
								}
								System.out.println("Host disconnected.");
								players.remove(i);
								Thread.sleep(500);
								System.exit(0);
							}
							else {
								//A client that is not the host has disconnected.
								for (int j = 0; j < getActualNumberOfPlayers(); j++) {
									players.get(j).serverToClient.put("clientdisconnected");
								}
								if (players.get(i).team == true) {
									numPlayersTeam1--;
								}
								else {
									numPlayersTeam2--;
								}
								if (players.get(i).hasOrb) {
									orbController.createNewOrb();
								}
								System.out.println("Player disconnected.");
							}
						}
						else {
							newPlayers.add(players.get(i));
						}
					}
					players.clear();
					players.addAll(newPlayers);
				}
			} catch (InterruptedException e) {e.printStackTrace();}
		}
	}



	/*********************
	 Getters and setters
	*********************/
	public List<Player> getPlayers() {
		return players;
	}

	public List<Cannon> getCannons() {
		return cannons;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}

	public List<Wall> getWalls() {
		return walls;
	}

	public List<Orb> getOrbs() {
		return orbs;
	}

	public void setOrbs(List<Orb> orbs) {
		this.orbs = orbs;
	}

	public List<OrbHolder> getOrbHolders() {
		return orbHolders;
	}

	public Fortress getFortress1() {
		return fortress1;
	}

	public void setFortress1(Fortress fortress1) {
		this.fortress1 = fortress1;
	}

	public Fortress getFortress2() {
		return fortress2;
	}

	public void setFortress2(Fortress fortress2) {
		this.fortress2 = fortress2;
	}

	public List<Bullet> getBullets() {
		return bullets;
	}

	public Space getPlayerPositionsSpace() {
		return playerPositionsSpace;
	}

	public Space getPlayerMovementSpace() {
		return playerMovementSpace;
	}

	public Space getCannonSpace() {
		return cannonSpace;
	}

	public Space getBulletSpace() {
		return bulletSpace;
	}

	public Space getWallSpace() {
		return wallSpace;
	}

	public Space getFortressSpace() {
		return fortressSpace;
	}

	public Space getResourceSpace() {
		return resourceSpace;
	}

	public Space getOrbSpace() {
		return orbSpace;
	}

	public Space getBuffSpace() {
		return buffSpace;
	}

	public void setBuffSpace(Space buffSpace) {
		this.buffSpace = buffSpace;
	}

	public Space getMutexSpace() {
		return mutexSpace;
	}

	public PlayerController getPlayerController() {
		return playerController;
	}

	public CannonController getCannonController() {
		return cannonController;
	}

	public WallController getWallController() {
		return wallController;
	}

	public FortressController getFortressController() {
		return fortressController;
	}

	public ResourceController getResourceController() {
		return resourceController;
	}

	public OrbController getOrbController() {
		return orbController;
	}

	public BuffController getBuffController() {
		return buffController;
	}

	public boolean isGameOver() {
		return gameOver;
	}
}