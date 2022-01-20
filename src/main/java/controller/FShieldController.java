package controller;

import game.Server;
import model.Bullet;
import model.Player;
import model.Wall;
import model.FShield;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class FShieldController {
    Server s;

    public FShieldController(Server server){
        this.s = server;
    }

    public void initializeFShield() throws InterruptedException{
        s.getfShields().clear();
        s.getfShieldSpace().getAll(new FormalField(Integer.class), new ActualField(String.class));
        s.getfShieldSpace().getAll(new ActualField("wall"), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
    }
    public void updateFShields() throws InterruptedException{
        List<Object[]> fShieldCommands = s.getfShieldSpace().getAll(new FormalField(Integer.class), new FormalField(String.class));
        Wall newWall;
        for (Object[] command : fShieldCommands) {
            int id = (int) command[0];
            Player player = s.getPlayerWithID(id);
            double wallXOffset = player.team ? -Wall.WIDTH : Player.WIDTH;
            FShield newFShield = new FShield(player.x + wallXOffset, (player.y+Player.HEIGHT/2) - Wall.HEIGHT/2, player.team);

            // Only build wall if it's not colliding with another cannon, wall, fortress
            /*if(
                    s.getCannons().stream().noneMatch(newFShield::intersects) &&
                            s.getWalls().stream().noneMatch(newFShield::intersects) &&
                            !newWall.intersects(s.getFortress1()) &&
                            !newWall.intersects(s.getFortress2()) &&
                            s.getPlayers().stream().filter(p -> p.team != player.team).noneMatch(newWall::intersects)
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
        }*/

        // Prevent player from going through FORCE SHIELD
        for (Player p : s.getPlayers()) {
            for (FShield FS : s.getfShields()) {
                if(FS.getTeam() != p.team && p.intersects(FS)){
                    // TODO Move player out of wall (Minkowsky?)
                }
            }
        }

        // Reduce HP of FORCE SHIELD if bullet or cannon collides with it
        s.getMutexSpace().get(new ActualField("bulletsLock"));
        for (Bullet b : s.getBullets()) {
            for (FShield FS : s.getfShields()) {
                if(b.intersects(FS) && b.getTeam() != FS.getTeam()){
                    s.getWallSpace().getp(new ActualField("wall"), new ActualField(FS.getId()), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
                    FS.setHealth(FS.getHealth() - 1);
                    if(FS.getHealth() > 0) {
                        s.getWallSpace().put("wall", FS.getId(), FS.getHealth(), FS.x, FS.y, FS.getTeam());
                    }
                }
            }
        }
        // Remove bullets that hit FORCE SHIELD
        s.getBullets().removeIf(b -> s.getfShields().stream().anyMatch(FS -> b.intersects(FS) && b.getTeam() != FS.getTeam()));
        s.getWalls().removeIf(FS -> FS.getHealth() <= 0);
        s.getMutexSpace().put("bulletsLock");
    }
}

}
