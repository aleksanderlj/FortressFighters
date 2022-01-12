package game;
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
	private Wall[] walls = new Wall[0];
	private Bullet[] bullets = new Bullet[0];
	private Fortress[] fortresses = new Fortress[0];
    private Resource[] resources = new Resource[0];
    private Orb[] orbs = new Orb[0];
    private OrbHolder[] orbHolders = new OrbHolder[0];
	private Space centralSpace;
	private Space playerPositionsSpace;
	private Space playerMovementSpace;
	private Space cannonSpace;
	private Space bulletSpace;
	private Space wallSpace;
	private Space fortressSpace;
	private Space resourceSpace;
	private Space orbSpace;
	private Space channelFromServer;
	private Space channelToServer;
	private int id;
	private GamePanel panel;
	private boolean createCannonKeyDown = false;
	private boolean createWallKeyDown = false;
	private BufferedImage manblue, manred, cannonblue, cannonred, fortressblue, fortressred, wood, iron, bulletred, bulletblue;
	private boolean gameStarted = false;
	private boolean gameOver = false;
	private boolean windowClosed = false;
	private String winningTeam = "";
	private String defaultFont;
	private Font fortressStatusFont;

	public Client(String address, GameFrame frame) {
		this.frame = frame;
		frame.addWindowListener(new java.awt.event.WindowAdapter() {public void windowClosing(java.awt.event.WindowEvent windowEvent) {windowClosed = true;}});
		panel = new GamePanel();
		frame.setPanel(panel);
		frame.setVisible(true);
		defaultFont = "Comic Sans MS";
		try {
			centralSpace = new RemoteSpace("tcp://" + address + ":9001/central?keep");
			playerPositionsSpace = new RemoteSpace("tcp://" + address + ":9001/playerpositions?keep");
			playerMovementSpace = new RemoteSpace("tcp://" + address + ":9001/playermovement?keep");
			cannonSpace = new RemoteSpace("tcp://" + address + ":9001/cannon?keep");
			bulletSpace = new RemoteSpace("tcp://" + address + ":9001/bullet?keep");
			wallSpace = new RemoteSpace("tcp://" + address + ":9001/wall?keep");
			fortressSpace = new RemoteSpace("tcp://" + address + ":9001/fortress?keep");
			resourceSpace = new RemoteSpace("tcp://" + address + ":9001/resource?keep");
			orbSpace = new RemoteSpace("tcp://" + address + ":9001/orb?keep");
			centralSpace.put("joined");
			Object[] tuple = centralSpace.get(new FormalField(Integer.class), new FormalField(String.class), new FormalField(String.class));
			id = (Integer) tuple[0];
			channelFromServer = new RemoteSpace("tcp://" + address + ":9001/"+((String) tuple[1])+"?keep");
			channelToServer = new RemoteSpace("tcp://" + address + ":9001/"+((String) tuple[2])+"?keep");
			// Load image resources
			manblue = ImageIO.read(getClass().getClassLoader().getResource("manblue.png"));
			manred = ImageIO.read(getClass().getClassLoader().getResource("manred.png"));
			cannonblue = ImageIO.read(getClass().getClassLoader().getResource("cannonblue.png"));
			cannonred = ImageIO.read(getClass().getClassLoader().getResource("cannonred.png"));
			fortressblue = ImageIO.read(getClass().getClassLoader().getResource("fortressblue.png"));
			fortressred = ImageIO.read(getClass().getClassLoader().getResource("fortressred.png"));
			wood = ImageIO.read(getClass().getClassLoader().getResource("wood.png"));
			iron = ImageIO.read(getClass().getClassLoader().getResource("iron.png"));
			bulletred = ImageIO.read(getClass().getClassLoader().getResource("bulletred.png"));
			bulletblue = ImageIO.read(getClass().getClassLoader().getResource("bulletblue.png"));
			fortressStatusFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("alagard.ttf"));
			fortressStatusFont = fortressStatusFont.deriveFont(Font.PLAIN, 36);
			checkGameStarted();
			new Thread(new Timer()).start();
			new Thread(new ServerCheckReader()).start();
		} catch (IOException | InterruptedException | FontFormatException e) {e.printStackTrace();}
	}

	public void update() {
		try {
			Object[] tuple = centralSpace.queryp(new ActualField("game over"), new FormalField(String.class));
			if (tuple == null) {
				gameOver = false;
				if (gameStarted) {
					// Get the updated status of each object from the server
					updatePlayers();
					updateCannons();
					updateBullets();
					updateWalls();
					updateFortresses();
					updateResources();
					updateOrbs();
				}
				else {
					checkGameStarted();
				}
			}
			else {
				winningTeam = (String) tuple[1];
				gameOver = true;
			}
		} catch (InterruptedException e) {e.printStackTrace();}
		panel.updatePanel();
	}
	
	private void checkGameStarted() {
		try {
			if (centralSpace.queryp(new ActualField("started")) != null) {
				gameStarted = true;
			}
		} catch (InterruptedException e) {e.printStackTrace();}
	}

	public void updatePlayers() throws InterruptedException {
		if (playerPositionsSpace.queryp(new ActualField("players")) != null) {
			List<Object[]> playersTuples = playerPositionsSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
			players = new Player[playersTuples.size()];
			for (int i = 0; i < playersTuples.size(); i++) {
				Object[] tuple = playersTuples.get(i);
				players[i] = new Player((double)tuple[0], (double)tuple[1], (int)tuple[2], (boolean)tuple[3]);
				players[i].wood = (int)tuple[4];
				players[i].iron = (int)tuple[5];
				players[i].hasOrb = (boolean)tuple[6];
			}	
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
		List<Object[]> bulletTuples = bulletSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		bullets = new Bullet[bulletTuples.size()];
		for (int i = 0; i < bulletTuples.size(); i++) {
			Object[] tuple = bulletTuples.get(i);
			bullets[i] = new Bullet((double)tuple[0], (double)tuple[1], (boolean)tuple[2]);
		}
	}

	public void updateWalls() throws InterruptedException {
		List<Object[]> wallTuples = wallSpace.queryAll(new ActualField("wall"), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		walls = new Wall[wallTuples.size()];
		for (int i = 0; i < wallTuples.size(); i++) {
			Object[] tuple = wallTuples.get(i);
			walls[i] = new Wall((int) tuple[1], (double)tuple[2], (double)tuple[3], (boolean)tuple[4]);
		}
	}

	public void updateFortresses() throws InterruptedException {
		List<Object[]> fortressTuples = fortressSpace.queryAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
		fortresses = new Fortress[fortressTuples.size()];
		for (int i = 0; i < fortressTuples.size(); i++) {
			Object[] tuple = fortressTuples.get(i);
			fortresses[i] = new Fortress((int) tuple[0], (int) tuple[1], (int) tuple[2], (boolean) tuple[3]);
		}
	}

	public void updateResources() throws InterruptedException {
        List<Object[]> tuples = resourceSpace.queryAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class));
        resources = new Resource[tuples.size()];
        for (int i = 0; i < tuples.size(); i++) {
            Object[] tuple = tuples.get(i);
            resources[i] = new Resource((int) tuple[0], (int) tuple[1], (int) tuple[2]);
        }
	}
	
	public void updateOrbs() throws InterruptedException {
        List<Object[]> orbTuples = orbSpace.queryAll(new FormalField(Integer.class), new FormalField(Integer.class));
        orbs = new Orb[orbTuples.size()];
        for (int i = 0; i < orbTuples.size(); i++) {
            Object[] tuple = orbTuples.get(i);
            orbs[i] = new Orb((int) tuple[0], (int) tuple[1]);
        }
        List<Object[]> orbHolderTuples = orbSpace.queryAll(new FormalField(Boolean.class), new FormalField(Boolean.class), new FormalField(Boolean.class));
        orbHolders = new OrbHolder[orbHolderTuples.size()];
        for (int i = 0; i < orbHolderTuples.size(); i++) {
            Object[] tuple = orbHolderTuples.get(i);
            orbHolders[i] = new OrbHolder((boolean) tuple[0], (boolean) tuple[1], (boolean) tuple[2]);
        }
	}

	public class GamePanel extends JPanel implements KeyListener {
		public Graphics2D g2D;
		private int numberOfDisconnectedClients = 0;

		public GamePanel() {
			setPreferredSize(new Dimension(Server.SCREEN_WIDTH, Server.SCREEN_HEIGHT));
			addKeyListener(this);
	        setFocusable(true);
		}

		public void paint(Graphics g) {
			super.paint(g);
			g2D = (Graphics2D) g;
			if (gameOver) {
				g2D.setFont(new Font(defaultFont, Font.PLAIN, 20));
				g2D.drawString("Team "+winningTeam+" has won!", 500, 250);
				g2D.drawString("Restarting...", 500, 300);
			}
			else if (gameStarted) {
				// Render each object on the screen
				paintFortresses();
				paintResources();
				paintOrbs();
				paintCannons();
				paintWalls();
				paintPlayers();
				paintBullets();
				for (int i = 0; i < numberOfDisconnectedClients; i++) {
					g2D.setFont(new Font(defaultFont, Font.PLAIN, 15));
					g2D.drawString("A player has disconnected.", Server.SCREEN_WIDTH-250, 50+i*20);
				}
			}
			else {
				g2D.setFont(new Font(defaultFont, Font.PLAIN, 20));
				g2D.drawString("Waiting for one more player to join...", 500, 300);
			}
		}

		public void paintPlayers(){
			g2D.setFont(new Font(defaultFont, Font.PLAIN, 12));
			for (int i = 0; i < players.length; i++) {
				Player p = players[i];
				if(p.team){
					g2D.drawImage(manred, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
				} else {
					g2D.drawImage(manblue, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
				}
				//g2D.drawRect((int) p.x, (int) p.y, (int) p.width, (int) p.height);
				g2D.drawString("Wood: " + p.wood, (int)p.x+20, (int)p.y - 30);
				g2D.drawString("Iron: " + p.iron, (int)p.x+20, (int)p.y - 10);
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
			for (Bullet b : bullets) {
				if(b.getTeam()){
					g2D.drawImage(bulletred, (int) b.x, (int) b.y, (int) b.width, (int) b.height, null);
				} else {
					g2D.drawImage(bulletblue, (int) b.x, (int) b.y, (int) b.width, (int) b.height, null);
				}
			}
		}

		public void paintWalls(){
			for (Wall w : walls) {
				/*
				if(w.getTeam()){
					g2D.drawImage(wallred, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
				} else {
					g2D.drawImage(wallblue, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
				}
				 */
				g2D.drawRect((int) w.x, (int) w.y, (int) w.width, (int) w.height);
			}
		}

		public void paintFortresses(){
			g2D.setFont(fortressStatusFont);
			for (Fortress f : fortresses) {
				if (f.getTeam()) {
					g2D.drawImage(fortressred, (int) f.x, (int) f.y, (int) f.width, (int) f.height, null);
					g2D.drawString("" + f.getHP(), (int) f.x + 30, (int) f.y + 202);
					g2D.drawString("" + f.getWood(), (int) f.x + 30, (int) f.y + 302);
					g2D.drawString("" + f.getIron(), (int) f.x + 30, (int) f.y + 402);
				} else {
					g2D.drawImage(fortressblue, (int) f.x, (int) f.y, (int) f.width, (int) f.height, null);
					g2D.drawString("" + f.getHP(), (int) f.x + 80, (int) f.y + 202);
					g2D.drawString("" + f.getWood(), (int) f.x + 80, (int) f.y + 302);
					g2D.drawString("" + f.getIron(), (int) f.x + 80, (int) f.y + 402);
				}
			}
		}

		public void paintResources(){
            for (Resource r : resources) {
                if (r.getType() == 0) {
                    g2D.drawImage(wood, (int) r.x, (int) r.y, (int) r.width, (int) r.height, null);
                }
                else {
					g2D.drawImage(iron, (int) r.x, (int) r.y, (int) r.width, (int) r.height, null);
                }
            }
		}
		
		public void paintOrbs(){
			for (Orb o : orbs) {
                g2D.setColor(Color.BLUE);
                g2D.drawRect((int) o.x, (int) o.y, (int) o.width, (int) o.height);
                g2D.setColor(Color.BLACK);
            }
			for (OrbHolder oh : orbHolders) {
                g2D.drawRect((int) oh.x, (int) oh.y, (int) oh.width, (int) oh.height);
                if (oh.hasOrb) {
                    g2D.drawRect((int) oh.x+3, (int) oh.y+3, (int) oh.width-6, (int) oh.height-6);
                }
            }
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
					case "createwall":
						if(!createWallKeyDown && wallSpace.queryp(new ActualField(id), new ActualField(input)) == null){
							wallSpace.put(id, input);
						}
						createWallKeyDown = true;
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
					case "createwall":
						createWallKeyDown = false;
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
				case KeyEvent.VK_E:
					input = "createwall";
					break;
				default:
					break;
			}
			return input;
		}
		
		public void clientDisconnected() {
			new Thread(new ShowPlayerDisconnected()).start();
		}
		
		private class ShowPlayerDisconnected implements Runnable {
			public void run() {
				numberOfDisconnectedClients++;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				numberOfDisconnectedClients--;
			}
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
	
	private class ServerCheckReader implements Runnable {
		public void run() {
			try {
				while (true) {
					String msg = (String) channelFromServer.get(new FormalField(String.class))[0];
					if (msg.equals("check") && !windowClosed) {
						channelToServer.put("acknowledged");
					}
					else if (msg.equals("stop") && !frame.isHost) {
						System.exit(0);
					}
					else if (msg.equals("clientdisconnected")) {
						panel.clientDisconnected();
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}