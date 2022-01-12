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
	public static final int INITIAL_RESOURCES = 10;
	private List<Space> serverClientChannels = new ArrayList<Space>();
	private List<Space> clientServerChannels = new ArrayList<Space>();
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
    private OrbPetriNet orbPetriNet1;
    private OrbPetriNet orbPetriNet2;
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
		numPlayersTeam1 = 0;
		numPlayersTeam2 = 0;
		boolean[] disconnected = new boolean[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			disconnected[i] = players.get(i).disconnected;
		}
		players = new ArrayList<Player>();
		for (int i = 0; i < numPlayers; i++) {
			playerController.addPlayer(i);
			players.get(i).disconnected = disconnected[i];
		}
		Collections.shuffle(players);
		cannons.forEach(c -> c.setAlive(false));
		cannons = new ArrayList<Cannon>();
		walls = new ArrayList<Wall>();
		try {
			cannonSpace.getAll(new FormalField(Integer.class), new ActualField(String.class));
			cannonSpace.getAll(new ActualField("cannon"), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
			wallSpace.getAll(new FormalField(Integer.class), new ActualField(String.class));
			wallSpace.getAll(new ActualField("wall"), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
			mutexSpace.get(new ActualField("bulletsLock"));
			orbSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class));
			orbSpace.getAll(new FormalField(Boolean.class), new FormalField(Boolean.class), new FormalField(Boolean.class));
			buffSpace.getAll(new FormalField(Boolean.class), new FormalField(String.class));
			bullets = new ArrayList<Bullet>();
			mutexSpace.put("bulletsLock");
			bulletSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
			buffSpace = new SequentialSpace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		buffController.resetTimers();
		fortress1 = null;
		fortress2 = null;
		fortressController.changeFortress();
        resources = new ArrayList<Resource>();
        orbHolders = new ArrayList<OrbHolder>();
        orbs = new ArrayList<Orb>();
        orbPetriNet1 = new OrbPetriNet(this, buffSpace, false);
        orbPetriNet2 = new OrbPetriNet(this, buffSpace, true);
        new Thread(orbPetriNet1).start();
        new Thread(orbPetriNet2).start();
        for (int i = 0; i < INITIAL_RESOURCES; i++) {
            resources.add(resourceController.createRandomResource());
        }
        for (int i = 0; i < 3; i++) {
            orbController.createNewOrb();
        }
        orbHolders.add(new OrbHolder(false, true, false));
        orbHolders.add(new OrbHolder(true, false, false));
        orbHolders.add(new OrbHolder(false, false, false));
        orbHolders.add(new OrbHolder(true, true, false));
        for (OrbHolder oh : orbHolders) {
        	try {
				orbSpace.put(oh.team, oh.top, oh.hasOrb);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        resourceController.resourcesChanged();
	}
	
	private void resetPetriNet() {
		try {
			buffSpace.put(false, false);
			buffSpace.put(false, true);
			buffSpace.put(true, false);
			buffSpace.put(true, true);
			orbPetriNet1.reset();
			orbPetriNet2.reset();
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
        	resetPetriNet();
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
		serverClientChannels.add(serverClient);
		clientServerChannels.add(clientServer);
		try {
			centralSpace.put(id, "serverclient"+id, "client"+id+"server");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private int getActualNumberOfPlayers() {
		//Get number of players excluding disconnected players.
		return numPlayersTeam1 + numPlayersTeam2;
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
					centralSpace.get(new ActualField("joined"));
					createNewChannel(numPlayers);
					playerController.addPlayer(numPlayers);
					numPlayers++;
					System.out.println("Player joined.");
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
					for (int i = 0; i < numPlayers; i++) {
						if (!players.get(i).disconnected) {
							serverClientChannels.get(i).put("check");
						}
					}
					int numPlayersBefore = numPlayers;
					Thread.sleep(2000);
					for (int i = 0; i < numPlayersBefore; i++) {
						if (!players.get(i).disconnected && clientServerChannels.get(i).getp(new ActualField("acknowledged")) == null) {
							//Client took too long to respond.
							if (i == 0) {
								//The host has disconnected.
								for (int j = 0; j < numPlayersBefore; j++) {
									if (!players.get(j).disconnected) {
										serverClientChannels.get(j).put("stop");
									}
								}
								System.out.println("Host disconnected.");
								Thread.sleep(500);
								System.exit(0);
							}
							else {
								//A client that is not the host has disconnected.
								for (int j = 0; j < numPlayersBefore; j++) {
									if (!players.get(j).disconnected) {
										serverClientChannels.get(j).put("clientdisconnected");
									}
								}
								players.get(i).disconnected = true;
								if (players.get(i).team == true) {
									numPlayersTeam1--;
								}
								else {
									numPlayersTeam2--;
								}
								System.out.println("Player disconnected.");
							}
						}
					}
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