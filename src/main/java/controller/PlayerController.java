package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;
import java.util.Random;

public class PlayerController {
    Server s;

    public PlayerController(Server server){
        this.s = server;
    }

    public void addPlayer(int id) {
        double randomY = (new Random()).nextInt((int)(Fortress.HEIGHT - Player.HEIGHT)) + ((Server.SCREEN_HEIGHT - Fortress.HEIGHT) / 2);
        double xOffset = Fortress.WIDTH + 20;

        if (s.numPlayersTeam1 == s.numPlayersTeam2) {
            int team = (new Random()).nextInt(2);
            if (team == 0) {
                s.getPlayers().add(new Player(Server.SCREEN_WIDTH - xOffset - Player.WIDTH, randomY, id, true));
                s.numPlayersTeam1++;
            }
            else {
                s.getPlayers().add(new Player(0 + xOffset, randomY, id, false));
                s.numPlayersTeam2++;
            }
        }
        else if (s.numPlayersTeam1 > s.numPlayersTeam2) {
            s.getPlayers().add(new Player(0 + xOffset, randomY, id, false));
            s.numPlayersTeam2++;
        }
        else {
            s.getPlayers().add(new Player(Server.SCREEN_WIDTH - xOffset - Player.WIDTH, randomY, id, true));
            s.numPlayersTeam1++;
        }
    }

    public void updatePlayers() throws InterruptedException {
        List<Object[]> movementTuples = s.getPlayerMovementSpace().queryAll(new FormalField(Integer.class), new FormalField(String.class));
        int[][] movementVectors = new int[s.getPlayers().size()][2];
        for (Object[] movementTuple : movementTuples) {
            int playerID = (Integer) movementTuple[0];
            Player player = s.getPlayers().get(playerID);
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
            Player player = s.getPlayers().get(i);
            double oldX = player.x;
            double oldY = player.y;
            double mvLength = Math.sqrt(movementVectors[i][0]*movementVectors[i][0] + movementVectors[i][1]*movementVectors[i][1]);
            if (mvLength != 0) {
                double speed = Player.SPEED * s.S_BETWEEN_UPDATES;
                if (s.getBuffController().isGhost(player)) {
                    speed *= 2;
                }
                player.x += (movementVectors[i][0] / mvLength) * speed;
                player.y += (movementVectors[i][1] / mvLength) * speed;
            }

            // Prevent collision
            if(isColliding(player) && !isColliding(new Player(oldX, oldY, player.id, player.team))){
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
        s.getPlayerPositionsSpace().getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
        for (Player p : s.getPlayers()) {
            if (!p.disconnected) {
                s.getMutexSpace().get(new ActualField("bulletsLock"));
                for (Bullet b : s.getBullets()) {
                    if (!s.getBuffController().isGhost(p) && b.getTeam() != p.team && b.intersects(p)) {
                        p.stunned = 0.5;
                    }
                }
                s.getBullets().removeIf(b -> !s.getBuffController().isGhost(p) && b.getTeam() != p.team && b.intersects(p));
                s.getMutexSpace().put("bulletsLock");
                s.getPlayerPositionsSpace().put(p.x, p.y, p.id, p.team, p.wood, p.iron, p.hasOrb);
                if (p.stunned > 0) {
                    p.stunned -= s.S_BETWEEN_UPDATES;
                }
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
