import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

public class Server {

	public static final double S_BETWEEN_UPDATES = 0.01;
	private static List<Rectangle.Double> players = new ArrayList<Rectangle.Double>();
	private static Space centralSpace;
	private static Space objectPositionsSpace;
	private static Space playerMovementSpace;
	private static int numPlayers = 0;
	private static int playerSpeed = 200;

	public static void main(String[] args) {
		SpaceRepository repository = new SpaceRepository();
		repository.addGate("tcp://localhost:9001/?keep");
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

	public static void update() {
		//Move players that want to move.
		try {
			List<Object[]> movementTuples = playerMovementSpace.queryAll(new FormalField(Integer.class), new FormalField(String.class));
			for (Object[] movementTuple : movementTuples) {
				int playerID = (Integer) movementTuple[0];
				Rectangle.Double player = players.get(playerID);
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
				//movementTuple = playerMovementSpace.getp(new FormalField(Integer.class), new FormalField(String.class));
			}
			objectPositionsSpace.get(new FormalField(List.class));
			objectPositionsSpace.put(players);
		} catch (InterruptedException e) {}
	}

	private static class Timer implements Runnable {
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

	private static class JoinedReader implements Runnable {
		public void run() {
			//Looks for players joining.
			try {
				while (true) {
					centralSpace.get(new ActualField("joined"));
					centralSpace.put(numPlayers);
					players.add(new Rectangle.Double(400, 400, 100, 100));
					numPlayers++;
					System.out.println("Player joined.");
				}
			} catch (InterruptedException e) {}
		}
	}

}