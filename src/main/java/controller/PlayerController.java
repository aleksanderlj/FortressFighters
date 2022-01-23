package controller;

import game.Server;
import game.Settings;
import model.Bullet;
import model.Fortress;
import model.Player;
import model.Wall;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import java.util.*;

public class PlayerController {
    Server s;

    public PlayerController(Server server){
        this.s = server;
    }

    public void initializePlayers(){
        Collections.shuffle(s.getPlayers());
        List<Player> tempPlayers = new ArrayList<>(s.getPlayers());
        s.getPlayers().clear();
        for (Player p : tempPlayers) {
            resetPlayer(p);
            s.getPlayers().add(p);
        }
    }
    
    public void resetPlayer(Player player) {
		player.hasOrb = false;
		player.wood = 0;
		player.iron = 0;
		player.stunned = 0;
        if(!Settings.fixedTeams) {
            player.team = getNextTeam(player.preferredTeam);
        }
    	double[] pos = getRandomPlayerPosition(player.team);
    	player.x = pos[0];
    	player.y = pos[1];
    }

    public void addPlayer(int id, String name, String preferred) {
    	boolean team = getNextTeam(preferred);
    	double[] pos = getRandomPlayerPosition(team);
    	s.getPlayers().add(new Player(pos[0], pos[1], id, team, name, preferred));
    }
    
    public boolean getNextTeam(String preferred) {
        if(!Settings.unevenTeams && s.getNumPlayers(true) != s.getNumPlayers(false)){
            return s.getNumPlayers(true) < s.getNumPlayers(false);
        } else {
            switch (preferred) {
                case "None":
                    return (new Random()).nextInt(2) == 0;
                case "Blue":
                    return false;
                case "Red":
                    return true;
                default:
                    return (new Random()).nextInt(2) == 0;
            }
        }
    }
    
    public double[] getRandomPlayerPosition(boolean team) {
    	double xOffset = Fortress.WIDTH + 20;
    	double y = (new Random()).nextInt((int)(Fortress.HEIGHT - Player.HEIGHT)) + ((Server.SCREEN_HEIGHT - Fortress.HEIGHT) / 2);
    	if (team) {
    		return new double[] {Server.SCREEN_WIDTH - xOffset - Player.WIDTH, y};
    	}
    	else {
    		return new double[] {xOffset, y};
    	}
    }

