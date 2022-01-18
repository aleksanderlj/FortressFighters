package game;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
	private String address;
	private Player[] players = new Player[0];
	private Cannon[] cannons = new Cannon[0];
	private Wall[] walls = new Wall[0];
	private Bullet[] bullets = new Bullet[0];
	private Fortress[] fortresses = new Fortress[0];
    private Resource[] resources = new Resource[0];
    private Orb[] orbs = new Orb[0];
    private OrbHolder[] orbHolders = new OrbHolder[0];
	private RemoteSpace centralSpace;
	private RemoteSpace playerPositionsSpace;
	private RemoteSpace playerMovementSpace;
	private RemoteSpace cannonSpace;
	private RemoteSpace bulletSpace;
	private RemoteSpace wallSpace;
	private RemoteSpace fortressSpace;
	private RemoteSpace resourceSpace;
	private RemoteSpace orbSpace;
	private RemoteSpace channelFromServer;
	private RemoteSpace channelToServer;
	public static int id;
	private GamePanel panel;
	private boolean createCannonKeyDown = false;
	private boolean createWallKeyDown = false;
	private boolean dropOrbKeyDown = false;
	private BufferedImage manblue, manred,
			cannonblue, cannonred,
			fortressblue, fortressred,
			wood, iron, orb,
			bulletred, bulletblue,
			wallred1, wallred2, wallred3,
			wallblue1, wallblue2, wallblue3,
			orbholderempty, orbholderfull,
			manblueorb, manredorb,
			ironShadow, woodShadow, orbShadow,
			cannonblueShadow, cannonredShadow, wallShadow;
	private boolean gameStarted = false;
	private boolean gameOver = false;
	private boolean gamePaused = false;
	private boolean windowClosed = false;
	private String winningTeam = "";
	private String defaultFont;
	private Font fortressStatusFont;
	private String name;

	public Client(String address, GameFrame frame, String name) {
		this.frame = frame;
		this.name = name;
		frame.addWindowListener(new java.awt.event.WindowAdapter() {public void windowClosing(java.awt.event.WindowEvent windowEvent) {windowClosed = true;}});
		panel = new GamePanel();
		frame.setPanel(panel);
		frame.setVisible(true);
		defaultFont = "Comic Sans MS";
		try {
			centralSpace = new RemoteSpace("tcp://" + address + ":9001/central?keep");
			centralSpace.put("joined", name);
			id = (Integer)centralSpace.get(new FormalField(Integer.class))[0];
			connectToServer(address);
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
			orb = ImageIO.read(getClass().getClassLoader().getResource("orb.png"));
			wallred1 = ImageIO.read(getClass().getClassLoader().getResource("wallred1.png"));
			wallred2 = ImageIO.read(getClass().getClassLoader().getResource("wallred2.png"));
			wallred3 = ImageIO.read(getClass().getClassLoader().getResource("wallred3.png"));
			wallblue1 = ImageIO.read(getClass().getClassLoader().getResource("wallblue1.png"));
			wallblue2 = ImageIO.read(getClass().getClassLoader().getResource("wallblue2.png"));
			wallblue3 = ImageIO.read(getClass().getClassLoader().getResource("wallblue3.png"));
			orbholderempty = ImageIO.read(getClass().getClassLoader().getResource("orbholderempty.png"));
			orbholderfull = ImageIO.read(getClass().getClassLoader().getResource("orbholderfull.png"));
			manblueorb = ImageIO.read(getClass().getClassLoader().getResource("manblueorb.png"));
			manredorb = ImageIO.read(getClass().getClassLoader().getResource("manredorb.png"));
			ironShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_iron.png"));
			woodShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_wood.png"));
			orbShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_orb.png"));
			cannonblueShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_cannonblue.png"));
			cannonredShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_cannonred.png"));
			wallShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_wall.png"));
			fortressStatusFont = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("alagard.ttf"));
			fortressStatusFont = fortressStatusFont.deriveFont(Font.PLAIN, 36);
			checkGameStarted();
			new Thread(new Timer()).start();
			new Thread(new ServerCheckReader()).start();
			new Thread(new NewHostReader()).start();
		} catch (IOException | InterruptedException | FontFormatException e) {
			//e.printStackTrace();
		}
	}

	public void update() {
		try {
			if (gamePaused) {
				return;
			}
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
		} catch (InterruptedException e) {System.out.println("Interrupted");}
		panel.updatePanel();
	}
	
	private void connectToServer(String address) {
		this.address = address;
		try {
			centralSpace = new RemoteSpace("tcp://" + address + ":9001/central?keep");
			channelFromServer = new RemoteSpace("tcp://" + address + ":9001/serverclient"+id+"?keep");
			channelToServer = new RemoteSpace("tcp://" + address + ":9001/client"+id+"server?keep");
			playerPositionsSpace = new RemoteSpace("tcp://" + address + ":9001/playerpositions?keep");
			playerMovementSpace = new RemoteSpace("tcp://" + address + ":9001/playermovement?keep");
			cannonSpace = new RemoteSpace("tcp://" + address + ":9001/cannon?keep");
			bulletSpace = new RemoteSpace("tcp://" + address + ":9001/bullet?keep");
			wallSpace = new RemoteSpace("tcp://" + address + ":9001/wall?keep");
			fortressSpace = new RemoteSpace("tcp://" + address + ":9001/fortress?keep");
			resourceSpace = new RemoteSpace("tcp://" + address + ":9001/resource?keep");
			orbSpace = new RemoteSpace("tcp://" + address + ":9001/orb?keep");
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("Connection reset");
		}
	}
	
	private void disconnectFromServer() {
		try {
			centralSpace.close();
			channelFromServer.close();
			channelToServer.close();
			playerPositionsSpace.close();
			playerMovementSpace.close();
			cannonSpace.close();
			bulletSpace.close();
			wallSpace.close();
			fortressSpace.close();
			resourceSpace.close();
			orbSpace.close();
		} catch (IOException e) {
			System.out.println("Connection reset");
			//e.printStackTrace();
		}
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
			List<Object[]> playersTuples = playerPositionsSpace.queryAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(String.class));
			players = new Player[playersTuples.size()];
			for (int i = 0; i < playersTuples.size(); i++) {
				Object[] tuple = playersTuples.get(i);
				players[i] = new Player((double)tuple[0], (double)tuple[1], (int)tuple[2], (boolean)tuple[3], (String)tuple[7]);
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
		List<Object[]> wallTuples = wallSpace.queryAll(
				new ActualField("wall"),
				new FormalField(Integer.class),
				new FormalField(Integer.class),
				new FormalField(Double.class),
				new FormalField(Double.class),
				new FormalField(Boolean.class)
		);
		walls = new Wall[wallTuples.size()];
		for (int i = 0; i < wallTuples.size(); i++) {
			Object[] tuple = wallTuples.get(i);
			walls[i] = new Wall((int) tuple[1], (int) tuple[2], (double)tuple[3], (double)tuple[4], (boolean)tuple[5]);
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
		private List<String> disconnectedClients = new ArrayList<>();
		private List<BuffMessage> buffMessages = new ArrayList<>();

		public GamePanel() {
			setPreferredSize(new Dimension(Server.SCREEN_WIDTH, Server.SCREEN_HEIGHT));
			addKeyListener(this);
	        setFocusable(true);
			setBackground(new Color(241, 209, 141));
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
				paintBuffs();
				for (int i = 0; i < disconnectedClients.size(); i++) {
					g2D.setFont(new Font(defaultFont, Font.PLAIN, 15));
					String stringToShow = disconnectedClients.get(i).equals("") ? "A player" : disconnectedClients.get(i);
					g2D.drawString(stringToShow+" has disconnected.", Server.SCREEN_WIDTH-250, 40+i*20);
				}
				if (gamePaused) {
					g2D.setFont(new Font(defaultFont, Font.PLAIN, 30));
					g2D.drawString("Switching host...", Server.SCREEN_WIDTH/2-60, Server.SCREEN_HEIGHT/2);
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
					if(p.hasOrb){
						g2D.drawImage(manredorb, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
					} else {
						g2D.drawImage(manred, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
					}
				} else {
					if(p.hasOrb){
						g2D.drawImage(manblueorb, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
					} else {
						g2D.drawImage(manblue, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
					}
				}
				//g2D.drawRect((int) p.x, (int) p.y, (int) p.width, (int) p.height);
				if (p.id == id) {
					g2D.setFont(new Font(defaultFont, Font.BOLD, 12));
				}
				g2D.drawString(p.name, (int)((p.x+Player.WIDTH/2)-(g2D.getFontMetrics().stringWidth(p.name)/2)), (int)p.y - 5);
				int x1 = (int)p.x + 31;
				int y1 = (int)(p.y + 38);
				g2D.setColor(Color.WHITE);
				g2D.drawString("" + p.wood, x1, y1);
				g2D.drawString("" + p.iron, x1, y1 + 15);
				g2D.setColor(Color.BLACK);
				g2D.setFont(new Font(defaultFont, Font.PLAIN, 12));
			}
		}

		public void paintCannons(){
			for (Cannon c : cannons) {
				if(c.getTeam()){
					g2D.drawImage(cannonredShadow, (int) c.x-2, (int) c.y, (int) c.width+4, (int) c.height+4, null);
					g2D.drawImage(cannonred, (int) c.x, (int) c.y, (int) c.width, (int) c.height, null);
				} else {
					g2D.drawImage(cannonblueShadow, (int) c.x-2, (int) c.y, (int) c.width+4, (int) c.height+4, null);
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
				g2D.drawImage(wallShadow, (int) w.x-2, (int) w.y, (int) w.width+4, (int) w.height+4, null);
				if(w.getTeam()){
					if (w.getHealth() == Wall.MAX_HEALTH){
						g2D.drawImage(wallred1, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
					} else if(w.getHealth() >= 3){
						g2D.drawImage(wallred2, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
					} else if(w.getHealth() > 0){
						g2D.drawImage(wallred3, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
					}
				} else {
					if (w.getHealth() == Wall.MAX_HEALTH){
						g2D.drawImage(wallblue1, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
					} else if(w.getHealth() >= 3){
						g2D.drawImage(wallblue2, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
					} else if(w.getHealth() > 0){
						g2D.drawImage(wallblue3, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
					}
				}
			}
		}

		public void paintFortresses(){
			g2D.setFont(fortressStatusFont);
			for (Fortress f : fortresses) {
				if (f.getTeam()) {
					g2D.drawImage(fortressred, (int) f.x, (int) f.y, (int) f.width, (int) f.height, null);
					g2D.drawString("" + f.getHP(), (int) f.x + 30, (int) f.y + 217);
					g2D.drawString("" + f.getWood(), (int) f.x + 30, (int) f.y + 317);
					g2D.drawString("" + f.getIron(), (int) f.x + 30, (int) f.y + 417);
				} else {
					g2D.drawImage(fortressblue, (int) f.x, (int) f.y, (int) f.width, (int) f.height, null);
					g2D.drawString("" + f.getHP(), (int) f.x + 80, (int) f.y + 217);
					g2D.drawString("" + f.getWood(), (int) f.x + 80, (int) f.y + 317);
					g2D.drawString("" + f.getIron(), (int) f.x + 80, (int) f.y + 417);
				}
			}
		}

		public void paintResources(){
            for (Resource r : resources) {
                if (r.getType() == 0) {
					g2D.drawImage(woodShadow, (int) r.x-2, (int) r.y, 54, 54, null);
                    g2D.drawImage(wood, (int) r.x, (int) r.y, (int) r.width, (int) r.height, null);
                }
                else {
					g2D.drawImage(ironShadow, (int) r.x-2, (int) r.y, 54, 54, null);
					g2D.drawImage(iron, (int) r.x, (int) r.y, (int) r.width, (int) r.height, null);
                }
            }
		}
		
		public void paintOrbs(){
			for (Orb o : orbs) {
				g2D.drawImage(orbShadow, (int) o.x-2, (int) o.y, (int) o.width + 4, (int) o.height + 4, null);
				g2D.drawImage(orb, (int) o.x, (int) o.y, (int) o.width, (int) o.height, null);
            }
			for (OrbHolder oh : orbHolders) {
                if (oh.hasOrb) {
					g2D.drawImage(orbholderfull, (int) oh.x, (int) oh.y, (int) oh.width, (int) oh.height, null);
                } else {
					g2D.drawImage(orbholderempty, (int) oh.x, (int) oh.y, (int) oh.width, (int) oh.height, null);
				}
            }
		}

		private void paintBuffs(){
			try {
				Object[] msg = channelFromServer.getp(new ActualField("buff_activated"), new FormalField(String.class), new FormalField(Boolean.class));
				if(msg != null){
					buffMessages.add(new BuffMessage((String) msg[1], (boolean)msg[2]));
				}
				for (int i = 0 ; i < buffMessages.size() ; i++){
					buffMessages.get(i).update();
					g2D.setFont(new Font(defaultFont, Font.PLAIN, 18));
					String s = "Team " + (buffMessages.get(i).getTeam() ? "RED" : "BLUE") + " got " + buffMessages.get(i).buff.toUpperCase(Locale.ROOT) + "!";
					g2D.drawString(s, (Server.SCREEN_WIDTH/2) - (g2D.getFontMetrics().stringWidth(s)/2), Server.SCREEN_HEIGHT - 20 - (30 * i));
				}
				buffMessages.removeIf(b -> !b.isActive());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public void updatePanel() {
			repaint();
		}
		
		@Override
		public void keyTyped(KeyEvent e) {}
		@Override
		public void keyPressed(KeyEvent e) {
			if (gamePaused) {
				return;
			}
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
					case "dropOrb":
						if (!dropOrbKeyDown) {
							orbSpace.put(id, input);
						}
					default:
						break;
				}
			} catch (InterruptedException e1) {e1.printStackTrace();}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (gamePaused) {
				return;
			}
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
					case "dropOrb":
						dropOrbKeyDown = false;
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
				case KeyEvent.VK_SPACE:
					input = "dropOrb";
				default:
					break;
			}
			return input;
		}
		
		public void clientDisconnected(String playerName) {
			new Thread(new ShowPlayerDisconnected(playerName)).start();
		}
		
		private class ShowPlayerDisconnected implements Runnable {
			private String playerName;
			public ShowPlayerDisconnected(String playerName) {
				this.playerName = playerName;
			}
			public void run() {
				disconnectedClients.add(playerName);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				disconnectedClients.remove(playerName);
			}
		}

		private class BuffMessage {
			private String buff;
			private boolean team;
			private final static double BUFF_MSG_TIMER_MAX = 3;
			private double buffMessageTimer;

			public BuffMessage(String buff, boolean team){
				this.buff = buff;
				this.team = team;
				this.buffMessageTimer = BUFF_MSG_TIMER_MAX;
			}

			public void update(){
				buffMessageTimer -= S_BETWEEN_UPDATES;
			}

			public boolean isActive(){
				return buffMessageTimer > 0;
			}

			public String getBuff() {
				return buff;
			}

			public boolean getTeam(){
				return team;
			}
		}
	}

	private class Timer implements Runnable {
		public void run() {
			try {
				while (!gamePaused) {
					Thread.sleep((long)(S_BETWEEN_UPDATES*1000));
					update();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class NewHostReader implements Runnable {
		public void run() {
			try {
				while (true) {
					Object[] tuple = channelFromServer.get(new ActualField("newip"), new FormalField(String.class), new FormalField(String.class));
					String newAddress = (String)tuple[1];
					String disconnectedName = (String)tuple[2];
					gamePaused = true;
					disconnectFromServer();
					Thread.sleep(2000);
					connectToServer(newAddress);
					gamePaused = false;
					panel.clientDisconnected(disconnectedName);
					new Thread(new Timer()).start();
					new Thread(new ServerCheckReader()).start();
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
						String playerName = (String) channelFromServer.get(new FormalField(String.class))[0];
						panel.clientDisconnected(playerName);
					}
					else if (msg.equals("host")) {
						Server newServer = new Server(false);
						if (fortresses[0].getTeam()) {
							newServer.switchHostInitialize(address,
									new ArrayList<>(Arrays.asList(cannons)),
									new ArrayList<>(Arrays.asList(resources)),
									new ArrayList<>(Arrays.asList(orbs)),
									new ArrayList<>(Arrays.asList(orbHolders)),
									new ArrayList<>(Arrays.asList(bullets)),
									fortresses[1], fortresses[0]);
						}
						else {
							newServer.switchHostInitialize(address,
									new ArrayList<>(Arrays.asList(cannons)),
									new ArrayList<>(Arrays.asList(resources)),
									new ArrayList<>(Arrays.asList(orbs)),
									new ArrayList<>(Arrays.asList(orbHolders)),
									new ArrayList<>(Arrays.asList(bullets)),
									fortresses[0], fortresses[1]);
						}
						channelToServer.put("done", Server.getIP());
						Thread.sleep(1500);
						frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
						newServer.switchHostInitializeSpaces();
					}
					else if (msg.equals("closethread")) {
						return;
					}
				}
			} catch (InterruptedException e) {
				System.out.println("Interrupted");
				//e.printStackTrace();
			}
		}
	}

}