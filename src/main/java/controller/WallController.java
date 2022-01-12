package controller;

import game.Server;
import model.Bullet;
import model.Player;
import model.Wall;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class WallController {
    Server server;

    public WallController(Server server){
        this.server = server;
    }

    public void updateWalls() throws InterruptedException{
        List<Object[]> wallCommands = server.wallSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
        Wall newWall;
        for (Object[] command : wallCommands) {
            int id = (int) command[0];
            Player player = server.players.get(id);
            double wallXOffset = player.team ? -Wall.WIDTH : Player.WIDTH;
            newWall = new Wall(player.x + wallXOffset, (player.y+Player.HEIGHT/2) - Wall.HEIGHT/2, player.team);

            // Only build wall if it's not colliding with another cannon, wall, fortress
            if(
                    server.cannons.stream().noneMatch(newWall::intersects) &&
                            server.walls.stream().noneMatch(newWall::intersects) &&
                            !newWall.intersects(server.fortress1) &&
                            !newWall.intersects(server.fortress2) &&
                            server.players.stream().filter(p -> p.team != player.team).noneMatch(newWall::intersects)
            ){
                // Spend resources from fortress when building a wall
                if (!newWall.getTeam() && server.fortress1.getWood() >= Wall.WOOD_COST) {
                    server.fortress1.setWood(server.fortress1.getWood() - Wall.WOOD_COST);
                    server.fortressController.changeFortress();
                } else if (newWall.getTeam() && server.fortress2.getWood() >= Wall.WOOD_COST) {
                    server.fortress2.setWood(server.fortress2.getWood() - Wall.WOOD_COST);
                    server.fortressController.changeFortress();
                } else {
                    return;
                }
                server.walls.add(newWall);
                server.wallSpace.put("wall", newWall.getId(), newWall.x, newWall.y, newWall.getTeam());
            }
        }

        // Prevent player from going through wall
        for (Player p : server.players) {
            for (Wall w : server.walls) {
                if(w.getTeam() != p.team && p.intersects(w)){
                    // TODO Move player out of wall (Minkowsky?)
                }
            }
        }

        // Reduce HP of wall if bullet or cannon collides with it
        server.mutexSpace.get(new ActualField("bulletsLock"));
        for (Bullet b : server.bullets) {
            for (Wall w : server.walls) {
                if(b.intersects(w) && b.getTeam() != w.getTeam()){
                    server.wallSpace.getp(new ActualField("wall"), new ActualField(w.getId()), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
                    w.setHealth(w.getHealth() - 1);
                    if(w.getHealth() > 0) {
                        server.wallSpace.put("wall", w.getId(), w.x, w.y, w.getTeam());
                    }
                }
            }
        }
        // Remove bullets that hit wall
        server.bullets.removeIf(b -> server.walls.stream().anyMatch(w -> b.intersects(w) && b.getTeam() != w.getTeam()));
        server.walls.removeIf(w -> w.getHealth() <= 0);
        server.mutexSpace.put("bulletsLock");
    }
}
