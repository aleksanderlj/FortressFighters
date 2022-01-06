import java.net.InetAddress;
import java.net.UnknownHostException;
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
	private List<Player> players = new ArrayList<Player>();
	private Space centralSpace;
	private Space playerPositionsSpace;
	private Space playerMovementSpace;
	private Space cannonSpace;
	private Space bulletSpace;
	private Space fortressSpace;
	private Space resourceSpace;
	private int numPlayers = 0;
	private int playerSpeed = 200;
	private int playerSize = 100;

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
		repository.add("fortress", fortressSpace);
		repository.add("resource", resourceSpace);
		try {
			playerPositionsSpace.put(players);
		} catch (InterruptedException e) {}
		new Thread(new JoinedReader()).start();
		new Thread(new Timer()).start();
	}

	public void update() {
		try {
			// Handle game logic for each object
			updatePlayers();
			updateCannons();
			updateBullets();
			updateFortresses();
			updateResources();
		} catch (InterruptedException e) {}
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
					player.x -= playerSpeed*S_BETWEEN_UPDATES;
					break;
				case "right":
					player.x += playerSpeed*S_BETWEEN_UPDATES;
					break;
				case "down":
					player.y += playerSpeed*S_BETWEEN_UPDATES;
					break;
				case "up":
					player.y -= playerSpeed*S_BETWEEN_UPDATES;
					break;
				default:
					break;
			}
		}
		for (Player p : players) {
			playerPositionsSpace.put(p.x, p.y, p.id, p.team);
		}
	}

	public void updateCannons(){

	}

	public void updateBullets(){

	}

	public void updateFortresses(){

	}

	public void updateResources(){

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
					players.add(new Player(400, 400, playerSize, playerSize, 0, true));
					numPlayers++;
					System.out.println("Player joined.");
				}
			} catch (InterruptedException e) {}
		}
	}
}