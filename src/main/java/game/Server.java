package game;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import client.Client;
import controller.*;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.RemoteSpace;
import org.jspace.SpaceRepository;

import model.*;

public class Server {

	//public static final double S_BETWEEN_UPDATES = 0.01;
	public static final int SCREEN_WIDTH = 1280;
	public static final int SCREEN_HEIGHT = 720;
	private long deltaTime;
	private long lastUpdate;
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
	private Space switchHostSpace;
	public int playerIDCounter = 0;
	private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean gamePaused = false;
	private PlayerController playerController;
	private CannonController cannonController;
	private WallController wallController;
	private FortressController fortressController;
	private ResourceController resourceController;
	private OrbController orbController;
	private BuffController buffController;
	private int team1Score = 0;
	private int team2Score = 0;

	public Server(boolean createSpaces) {
		lastUpdate = System.currentTimeMillis();
		if (createSpaces) {
			createSpaces();
		}
		playerController = new PlayerController(this);
		cannonController = new CannonController(this);
		wallController = new WallController(this);
		fortressController = new FortressController(this);
		resourceController = new ResourceController(this);
		orbController = new OrbController(this);
		buffController = new BuffController(this);
	}
	
	public void createSpaces() {
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
		switchHostSpace = new SequentialSpace();
		repository.add("central", centralSpace);
		repository.add("playerpositions", playerPositionsSpace);
		repository.add("playermovement", playerMovementSpace);
		repository.add("cannon", cannonSpace);
		repository.add("bullet", bulletSpace);
		repository.add("wall", wallSpace);
		repository.add("fortress", fortressSpace);
		repository.add("resource", resourceSpace);
		repository.add("orb", orbSpace);
		repository.add("buff", buffSpace);
		repository.add("switchhost", switchHostSpace);
		new Thread(new Timer()).start();
		new Thread(new DisconnectChecker()).start();
		new Thread(new JoinedReader()).start();
		try {
			mutexSpace.put("bulletsLock");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void switchHostInitialize(String oldAddress, List<Cannon> cannons, List<Resource> resources, List<Orb> orbs, List<OrbHolder> orbHolders, List<Bullet> bullets, Fortress fortress1, Fortress fortress2) {
		try {
			gamePaused = true;
			RemoteSpace oldSwitchHostSpace = new RemoteSpace("tcp://" + oldAddress + ":9001/switchhost?keep");
			gameOver = (boolean)oldSwitchHostSpace.get(new ActualField("gameOver"), new FormalField(Boolean.class))[1];
			playerIDCounter = (int)oldSwitchHostSpace.get(new ActualField("playerIDCounter"), new FormalField(Integer.class))[1];
			Wall.idCounter = (int)oldSwitchHostSpace.get(new ActualField("idCounter"), new FormalField(Integer.class))[1];
			this.cannons = cannons;
			this.resources = resources;
			this.orbs = orbs;
			this.orbHolders = orbHolders;
			this.bullets = bullets;
			this.fortress1 = fortress1;
			this.fortress2 = fortress2;
			List<Object[]> pTuples = oldSwitchHostSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(String.class), new FormalField(Boolean.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Double.class));
			for (Object[] tuple : pTuples) {
				Player p = new Player((double)tuple[0], (double)tuple[1], (int)tuple[2], (boolean)tuple[4], (String)tuple[3]);
				p.hasOrb = (boolean)tuple[5];
				p.wood = (int)tuple[6];
				p.iron = (int)tuple[7];
				p.stunned = (double)tuple[8];
				players.add(p);
			}
			List<Object[]> wTuples = oldSwitchHostSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class));
			for (Object[] tuple : wTuples) {
				Wall w;
				// Dirty way of checking if it's a shield
				if((int)tuple[4] <= Wall.MAX_HEALTH){
					w = new Wall((int)tuple[2], (int)tuple[4], (double)tuple[0], (double)tuple[1], (boolean)tuple[3]);
					walls.add(w);
				}
			}
			for (Cannon c : cannons) {
				cannonController.activateCannon(c);
			}
			gameStarted = true;
			gamePaused = false;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void switchHostInitializeSpaces() {
        try {
        	createSpaces();
    		orbController.initializeOrbPetriNetsNewHost();
        	for (Player p : players) {
				createNewChannel(p.id, true);
        	}
    		for (Resource r : resources) {
    			resourceSpace.put((int)r.x, (int)r.y, r.getType());
    		}
    		for (Orb o : orbs) {
                orbSpace.put((int)o.x, (int)o.y);
    		}
    		for (Cannon c : cannons) {
    			cannonSpace.put("cannon", c.x, c.y, c.getTeam());
    		}
    		for (Wall w : walls) {
    			wallSpace.put("wall", w.getId(), w.getHealth(), w.x, w.y, w.getTeam());
    		}
    		fortressSpace.put(fortress1.getWood(), fortress1.getIron(), fortress1.getHP(), false);
    		fortressSpace.put(fortress2.getWood(), fortress2.getIron(), fortress2.getHP(), true);
    		centralSpace.put("started");
    		if (gameOver) {
    			gameOver = false;
    			startGame();
    		}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void startGame() {
		try {
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
		long currentTime = System.currentTimeMillis();
		deltaTime = currentTime - lastUpdate;
		lastUpdate = currentTime;
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (gamePaused) {
			return;
		}
		try {
			if (gameStarted) {
				// Handle game logic for each object
				playerController.updatePlayers(deltaTime);
				cannonController.updateCannons(deltaTime);
				cannonController.updateBullets(deltaTime);
				wallController.updateWalls(deltaTime);
				fortressController.updateFortresses(deltaTime);
				resourceController.updateResources(deltaTime);
				orbController.updateOrbs(deltaTime);
				buffController.updateBuffs(deltaTime);
			}
			else {
				if (playerIDCounter >= 2) {
					centralSpace.put("started");
					startGame();
					gameStarted = true;
				}
			}
		} catch (InterruptedException e) {e.printStackTrace();}
	}
	
	public void gameOver(boolean winningTeam) {
		try {
			centralSpace.put("game over", winningTeam ? "red" : "blue");
			centralSpace.getp(new ActualField("scores"), new FormalField(Integer.class), new FormalField(Integer.class));
			if(winningTeam){
				team2Score++;
			} else {
				team1Score++;
			}
			centralSpace.put("scores", team1Score, team2Score);
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
	
	public void switchHost() {
		try {
			gamePaused = true;
			if (getActualNumberOfPlayers() <= 1) {
				//Only one player is connected. Cannot switch host.
				System.exit(0);
			}
			switchHostSpace.put("gameOver", gameOver);
			switchHostSpace.put("playerIDCounter", playerIDCounter);
			switchHostSpace.put("idCounter", Wall.idCounter);
			Player player = getPlayerWithID(Client.id);
			players.remove(player);
			for (Player p : players) {
				switchHostSpace.put(p.x, p.y, p.id, p.name, p.team, p.hasOrb, p.wood, p.iron, p.stunned);
			}
			for (Wall w : walls) {
				switchHostSpace.put(w.x, w.y, w.getId(), w.getTeam(), w.getHealth());
			}
			Player newHost = players.get(0);
			newHost.serverToClient.put("host");
			new Thread(new ForceStop(5000)).start();
			Object[] tuple = newHost.clientToServer.get(new ActualField("done"), new FormalField(String.class));
			for (Player p : players) {
				p.serverToClient.put("newip", (String)tuple[1], player.name);
				p.serverToClient.put("closethread");
			}
			Thread.sleep(1000);
			repository.closeGates();
			System.exit(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void createNewChannel(int id, boolean newHost) {
		Space serverClient = new SequentialSpace();
		Space clientServer = new SequentialSpace();
		repository.add("serverclient"+id, serverClient);
		repository.add("client"+id+"server", clientServer);
		getPlayerWithID(id).clientToServer = clientServer;
		getPlayerWithID(id).serverToClient = serverClient;
		if (!newHost) {
			try {
				centralSpace.put(id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
			while (true) {
				if (!gameOver) {
					update();
				}
			}
		}
	}

	private class JoinedReader implements Runnable {
		
		public void run() {
			try {
				while (true) {
					Object[] tuple = centralSpace.get(new ActualField("joined"), new FormalField(String.class), new FormalField(String.class));
					playerController.addPlayer(playerIDCounter, (String)tuple[1], (String)tuple[2]);
					createNewChannel(playerIDCounter, false);
					playerIDCounter++;
					System.out.println("Player joined.");
					for (Player p : players) {
						p.serverToClient.put("clientconnected");
						p.serverToClient.put((String)tuple[1]);
					}
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
				Thread.sleep(3000);
				while (true) {
					for (int i = 0; i < getActualNumberOfPlayers(); i++) {
						players.get(i).serverToClient.put("check");
					}
					List<Player> playersBefore = new ArrayList<Player>(players);
					Thread.sleep(2000);
					List<Player> playersToRemove = new ArrayList<>();
					for (int i = 0; i < playersBefore.size(); i++) {
						if (playersBefore.get(i).clientToServer.getp(new ActualField("acknowledged")) == null) {
							//Client took too long to respond.
							if (playersBefore.get(i).id == Client.id) {
								//The host has disconnected.
								if (playersBefore.get(i).hasOrb) {
									orbController.createNewOrb();
								}
								System.out.println("Host disconnected.");
								switchHost();
							}
							else {
								//A client that is not the host has disconnected.
								for (int j = 0; j < getActualNumberOfPlayers(); j++) {
									players.get(j).serverToClient.put("clientdisconnected");
									players.get(j).serverToClient.put(playersBefore.get(i).name);
								}
								if (playersBefore.get(i).hasOrb) {
									orbController.createNewOrb();
								}
								playersToRemove.add(playersBefore.get(i));
								System.out.println("Player disconnected.");
							}
						}
					}
					for (Player player : playersToRemove) {
						players.remove(player);
					}
				}
			} catch (InterruptedException e) {e.printStackTrace();}
		}
	}

	private class ForceStop implements Runnable{
		int delay;
		public ForceStop(int delay){
			this.delay = delay;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(delay);
				for (Player p : players) {
					p.serverToClient.put("stop");
				}
				Thread.sleep(1000);
				repository.closeGates();
				System.exit(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public int getNumPlayers(boolean team){
		return (int)players.stream().filter(p -> p.team == team).count();
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