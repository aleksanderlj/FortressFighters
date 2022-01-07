import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
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
	private Cannon[] cannons = new Cannon[0];
	private Space centralSpace;
	private Space playerPositionsSpace;
	private Space playerMovementSpace;
	private Space cannonSpace;
	private Space bulletSpace;
	private Space wallSpace;
	private Space fortressSpace;
	private Space resourceSpace;
	private int id;
	private GamePanel panel;
	private boolean createCannonKeyDown = false;
	private BufferedImage manblue, manred, cannonblue, cannonred;

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
			wallSpace = new RemoteSpace("tcp://" + address + ":9001/wall?keep");
			fortressSpace = new RemoteSpace("tcp://" + address + ":9001/fortress?keep");
			resourceSpace = new RemoteSpace("tcp://" + address + ":9001/resource?keep");
			centralSpace.put("joined");
			id = (Integer) centralSpace.get(new FormalField(Integer.class))[0];

			// Load image resources
			manblue = ImageIO.read(getClass().getResource("manblue.png"));
			manred = ImageIO.read(getClass().getResource("manred.png"));
			cannonblue = ImageIO.read(getClass().getResource("cannonblue.png"));
			cannonred = ImageIO.read(getClass().getResource("cannonred.png"));
		} catch (IOException | InterruptedException e) {e.printStackTrace();}

		new Thread(new Timer()).start();
	}

	public void update() {
		try {
			// Get the updated status of each object from the server
			updatePlayers();
			updateCannons();
			updateBullets();
			updateWalls();
			updateFortresses();
			updateResources();
		} catch (InterruptedException e) {e.printStackTrace();}
		panel.updatePanel();
	}

	public void updatePlayers() throws InterruptedException {
		List<Object[]> playersTuples = playerPositionsSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class));
		players = new Player[playersTuples.size()];
		for (int i = 0; i < playersTuples.size(); i++) {
			Object[] tuple = playersTuples.get(i);
			players[i] = new Player((double)tuple[0], (double)tuple[1], (int)tuple[2], (boolean)tuple[3]);
		}
	}

	public void updateCannons() throws InterruptedException {
		List<Object[]> cannonTuples = cannonSpace.queryAll(new ActualField("cannon"), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		cannons = new Cannon[cannonTuples.size()];
		for (int i = 0; i < cannonTuples.size(); i++) {
			Object[] tuple = cannonTuples.get(i);
			cannons[i] = new Cannon((double)tuple[1], (double)tuple[2], (boolean)tuple[3]);
		}
	}

	public void updateBullets() throws InterruptedException {

	}

	public void updateWalls() throws InterruptedException {

	}

	public void updateFortresses() throws InterruptedException {

	}

	public void updateResources() throws InterruptedException {

	}

	private class GamePanel extends JPanel implements KeyListener {
		public Graphics2D g2D;

		public GamePanel() {
			setPreferredSize(new Dimension(Server.SCREEN_WIDTH, Server.SCREEN_HEIGHT));
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
			paintWalls();
			paintFortresses();
			paintResources();
		}

		public void paintPlayers(){
			for (int i = 0; i < players.length; i++) {
				Player p = players[i];
				if(p.team){
					g2D.drawImage(manred, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
				} else {
					g2D.drawImage(manblue, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
				}
				//g2D.drawRect((int) p.x, (int) p.y, (int) p.width, (int) p.height);
			}
		}

		public void paintCannons(){
			for (Cannon c : cannons) {
				if(c.getTeam()){
					g2D.drawImage(cannonred, (int) c.x, (int) c.y, (int) c.width, (int) c.height, null);
				} else {
					g2D.drawImage(cannonblue, (int) c.x, (int) c.y, (int) c.width, (int) c.height, null);
				}
				//g2D.drawRect((int) c.x, (int) c.y, (int) c.width, (int) c.height);
			}
		}

		public void paintBullets(){

		}

		public void paintWalls(){

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
			String input = getInput(e.getKeyCode());
			try {
				switch (input){
					case "left":
					case "right":
					case "down":
					case "up":
						if (playerMovementSpace.queryp(new ActualField(id), new ActualField(input)) == null) {
							playerMovementSpace.put(id, input);
						}
						break;
					case "createcannon":
						if(!createCannonKeyDown && cannonSpace.queryp(new ActualField(id), new ActualField(input)) == null){
							cannonSpace.put(id, input);
						}
						createCannonKeyDown = true;
						break;
					default:
						break;
				}
			} catch (InterruptedException e1) {e1.printStackTrace();}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			String input = getInput(e.getKeyCode());
			try {
				switch (input){
					case "left":
					case "right":
					case "down":
					case "up":
						playerMovementSpace.getp(new ActualField(id), new ActualField(input));
						break;
					case "createcannon":
						createCannonKeyDown = false;
						break;
					default:
						break;
				}
			} catch (InterruptedException e1) {e1.printStackTrace();}
		}

		private String getInput(int keyCode){
			String input = "";
			switch (keyCode) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A:
					input = "left";
					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D:
					input = "right";
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S:
					input = "down";
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W:
					input = "up";
					break;
				case KeyEvent.VK_Q:
					input = "createcannon";
					break;
				default:
					break;
			}
			return input;
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