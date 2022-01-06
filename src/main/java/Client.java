import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import model.*;

public class Client {

	public static final double S_BETWEEN_UPDATES = 0.01;
	private GameFrame frame;
	private Player[] players = new Player[0];
	private Space centralSpace;
	private Space playerPositionsSpace;
	private Space playerMovementSpace;
	private Space cannonSpace;
	private Space bulletSpace;
	private Space fortressSpace;
	private Space resourceSpace;
	private int id;
	private int playerSize = 100;
	private GamePanel panel;
	private int windowWidth = 500;
	private int windowHeight = 500;

	public Client(String address, GameFrame frame) {
		panel = new GamePanel();
		frame.setPanel(panel);
		frame.setVisible(true);
		try {
			centralSpace = new RemoteSpace("tcp://" + address + ":9001/central?keep");
			playerPositionsSpace = new RemoteSpace("tcp://" + address + ":9001/playerpositions?keep");
			playerMovementSpace = new RemoteSpace("tcp://" + address + ":9001/playermovement?keep");
			cannonSpace = new RemoteSpace("tcp://" + address + ":9001/cannon?keep");
			bulletSpace = new RemoteSpace("tcp://" + address + ":9001/bullet?keep");
			fortressSpace = new RemoteSpace("tcp://" + address + ":9001/fortress?keep");
			resourceSpace = new RemoteSpace("tcp://" + address + ":9001/resource?keep");
			centralSpace.put("joined");
			id = (Integer) centralSpace.get(new FormalField(Integer.class))[0];
		} catch (IOException | InterruptedException e) {}
		new Thread(new Timer()).start();
	}

	public void update() {
		try {
			// Get the updated status of each object from the server
			updatePlayers();
			updateCannons();
			updateBullets();
			updateFortresses();
			updateResources();
		} catch (InterruptedException e) {}
		panel.updatePanel();
	}

	public void updatePlayers() throws InterruptedException {
		List<Object[]> playersTuples = playerPositionsSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class));
		players = new Player[playersTuples.size()];
		for (int i = 0; i < playersTuples.size(); i++) {
			Object[] tuple = playersTuples.get(i);
			players[i] = new Player((double)tuple[0], (double)tuple[1], playerSize, playerSize, (int)tuple[2], (boolean)tuple[3]);
		}
	}

	public void updateCannons() throws InterruptedException {
	}

	public void updateBullets() throws InterruptedException {

	}

	public void updateFortresses() throws InterruptedException {

	}

	public void updateResources() throws InterruptedException {

	}

	private class GamePanel extends JPanel implements KeyListener {
		public Graphics2D g2D;

		public GamePanel() {
			setPreferredSize(new Dimension(windowWidth, windowHeight));
			addKeyListener(this);
	        setFocusable(true);
		}

		public void paint(Graphics g) {
			super.paint(g);
			g2D = (Graphics2D) g;
			// Render each object on the screen
			paintPlayers();
			paintCannons();
			paintBullets();
			paintFortresses();
			paintResources();
		}

		public void paintPlayers(){
			for (int i = 0; i < players.length; i++) {
				Player p = players[i];
				g2D.drawRect((int)p.x, (int)p.y, (int)p.width, (int)p.height);
			}
		}

		public void paintCannons(){

		}

		public void paintBullets(){

		}

		public void paintFortresses(){

		}

		public void paintResources(){

		}

		public void updatePanel() {
			repaint();
		}
		
		@Override
		public void keyTyped(KeyEvent e) {}
		@Override
		public void keyPressed(KeyEvent e) {
			String direction = getInput(e.getKeyCode());
			try {
				if (!direction.equals("") && playerMovementSpace.queryp(new ActualField(id), new ActualField(direction)) == null) {
					playerMovementSpace.put(id, direction);
				}
			} catch (InterruptedException e1) {}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			String direction = getInput(e.getKeyCode());
			try {
				playerMovementSpace.getp(new ActualField(id), new ActualField(direction));
			} catch (InterruptedException e1) {}
		}

		private String getInput(int keyCode){
			String direction = "";
			switch (keyCode) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A:
					direction = "left";
					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D:
					direction = "right";
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S:
					direction = "down";
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W:
					direction = "up";
					break;
				default:
					break;
			}
			return direction;
		}
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

}