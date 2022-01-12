package controller;

import game.Server;
import model.Bullet;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class PlayerController {
    Server server;

    public PlayerController(Server server){
        this.server = server;
    }

    public void updatePlayers() throws InterruptedException {
        List<Object[]> movementTuples = server.playerMovementSpace.queryAll(new FormalField(Integer.class), new FormalField(String.class));
        int[][] movementVectors = new int[server.players.size()][2];
        for (Object[] movementTuple : movementTuples) {
            int playerID = (Integer) movementTuple[0];
            Player player = server.players.get(playerID);
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
            Player player = server.players.get(i);
            double oldX = player.x;
            double oldY = player.y;
            double mvLength = Math.sqrt(movementVectors[i][0]*movementVectors[i][0] + movementVectors[i][1]*movementVectors[i][1]);
            if (mvLength != 0) {
                double speed = Player.SPEED * server.S_BETWEEN_UPDATES;
                if (server.isGhost(player)) {
                    speed *= 2;
                }
                player.x += (movementVectors[i][0] / mvLength) * speed;
                player.y += (movementVectors[i][1] / mvLength) * speed;
            }

            // Prevent collision
            if(server.isColliding(player) && !server.isColliding(new Player(oldX, oldY, player.id, player.team))){
                player.x = oldX;
                player.y = oldY;
            }
        }

        // Prevent player from  going out of bounds
        for (Player p : server.players) {
            if(p.x < 0){
                p.x = 0;
            }
            if(p.x > server.SCREEN_WIDTH - Player.WIDTH){
                p.x = server.SCREEN_WIDTH - Player.WIDTH;
            }
            if(p.y < 0){
                p.y = 0;
            }
            if(p.y > server.SCREEN_HEIGHT - Player.HEIGHT){
                p.y = server.SCREEN_HEIGHT - Player.HEIGHT;
            }
        }

        server.playerPositionsSpace.getp(new ActualField("players"));
        server.playerPositionsSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
        for (Player p : server.players) {
            if (!p.disconnected) {
                server.mutexSpace.get(new ActualField("bulletsLock"));
                for (Bullet b : server.bullets) {
                    if (!server.isGhost(p) && b.getTeam() != p.team && b.intersects(p)) {
                        p.stunned = 0.5;
                    }
                }
                server.bullets.removeIf(b -> !server.isGhost(p) && b.getTeam() != p.team && b.intersects(p));
                server.mutexSpace.put("bulletsLock");
                server.playerPositionsSpace.put(p.x, p.y, p.id, p.team, p.wood, p.iron, p.hasOrb);
                if (p.stunned > 0) {
                    p.stunned -= server.S_BETWEEN_UPDATES;
                }
            }
        }
        server.playerPositionsSpace.put("players");
    }
}
