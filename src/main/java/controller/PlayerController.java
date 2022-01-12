package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlayerController {
    Server server;
    private Space playerPositionsSpace;
    private Space playerMovementSpace;
    private List<Player> players;

    public PlayerController(Server server){
        this.server = server;
        playerPositionsSpace = new SequentialSpace();
        playerMovementSpace = new SequentialSpace();
        players = new ArrayList<Player>();
    }

    public void initializePlayers(){
        boolean[] disconnected = new boolean[server.numPlayers];
        for (int i = 0; i < server.numPlayers; i++) {
            disconnected[i] = players.get(i).disconnected;
        }

        players = new ArrayList<Player>();
        for (int i = 0; i < server.numPlayers; i++) {
            addPlayer(i);
            players.get(i).disconnected = disconnected[i];
        }
        Collections.shuffle(players);
    }

    public void addPlayer(int id) {
        double randomY = (new Random()).nextInt((int)(Fortress.HEIGHT - Player.HEIGHT)) + ((Server.SCREEN_HEIGHT - Fortress.HEIGHT) / 2);
        double xOffset = Fortress.WIDTH + 20;

        if (server.numPlayersTeam1 == server.numPlayersTeam2) {
            int team = (new Random()).nextInt(2);
            if (team == 0) {
                players.add(new Player(Server.SCREEN_WIDTH - xOffset - Player.WIDTH, randomY, id, true));
                server.numPlayersTeam1++;
            }
            else {
                players.add(new Player(0 + xOffset, randomY, id, false));
                server.numPlayersTeam2++;
            }
        }
        else if (server.numPlayersTeam1 > server.numPlayersTeam2) {
            players.add(new Player(0 + xOffset, randomY, id, false));
            server.numPlayersTeam2++;
        }
        else {
            players.add(new Player(Server.SCREEN_WIDTH - xOffset - Player.WIDTH, randomY, id, true));
            server.numPlayersTeam1++;
        }
    }

    public void updatePlayers() throws InterruptedException {
        List<Object[]> movementTuples = playerMovementSpace.queryAll(new FormalField(Integer.class), new FormalField(String.class));
        int[][] movementVectors = new int[players.size()][2];
        for (Object[] movementTuple : movementTuples) {
            int playerID = (Integer) movementTuple[0];
            Player player = players.get(playerID);
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
            Player player = players.get(i);
            double oldX = player.x;
            double oldY = player.y;
            double mvLength = Math.sqrt(movementVectors[i][0]*movementVectors[i][0] + movementVectors[i][1]*movementVectors[i][1]);
            if (mvLength != 0) {
                double speed = Player.SPEED * server.S_BETWEEN_UPDATES;
                if (server.getBuffController().isGhost(player)) {
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
        for (Player p : players) {
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

        playerPositionsSpace.getp(new ActualField("players"));
        playerPositionsSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Integer.class), new FormalField(Boolean.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));
        for (Player p : players) {
            if (!p.disconnected) {
                server.getMutexSpace().get(new ActualField("bulletsLock"));
                for (Bullet b : server.getCannonController().getBullets()) {
                    if (!server.getBuffController().isGhost(p) && b.getTeam() != p.team && b.intersects(p)) {
                        p.stunned = 0.5;
                    }
                }
                server.getCannonController().getBullets().removeIf(b -> !server.getBuffController().isGhost(p) && b.getTeam() != p.team && b.intersects(p));
                server.getMutexSpace().put("bulletsLock");
                playerPositionsSpace.put(p.x, p.y, p.id, p.team, p.wood, p.iron, p.hasOrb);
                if (p.stunned > 0) {
                    p.stunned -= server.S_BETWEEN_UPDATES;
                }
            }
        }
        playerPositionsSpace.put("players");
    }

    public boolean isColliding(Player player) {
        return (!server.getBuffController().isGhost(player) && server.getWallController().getWalls().stream().anyMatch(w -> w.getTeam() != player.team && w.intersects(player)) ||
                (player.team && server.getFortressController().getFortress1().intersects(player)) ||
                (!player.team && server.getFortressController().getFortress2().intersects(player)));
    }

    public Space getPlayerMovementSpace() {
        return playerMovementSpace;
    }

    public Space getPlayerPositionsSpace() {
        return playerPositionsSpace;
    }

    public List<Player> getPlayers() {
        return players;
    }
}
