package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
		player.team = getNextTeam();
    	double[] pos = getRandomPlayerPosition(player.team);
    	player.x = pos[0];
    	player.y = pos[1];
    }

    public void addPlayer(int id, String name) {
    	boolean team = getNextTeam();
    	double[] pos = getRandomPlayerPosition(team);
    	s.getPlayers().add(new Player(pos[0], pos[1], id, team, name));
    }
    
    public boolean getNextTeam() {
        if (s.getNumPlayers(true) == s.getNumPlayers(false)) {
            int team = (new Random()).nextInt(2);
            if (team == 0) {
            	return true;
            }
            else {
            	return false;
            }
        }
        else if (s.getNumPlayers(true) > s.getNumPlayers(false)) {
        	return false;
        }
        else {
        	return true;
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

    public void updatePlayers() throws InterruptedException {
        List<Object[]> movementTuples = s.getPlayerMovementSpace().queryAll(new FormalField(Integer.class), new FormalField(String.class));
        int[][] movementVectors = new int[s.playerIDCounter][2];
        for (Object[] movementTuple : movementTuples) {
            int playerID = (Integer) movementTuple[0];
            Player player = s.getPlayerWithID(playerID);
            if (player == null) {
            	continue;
            }
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
            Player player = s.getPlayerWithID(i);
            if (player == null) {
            	continue;
            }
            double oldX = player.x;
            double oldY = player.y;
            double mvLength = Math.sqrt(movementVectors[i][0]*movementVectors[i][0] + movementVectors[i][1]*movementVectors[i][1]);
            if (mvLength != 0) {
                double speed = Player.SPEED * s.S_BETWEEN_UPDATES;
                if (s.getBuffController().isGhost(player)) {
                    speed *= 2;
                }
                if (player.hasOrb) {
                	speed *= 0.8;
                }
                player.x += (movementVectors[i][0] / mvLength) * speed;
                player.y += (movementVectors[i][1] / mvLength) * speed;
            }

            // Prevent collision
            if(isColliding(player) && !isColliding(new Player(player.x, oldY, player.id, player.team, player.name))){
                player.y = oldY;
            } else if(isColliding(player) && !isColliding(new Player(oldX, player.y, player.id, player.team, player.name))){
                player.x = oldX;
            } else if(isColliding(player) && !isColliding(new Player(oldX, oldY, player.id, player.team, player.name))) {
                player.x = oldX;
                player.y = oldY;
            }
        }

        // Prevent player from  going out of bounds
        for (Player p : s.getPlayers()) {
            if(p.x < 0){
                p.x = 0;
            }
            if(p.x > s.SCREEN_WIDTH - Player.WIDTH){
                p.x = s.SCREEN_WIDTH - Player.WIDTH;
            }
            if(p.y < 0){
                p.y = 0;
            }
            if(p.y > s.SCREEN_HEIGHT - Player.HEIGHT){
                p.y = s.SCREEN_HEIGHT - Player.HEIGHT;
            }
        }

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
                p.stunned -= Server.S_BETWEEN_UPDATES;
            }
        }
        s.getPlayerPositionsSpace().put("players");
    }

    public boolean isColliding(Player player) {
        return (!s.getBuffController().isGhost(player) && s.getWalls().stream().anyMatch(w -> w.getTeam() != player.team && w.intersects(player)) ||
                (player.team && s.getFortress1().intersects(player)) ||
                (!player.team && s.getFortress2().intersects(player)));
    }
}
