package controller;

import game.Server;
import model.Bullet;
import model.Player;
import model.Wall;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class WallController {
    Server s;

    public WallController(Server server){
        this.s = server;
    }

    public void initializeWalls() throws InterruptedException {
        s.getWalls().clear();
        s.getWallSpace().getAll(new FormalField(Integer.class), new ActualField(String.class));
        s.getWallSpace().getAll(new ActualField("wall"), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
    }

    public void updateWalls() throws InterruptedException{
        List<Object[]> wallCommands = s.getWallSpace().getAll(new FormalField(Integer.class), new FormalField(String.class));
        Wall newWall;
        for (Object[] command : wallCommands) {
            int id = (int) command[0];
            Player player = s.getPlayers().get(id);
            double wallXOffset = player.team ? -Wall.WIDTH : Player.WIDTH;
            newWall = new Wall(player.x + wallXOffset, (player.y+Player.HEIGHT/2) - Wall.HEIGHT/2, player.team);

            // Only build wall if it's not colliding with another cannon, wall, fortress
            if(
                    s.getCannons().stream().noneMatch(newWall::intersects) &&
                            s.getWalls().stream().noneMatch(newWall::intersects) &&
                            !newWall.intersects(s.getFortress1()) &&
                            !newWall.intersects(s.getFortress2()) &&
                            s.getPlayers().stream().filter(p -> p.team != player.team || p.disconnected).noneMatch(newWall::intersects)
            ){
                // Spend resources from fortress when building a wall
                if (!newWall.getTeam() && s.getFortress1().getWood() >= Wall.WOOD_COST) {
                    s.getFortress1().setWood(s.getFortress1().getWood() - Wall.WOOD_COST);
                    s.getFortressController().changeFortress();
                } else if (newWall.getTeam() && s.getFortress2().getWood() >= Wall.WOOD_COST) {
                    s.getFortress2().setWood(s.getFortress2().getWood() - Wall.WOOD_COST);
                    s.getFortressController().changeFortress();
                } else {
                    return;
                }
                s.getWalls().add(newWall);
                s.getWallSpace().put("wall", newWall.getId(), newWall.getHealth(), newWall.x, newWall.y, newWall.getTeam());
            }
        }

        // Prevent player from going through wall
        for (Player p : s.getPlayers()) {
            for (Wall w : s.getWalls()) {
                if(w.getTeam() != p.team && p.intersects(w)){
                    // TODO Move player out of wall (Minkowsky?)
                }
            }
        }

        // Reduce HP of wall if bullet or cannon collides with it
        s.getMutexSpace().get(new ActualField("bulletsLock"));
        for (Bullet b : s.getBullets()) {
            for (Wall w : s.getWalls()) {
                if(b.intersects(w) && b.getTeam() != w.getTeam()){
                    s.getWallSpace().getp(new ActualField("wall"), new ActualField(w.getId()), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
                    w.setHealth(w.getHealth() - 1);
                    if(w.getHealth() > 0) {
                        s.getWallSpace().put("wall", w.getId(), w.getHealth(), w.x, w.y, w.getTeam());
                    }
                }
            }
        }
        // Remove bullets that hit wall
        s.getBullets().removeIf(b -> s.getWalls().stream().anyMatch(w -> b.intersects(w) && b.getTeam() != w.getTeam()));
        s.getWalls().removeIf(w -> w.getHealth() <= 0);
        s.getMutexSpace().put("bulletsLock");
    }
}