    public void updatePlayers(long deltaTime) throws InterruptedException {
        movePlayers(deltaTime,
                s.getPlayerMovementSpace().queryAll(new FormalField(Integer.class), new FormalField(String.class)),
                s.getPlayers(),
                s.getWalls(),
                s.getFortress1(),
                s.getFortress2(),
                s.getBuffController().getTeam1GhostTimer(),
                s.getBuffController().getTeam2GhostTimer());

        s.getPlayerPositionsSpace().getp(new ActualField("players"));
        s.getPlayerPositionsSpace().getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(String.class));
        for (Player p : s.getPlayers()) {
            s.getMutexSpace().get(new ActualField("bulletsLock"));
            for (Bullet b : s.getBullets()) {
                if (!s.getBuffController().isGhost(p) && b.getTeam() != p.team && b.intersects(p)) {
                    p.stunned = 0.5;
                }
            }
            s.getBullets().removeIf(b -> !s.getBuffController().isGhost(p) && b.getTeam() != p.team && b.intersects(p));
            s.getMutexSpace().put("bulletsLock");
            s.getPlayerPositionsSpace().put(p.x, p.y, p.id, p.team, p.wood, p.iron, p.hasOrb, p.name);
            if (p.stunned > 0) {
                p.stunned -= ((double)deltaTime)/1000;
            }
        }
        s.getPlayerPositionsSpace().put("players");
    }

    public static void movePlayers(long deltaTime,
                                   List<Object[]> movementTuples,
                                   List<Player> players,
                                   List<Wall> walls,
                                   Fortress f1,
                                   Fortress f2,
                                   double team1GhostTimer,
                                   double team2GhostTimer)
    {
        int[][] movementVectors = new int[players.size()][2];
        for (Object[] movementTuple : movementTuples) {
            int playerID = (Integer) movementTuple[0];
            Optional<Player> optionalPlayer = players.stream().filter(p -> p.id == playerID).findFirst();
            if (!optionalPlayer.isPresent()) {
                continue;
            }
            Player player = optionalPlayer.get();
            String direction = (String) movementTuple[1];
            if (player.stunned <= 0) {
                switch (direction) {
                    case "left":
                        movementVectors[playerID][0] -= 1;
                        break;
                    case "right":
                        movementVectors[playerID][0] += 1;
                        break;
                    case "down":
                        movementVectors[playerID][1] += 1;
                        break;
                    case "up":
                        movementVectors[playerID][1] -= 1;
                        break;
                    default:
                        break;
                }
            }
        }

        for (int i = 0; i < movementVectors.length; i++) {
            int finalI = i;
            Optional<Player> optionalPlayer = players.stream().filter(p -> p.id == finalI).findFirst();
            if (!optionalPlayer.isPresent()) {
                continue;
            }
            Player player = optionalPlayer.get();
            double oldX = player.x;
            double oldY = player.y;
            double mvLength = Math.sqrt(movementVectors[i][0]*movementVectors[i][0] + movementVectors[i][1]*movementVectors[i][1]);
            if (mvLength != 0) {
                double speed = Player.SPEED * ((double)deltaTime)/1000;
                if (BuffController.isGhost(player, team1GhostTimer, team2GhostTimer)) {
                    speed *= 2;
                }
                if (player.hasOrb) {
                    speed *= 0.8;
                }
                player.x += (movementVectors[i][0] / mvLength) * speed;
                player.y += (movementVectors[i][1] / mvLength) * speed;
            }

            // Prevent collision
            if(isColliding(player, walls, f1, f2, team1GhostTimer, team2GhostTimer) && !isColliding(new Player(player.x, oldY, player.id, player.team, player.name), walls, f1, f2, team1GhostTimer, team2GhostTimer)){
                player.y = oldY;
            } else if(isColliding(player, walls, f1, f2, team1GhostTimer, team2GhostTimer) && !isColliding(new Player(oldX, player.y, player.id, player.team, player.name), walls, f1, f2, team1GhostTimer, team2GhostTimer)){
                player.x = oldX;
            } else if(isColliding(player, walls, f1, f2, team1GhostTimer, team2GhostTimer) && !isColliding(new Player(oldX, oldY, player.id, player.team, player.name), walls, f1, f2, team1GhostTimer, team2GhostTimer)) {
                player.x = oldX;
                player.y = oldY;
            }
        }

        // Prevent player from  going out of bounds
        for (Player p : players) {
            if(p.x < 0){
                p.x = 0;
            }
            if(p.x > Server.SCREEN_WIDTH - Player.WIDTH){
                p.x = Server.SCREEN_WIDTH - Player.WIDTH;
            }
            if(p.y < 0){
                p.y = 0;
            }
            if(p.y > Server.SCREEN_HEIGHT - Player.HEIGHT){
                p.y = Server.SCREEN_HEIGHT - Player.HEIGHT;
            }
        }
    }

    public boolean isColliding(Player player) {
        return (!s.getBuffController().isGhost(player) && s.getWalls().stream().anyMatch(w -> w.getTeam() != player.team && w.intersects(player)) ||
                (player.team && s.getFortress1().intersects(player)) ||
                (!player.team && s.getFortress2().intersects(player)));
    }

    public static boolean isColliding(Player player, List<Wall> walls, Fortress f1, Fortress f2, double team1GhostTimer, double team2GhostTimer) {
        return (!BuffController.isGhost(player, team1GhostTimer, team2GhostTimer) && walls.stream().anyMatch(w -> w.getTeam() != player.team && w.intersects(player)) ||
                (player.team && f1.intersects(player)) ||
                (!player.team && f2.intersects(player)));
    }
}
