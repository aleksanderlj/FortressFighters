package game;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import controller.*;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.QueueSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

public class Server {

	public static final double S_BETWEEN_UPDATES = 0.01;
	public static final int SCREEN_WIDTH = 1280;
	public static final int SCREEN_HEIGHT = 720;
	public static final int INITIAL_RESOURCES = 10;
	private List<Space> serverClientChannels = new ArrayList<Space>();
	private List<Space> clientServerChannels = new ArrayList<Space>();
	private SpaceRepository repository;
	private Space centralSpace;
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
		mutexSpace = new SequentialSpace();
		playerController = new PlayerController(this);
		cannonController = new CannonController(this);
		wallController = new WallController(this);
		fortressController = new FortressController(this);
		resourceController = new ResourceController(this);
		orbController = new OrbController(this);
		buffController = new BuffController(this);
		repository.add("central", centralSpace);
		repository.add("playerpositions", playerController.getPlayerPositionsSpace());
		repository.add("playermovement", playerController.getPlayerMovementSpace());
		repository.add("cannon", cannonController.getCannonSpace());
		repository.add("bullet", cannonController.getBulletSpace());
		repository.add("wall", wallController.getWallSpace());
		repository.add("fortress", fortressController.getFortressSpace());
		repository.add("resource", resourceController.getResourceSpace());
		repository.add("orb", orbController.getOrbSpace());
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
		playerController.initializePlayers();
		wallController.initializeWalls();
		try {
			cannonController.resetCannonSpace();
			wallController.resetWallSpace();
			mutexSpace.get(new ActualField("bulletsLock"));
			orbController.resetOrbSpace();
			buffController.resetBuffSpace();
			cannonController.initializeBullets();
			mutexSpace.put("bulletsLock");
			cannonController.resetBulletSpace();
			buffController.setBuffSpace(new SequentialSpace());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		buffController.resetTimers();
		fortressController.initializeFortresses();
		resourceController.initializeResources();
        orbController.initializeOrbHolders();
		orbController.initializeOrbs();
        orbController.initializeOrbPetriNets();
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
						if (!playerController.getPlayers().get(i).disconnected) {
							serverClientChannels.get(i).put("check");
						}
					}
					int numPlayersBefore = numPlayers;
					Thread.sleep(2000);
					for (int i = 0; i < numPlayersBefore; i++) {
						if (false) {
							//Client took too long to respond.
							if (i == 0) {
								//The host has disconnected.
								for (int j = 0; j < numPlayersBefore; j++) {
									if (!playerController.getPlayers().get(j).disconnected) {
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
									if (!playerController.getPlayers().get(j).disconnected) {
										serverClientChannels.get(j).put("clientdisconnected");
									}
								}
								playerController.getPlayers().get(i).disconnected = true;
								if (playerController.getPlayers().get(i).team == true) {
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