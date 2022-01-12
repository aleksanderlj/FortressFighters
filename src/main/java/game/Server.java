package game;
import java.awt.Rectangle;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
	private List<Space> serverClientChannels = new ArrayList<Space>();
	private List<Space> clientServerChannels = new ArrayList<Space>();
	public List<Player> players = new ArrayList<>();
	public List<Cannon> cannons = new ArrayList<>();
	public List<Resource> resources = new ArrayList<>();
	public List<Wall> walls = new ArrayList<>();
	public List<Orb> orbs = new ArrayList<>();
	public List<OrbHolder> orbHolders = new ArrayList<>();
	public Fortress fortress1;
	public Fortress fortress2;
	public List<Bullet> bullets = new ArrayList<>();
	private SpaceRepository repository;
	public Space centralSpace;
	public Space playerPositionsSpace;
	public Space playerMovementSpace;
	public Space cannonSpace;
	public Space bulletSpace;
	public Space wallSpace;
	public Space fortressSpace;
	public Space resourceSpace;
	public Space orbSpace;
	public Space buffSpace;
	public Space mutexSpace;
	private int numPlayers = 0; //Including disconnected players.
	public int numPlayersTeam1 = 0; //Excluding disconnected players.
	public int numPlayersTeam2 = 0; //Excluding disconnected players.
	private boolean gameStarted = false;
    private int numberOfResources = 10;
    public boolean gameOver = false;
    private OrbPetriNet orbPetriNet1;
    private OrbPetriNet orbPetriNet2;
	public PlayerController playerController;
	public CannonController cannonController;
	public WallController wallController;
	public FortressController fortressController;
	public ResourceController resourceController;
	public OrbController orbController;
	public BuffController buffController;

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
        for (int i = 0; i < numberOfResources; i++) {
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
}