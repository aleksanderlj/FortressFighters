import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
	private List<Player> players = new ArrayList<Player>();
	private List<Cannon> cannons = new ArrayList<>();
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
	private int numPlayers = 0; //Including disconnected players.
	private int numPlayersTeam1 = 0; //Excluding disconnected players.
	private int numPlayersTeam2 = 0; //Excluding disconnected players.
	private boolean gameStarted = false;

	public Server() {
		repository = new SpaceRepository();
		repository.addGate("tcp://localhost:9001/?keep");
		centralSpace = new SequentialSpace();
		playerPositionsSpace = new SequentialSpace();
		playerMovementSpace = new SequentialSpace();
		cannonSpace = new SequentialSpace();
		bulletSpace = new SequentialSpace();
		fortressSpace = new SequentialSpace();
		resourceSpace = new SequentialSpace();
		repository.add("central", centralSpace);
		repository.add("playerpositions", playerPositionsSpace);
		repository.add("playermovement", playerMovementSpace);
		repository.add("cannon", cannonSpace);
		repository.add("bullet", bulletSpace);
		repository.add("wall", wallSpace);
		repository.add("fortress", fortressSpace);
		repository.add("resource", resourceSpace);
		new Thread(new JoinedReader()).start();
		new Thread(new Timer()).start();
		new Thread(new DisconnectChecker()).start();
	}
	
	public void startGame() {
		boolean[] disconnected = new boolean[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			disconnected[i] = players.get(i).disconnected;
		}
		players = new ArrayList<Player>();
		for (int i = 0; i < numPlayers; i++) {
			addPlayer(i);
			players.get(i).disconnected = disconnected[i];
		}
		Collections.shuffle(players);
		cannons = new ArrayList<Cannon>();
	}
	
	private void addPlayer(int id) {
		if (numPlayersTeam1 == numPlayersTeam2) {
			int team = (new Random()).nextInt(2);
			if (team == 0) {
				players.add(new Player(400, 400, id, true));
				numPlayersTeam1++;
			}
			else {
				players.add(new Player(400, 400, id, false));
				numPlayersTeam2++;
			}
		}
		else if (numPlayersTeam1 > numPlayersTeam2) {
			players.add(new Player(400, 400, id, false));
			numPlayersTeam2++;
		}
		else {
			players.add(new Player(400, 400, id, true));
			numPlayersTeam1++;
		}
	}

	public void update() {
		try {
			if (gameStarted) {
				// Handle game logic for each object
				updatePlayers();
				updateCannons();
				updateBullets();
				updateWalls();
				updateFortresses();
				updateResources();	
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

	public void updatePlayers() throws InterruptedException {
		for (Player p : players) {
			playerPositionsSpace.getp(new ActualField(p.x), new ActualField(p.y), new ActualField(p.id), new ActualField(p.team));
		}
		List<Object[]> movementTuples = playerMovementSpace.queryAll(new FormalField(Integer.class), new FormalField(String.class));
		for (Object[] movementTuple : movementTuples) {
			int playerID = (Integer) movementTuple[0];
			Player player = players.get(playerID);
			String direction = (String) movementTuple[1];
			switch (direction) {
				case "left":
					player.x -= Player.SPEED * S_BETWEEN_UPDATES;
					break;
				case "right":
					player.x += Player.SPEED * S_BETWEEN_UPDATES;
					break;
				case "down":
					player.y += Player.SPEED * S_BETWEEN_UPDATES;
					break;
				case "up":
					player.y -= Player.SPEED * S_BETWEEN_UPDATES;
					break;
				default:
					break;
			}
		}
		for (Player p : players) {
			if (!p.disconnected) {
				playerPositionsSpace.put(p.x, p.y, p.id, p.team);
			}
		}
	}

	public void updateCannons() throws InterruptedException {
		List<Object[]> cannonCommands = cannonSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
		Cannon newCannon;
		for (Object[] command : cannonCommands) {
			int id = (int) command[0];
			Player player = players.get(id);
			newCannon = new Cannon(player.x, player.y, player.team);

			// Only build cannon if it's not colliding with another cannon
			if(cannons.stream().noneMatch(newCannon::intersects)){
				// TODO Spend resources on cannon
				cannons.add(newCannon);
				cannonSpace.put("cannon", newCannon.x + player.width / 4, newCannon.y + player.height / 2, newCannon.getTeam());
				new Thread(new CannonShooter(newCannon)).start(); // TODO Need some way to stop and remove this when game is reset or cannon is destroyed
			}
		}

	}

	public void updateBullets() throws InterruptedException{

	}

	public void updateWalls() throws InterruptedException{

	}

	public void updateFortresses() throws InterruptedException{

	}

	public void updateResources() throws InterruptedException{

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
	
	private class Timer implements Runnable {
		public void run() {
			try {
				while (true) {
					Thread.sleep((long)(S_BETWEEN_UPDATES*1000));
					update();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class JoinedReader implements Runnable {
		public void run() {
			//Looks for players joining.
			try {
				while (true) {
					centralSpace.get(new ActualField("joined"));
					createNewChannel(numPlayers);
					addPlayer(numPlayers);
					numPlayers++;
					System.out.println("Player joined.");
				}
			} catch (InterruptedException e) {e.printStackTrace();}
		}
	}

	public class CannonShooter implements Runnable {
		Cannon cannon;

		public CannonShooter(Cannon cannon){
			this.cannon = cannon;
		}

		public void run() {
			try {
				while(true){ // TODO stop while loop when cannon is destroyed?
					if(cannon.isActive()){
						Thread.sleep(3000);
						Bullet bullet;
						if(cannon.getTeam()){
							bullet = new Bullet(cannon.x + 10 + Bullet.WIDTH, cannon.y + Cannon.HEIGHT + 6, cannon.getTeam());
						} else {
							bullet = new Bullet(cannon.x + Cannon.WIDTH + 21 - Bullet.WIDTH, cannon.y + Cannon.HEIGHT + 7, cannon.getTeam());
						}
						bullets.add(bullet);
						bulletSpace.put(bullet.x, bullet.y, bullet.getTeam());
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
			} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}