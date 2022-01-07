import java.util.ArrayList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import model.*;

public class Server {

	public static final double S_BETWEEN_UPDATES = 0.01;
	public static final int SCREEN_WIDTH = 1280;
	public static final int SCREEN_HEIGHT = 720;
	private List<Player> players = new ArrayList<Player>();
	private List<Cannon> cannons = new ArrayList<>();
	private Space centralSpace;
	private Space playerPositionsSpace;
	private Space playerMovementSpace;
	private Space cannonSpace;
	private Space bulletSpace;
	private Space wallSpace;
	private Space fortressSpace;
	private Space resourceSpace;
	private int numPlayers = 0;

	public Server() {
		SpaceRepository repository = new SpaceRepository();
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
		try {
			playerPositionsSpace.put(players);
		} catch (InterruptedException e) {e.printStackTrace();}
		new Thread(new JoinedReader()).start();
		new Thread(new Timer()).start();
	}

	public void update() {
		try {
			// Handle game logic for each object
			updatePlayers();
			updateCannons();
			updateBullets();
			updateWalls();
			updateFortresses();
			updateResources();
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
			playerPositionsSpace.put(p.x, p.y, p.id, p.team);
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
				// TODO Spend resources on canon
				cannons.add(newCannon);
				cannonSpace.put("cannon", newCannon.x + player.width / 4, newCannon.y + player.height / 2, newCannon.getTeam());
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
					centralSpace.put(numPlayers);
					players.add(new Player(400, 400, 0, true));
					numPlayers++;
					System.out.println("Player joined.");
				}
			} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}