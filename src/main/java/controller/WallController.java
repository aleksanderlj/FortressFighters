package controller;

import game.Server;
import model.Bullet;
import model.Player;
import model.Wall;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.util.List;

public class WallController {
    Server server;
    private Space wallSpace;

    public WallController(Server server){
        this.server = server;
        wallSpace = new SequentialSpace();
    }

    public void updateWalls() throws InterruptedException{
        List<Object[]> wallCommands = wallSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
        Wall newWall;
        for (Object[] command : wallCommands) {
            int id = (int) command[0];
            Player player = server.getPlayers().get(id);
            double wallXOffset = player.team ? -Wall.WIDTH : Player.WIDTH;
            newWall = new Wall(player.x + wallXOffset, (player.y+Player.HEIGHT/2) - Wall.HEIGHT/2, player.team);

            // Only build wall if it's not colliding with another cannon, wall, fortress
            if(
                    server.getCannons().stream().noneMatch(newWall::intersects) &&
                            server.getWalls().stream().noneMatch(newWall::intersects) &&
                            !newWall.intersects(server.getFortress1()) &&
                            !newWall.intersects(server.getFortress2()) &&
                            server.getPlayers().stream().filter(p -> p.team != player.team).noneMatch(newWall::intersects)
            ){
                // Spend resources from fortress when building a wall
                if (!newWall.getTeam() && server.getFortress1().getWood() >= Wall.WOOD_COST) {
                    server.getFortress1().setWood(server.getFortress1().getWood() - Wall.WOOD_COST);
                    server.getFortressController().changeFortress();
                } else if (newWall.getTeam() && server.getFortress2().getWood() >= Wall.WOOD_COST) {
                    server.getFortress2().setWood(server.getFortress2().getWood() - Wall.WOOD_COST);
                    server.getFortressController().changeFortress();
                } else {
                    return;
                }
                server.getWalls().add(newWall);
                wallSpace.put("wall", newWall.getId(), newWall.x, newWall.y, newWall.getTeam());
            }
        }

        // Prevent player from going through wall
        for (Player p : server.getPlayers()) {
            for (Wall w : server.getWalls()) {
                if(w.getTeam() != p.team && p.intersects(w)){
                    // TODO Move player out of wall (Minkowsky?)
                }
            }
        }

        // Reduce HP of wall if bullet or cannon collides with it
        server.getMutexSpace().get(new ActualField("bulletsLock"));
        for (Bullet b : server.getBullets()) {
            for (Wall w : server.getWalls()) {
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
        server.getBullets().removeIf(b -> server.getWalls().stream().anyMatch(w -> b.intersects(w) && b.getTeam() != w.getTeam()));
        server.getWalls().removeIf(w -> w.getHealth() <= 0);
        server.getMutexSpace().put("bulletsLock");
    }

    public Space getWallSpace() {
        return wallSpace;
    }

    public void resetWallSpace() throws InterruptedException {
        wallSpace.getAll(new FormalField(Integer.class), new ActualField(String.class));
        wallSpace.getAll(new ActualField("wall"), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
    }
}
