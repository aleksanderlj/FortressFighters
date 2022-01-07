package game;
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
	private List<Player> players = new ArrayList<>();
	private List<Cannon> cannons = new ArrayList<>();
    private List<Resource> resources = new ArrayList<>();
	private List<Wall> walls = new ArrayList<>();
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
	private Space mutexSpace;
	private int numPlayers = 0; //Including disconnected players.
	private int numPlayersTeam1 = 0; //Excluding disconnected players.
	private int numPlayersTeam2 = 0; //Excluding disconnected players.
	private boolean gameStarted = false;
    private int numberOfResources = 10;
    boolean gameOver = false;

	public Server() {
		repository = new SpaceRepository();
		repository.addGate("tcp://localhost:9001/?keep");
		centralSpace = new SequentialSpace();
		playerPositionsSpace = new SequentialSpace();
		playerMovementSpace = new SequentialSpace();
		cannonSpace = new SequentialSpace();
		bulletSpace = new SequentialSpace();
		wallSpace = new SequentialSpace();
		fortressSpace = new SequentialSpace();
		resourceSpace = new SequentialSpace();
		mutexSpace = new SequentialSpace();
		repository.add("central", centralSpace);
		repository.add("playerpositions", playerPositionsSpace);
		repository.add("playermovement", playerMovementSpace);
		repository.add("cannon", cannonSpace);
		repository.add("bullet", bulletSpace);
		repository.add("wall", wallSpace);
		repository.add("fortress", fortressSpace);
		repository.add("resource", resourceSpace);
		new Thread(new Timer()).start();
		new Thread(new DisconnectChecker()).start();
		new Thread(new JoinedReader()).start();
		try {
			mutexSpace.put("bulletsLock");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
		walls = new ArrayList<Wall>();
		try {
			cannonSpace.getAll(new FormalField(Integer.class), new ActualField(String.class));
			cannonSpace.getAll(new ActualField("cannon"), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
			wallSpace.getAll(new FormalField(Integer.class), new ActualField(String.class));
			wallSpace.getAll(new ActualField("wall"), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		fortress1 = null;
		fortress2 = null;
		changeFortress();
        resources = new ArrayList<Resource>();
        for (int i = 0; i < numberOfResources; i++) {
            resources.add(createRandomResource());
        }
        resourcesChanged();
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
		playerPositionsSpace.getp(new ActualField("players"));
		playerPositionsSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class));
		for (Player p : players) {
			if (!p.disconnected) {
				playerPositionsSpace.put(p.x, p.y, p.id, p.team, p.wood, p.iron);
			}
		}
		playerPositionsSpace.put("players");
	}

	public void updateCannons() throws InterruptedException {
		List<Object[]> cannonCommands = cannonSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
		Cannon newCannon;
		for (Object[] command : cannonCommands) {
			int id = (int) command[0];
			Player player = players.get(id);
			newCannon = new Cannon(player.x + player.width / 4, player.y + player.height / 2, player.team);

			// Only build cannon if it's not colliding with another cannon
			// TODO collision with: Wall, Fortress, Resource
			if(cannons.stream().noneMatch(newCannon::intersects)){
				// Can't place cannon on top of fortress
				if (newCannon.intersects(fortress1) || newCannon.intersects(fortress2)) { return; }
				
				// Spend resources from fortress when building a cannon
				if (!newCannon.getTeam() && fortress1.getIron() >= Cannon.IRON_COST) {
					fortress1.setIron(fortress1.getIron() - Cannon.IRON_COST);
					changeFortress();
				} else if (newCannon.getTeam() && fortress2.getIron() >= Cannon.IRON_COST) {
					fortress2.setIron(fortress2.getIron() - Cannon.IRON_COST);
					changeFortress();
				} else {
					return;
				}
				
				cannons.add(newCannon);
				cannonSpace.put("cannon", newCannon.x, newCannon.y, newCannon.getTeam());
				new Thread(new CannonShooter(newCannon)).start(); // TODO Need some way to stop and remove this when game is reset or cannon is destroyed
			}
		}
	}

	public void updateBullets() throws InterruptedException{
		bulletSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
		mutexSpace.get(new ActualField("bulletsLock"));
		bullets.removeIf(b -> b.x < 0 || b.x > SCREEN_WIDTH); // Remove bullets that are out of bounds
		for (Bullet b : bullets) {
			if(b.getTeam()){
				b.x -= Bullet.SPEED * S_BETWEEN_UPDATES;
			} else {
				b.x += Bullet.SPEED * S_BETWEEN_UPDATES;
			}
			bulletSpace.put(b.x, b.y, b.getTeam());
		}
		mutexSpace.put("bulletsLock");
	}

	public void updateWalls() throws InterruptedException{
		List<Object[]> wallCommands = wallSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
		Wall newWall;
		for (Object[] command : wallCommands) {
			int id = (int) command[0];
			Player player = players.get(id);
			double wallXOffset = player.team ? -Wall.WIDTH : Player.WIDTH;
			newWall = new Wall(player.x + wallXOffset, (player.y+Player.HEIGHT/2) - Wall.HEIGHT/2, player.team);

			// Only build wall if it's not colliding with another wall
			// TODO collision with: Cannon, Fortress, Resource
			if(walls.stream().noneMatch(newWall::intersects)){
				// Spend resources from fortress when building a wall
				if (!newWall.getTeam() && fortress1.getWood() >= Wall.WOOD_COST) {
					fortress1.setWood(fortress1.getWood() - Wall.WOOD_COST);
					changeFortress();
				} else if (newWall.getTeam() && fortress2.getWood() >= Wall.WOOD_COST) {
					fortress2.setWood(fortress2.getWood() - Wall.WOOD_COST);
					changeFortress();
				} else {
					return;
				}
				walls.add(newWall);
				wallSpace.put("wall", newWall.getId(), newWall.x, newWall.y, newWall.getTeam());
			}
		}

		// Reduce HP of wall if bullet or cannon collides with it
		mutexSpace.get(new ActualField("bulletsLock"));
		for (Bullet b : bullets) {
			for (Wall w : walls) {
				if(b.intersects(w) && b.getTeam() != w.getTeam()){
					wallSpace.getp(new ActualField("wall"), new ActualField(w.getId()), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
					w.setHealth(w.getHealth() - 1);
					if(w.getHealth() > 0) {
						wallSpace.put("wall", w.getId(), w.x, w.y, w.getTeam());
					}
				}
			}
		}
		// Remove bullets that hit wall
		bullets.removeIf(b -> walls.stream().anyMatch(w -> b.intersects(w) && b.getTeam() != w.getTeam()));
		walls.removeIf(w -> w.getHealth() <= 0);
		mutexSpace.put("bulletsLock");
	}

	public void updateFortresses() throws InterruptedException {
		if (fortress1 == null) { return; }
		
		boolean changed = false;
		
		// Increase resources if player collides with fortress when holding resources
		for (Player p : players) {
			if (p.wood == 0 && p.iron == 0) { continue; }
			
			if (p.team && p.intersects(fortress2)) {
				fortress2.setWood(fortress2.getWood() + p.wood);
				fortress2.setIron(fortress2.getIron() + p.iron);
				p.wood = 0;
				p.iron = 0;
				changed = true;
			} else if (!p.team && p.intersects(fortress1)) {
				fortress1.setWood(fortress1.getWood() + p.wood);
				fortress1.setIron(fortress1.getIron() + p.iron);
				p.wood = 0;
				p.iron = 0;
				changed = true;
			}
		}
		
		// Reduce HP of fortress if bullet or cannon collides with it
		mutexSpace.get(new ActualField("bulletsLock"));
		for (Bullet b : bullets) {
			if (b.getTeam() && b.intersects(fortress1)) {
				fortress1.setHP(fortress1.getHP() - 5);
				if (fortress1.getHP() <= 0) {
					gameOver(false);
				}
				changed = true;
			} else if (!b.getTeam() && b.intersects(fortress2)) {
				fortress2.setHP(fortress2.getHP() - 5);
				if (fortress2.getHP() <= 0) {
					gameOver(true);
				}
				changed = true;
			}
		}
		// Remove bullets that hit fortress
		bullets.removeIf(b -> (b.intersects(fortress1) && b.getTeam()) || (b.intersects(fortress2) && !b.getTeam()));
		mutexSpace.put("bulletsLock");
		
		if (changed) { changeFortress(); }
	}
	
	public void changeFortress() {
		try {
			fortressSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
			
			// Update fortresses if they exist, otherwise build two new ones
			if (fortress1 != null) {
				fortressSpace.put(fortress1.getWood(), fortress1.getIron(), fortress1.getHP(), false);
				fortressSpace.put(fortress2.getWood(), fortress2.getIron(), fortress2.getHP(), true);
			} else {
				fortress1 = new Fortress(false);
				fortress2 = new Fortress(true);
				fortressSpace.put(0, 0, 100, false);
				fortressSpace.put(0, 0, 100, true);
			}
		} catch (InterruptedException e) {}
	}
	
	public void gameOver(boolean winningTeam) {
		try {
			centralSpace.put("game over", winningTeam ? "blue" : "red");
			gameOver = true;
			Thread.sleep(5000);
			gameOver = false;
			centralSpace.get(new ActualField("game over"), new FormalField(String.class));
			startGame();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

    public void updateResources() {
        List<Resource> newResources = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            boolean add = true;
            Resource r = resources.get(i);
            for (int j = 0; j < players.size(); j++) {
                Player p = players.get(j);
                if (!p.disconnected && p.intersects(r)) {
                    add = false;
                    if (r.getType() == 0) {
                        p.wood++;
                    }
                    else {
                        p.iron++;
                    }
                    break;
                }
            }
            if (add) {
                newResources.add(r);
            }
        }
        boolean update = resources.size() != newResources.size();
        for (int i = newResources.size(); i < resources.size(); i++) {
            newResources.add(createRandomResource());
        }
        resources = newResources;
        if (update) {
            resourcesChanged();
        }
    }
    
    public void resourcesChanged() {
        try {
            resourceSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class));
            for (Resource r : resources) {
                resourceSpace.put((int)r.x, (int)r.y, r.getType());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public Resource createRandomResource() {
        Random r = new Random();
        int x = r.nextInt((int)(SCREEN_WIDTH-2*Fortress.WIDTH-2*Resource.WIDTH))+(int)Fortress.WIDTH+(int)Resource.WIDTH;
        int y = r.nextInt((int)(SCREEN_HEIGHT-2*Resource.WIDTH))+(int)Resource.WIDTH;
        int type = r.nextInt(2);
        return new Resource(x, y, type);
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
					if (!gameOver) {
						Thread.sleep((long)(S_BETWEEN_UPDATES*1000));
						update();	
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class JoinedReader implements Runnable {
		
		public void run() {
			try {
				while (true) {
					centralSpace.get(new ActualField("joined"));
					createNewChannel(numPlayers);
					addPlayer(numPlayers);
					numPlayers++;
					System.out.println("Player joined.");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
							bullet = new Bullet(cannon.x + Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
						} else {
							bullet = new Bullet(cannon.x + Cannon.WIDTH - Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
						}
						mutexSpace.get(new ActualField("bulletsLock"));
						bullets.add(bullet);
						mutexSpace.put("bulletsLock");
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