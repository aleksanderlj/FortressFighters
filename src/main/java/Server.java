import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import model.*;

public class Server {

	public static final double S_BETWEEN_UPDATES = 0.01;
	private String address;
	private List<Player> players = new ArrayList<Player>();
	private Space centralSpace;
	private Space objectPositionsSpace;
	private Space playerMovementSpace;
	private int numPlayers = 0;
	private int playerSpeed = 200;
	private int playerSize = 100;

	public Server() {
		SpaceRepository repository = new SpaceRepository();
		repository.addGate("tcp://localhost:9001/?keep");
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		centralSpace = new SequentialSpace();
		objectPositionsSpace = new SequentialSpace();
		playerMovementSpace = new SequentialSpace();
		repository.add("central", centralSpace);
		repository.add("objectpositions", objectPositionsSpace);
		repository.add("playermovement", playerMovementSpace);
		try {
			objectPositionsSpace.put(players);
		} catch (InterruptedException e) {}
		new Thread(new JoinedReader()).start();
		new Thread(new Timer()).start();
	}

	public void update() {
		//Move players that want to move.
		try {
			for (Player p : players) {
				objectPositionsSpace.getp(new ActualField(p.x), new ActualField(p.y), new ActualField(p.id), new ActualField(p.team));
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
				objectPositionsSpace.put(p.x, p.y, p.id, p.team);
			}
		} catch (InterruptedException e) {}
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

	public String getAddress() {
		return address;
	}
}