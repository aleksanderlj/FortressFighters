package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

public class FortressController {
    Server server;

    public FortressController(Server server){
        this.server = server;
    }

    public void updateFortresses() throws InterruptedException {
        if (server.fortress1 == null) { return; }

        boolean changed = false;

        // Increase resources if player collides with fortress when holding resources
        for (Player p : server.players) {
            if (p.wood == 0 && p.iron == 0) { continue; }

            if (p.team && p.intersects(server.fortress2)) {
                server.fortress2.setWood(server.fortress2.getWood() + p.wood);
                server.fortress2.setIron(server.fortress2.getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            } else if (!p.team && p.intersects(server.fortress1)) {
                server.fortress1.setWood(server.fortress1.getWood() + p.wood);
                server.fortress1.setIron(server.fortress1.getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            }
        }

        // Prevent player from going through enemy fortress
        for (Player p : server.players) {
            if (!p.team && p.intersects(server.fortress2)) {
                // TODO Move blue player out of red fortress
            } else if (p.team && p.intersects(server.fortress1)) {
                // TODO Move red player out of blue fortress
            }
        }

        // Reduce HP of fortress if bullet or cannon collides with it
        server.mutexSpace.get(new ActualField("bulletsLock"));
        for (Bullet b : server.bullets) {
            if (b.getTeam() && b.intersects(server.fortress1)) {
                server.fortress1.setHP(server.fortress1.getHP() - 5);
                changed = true;
            } else if (!b.getTeam() && b.intersects(server.fortress2)) {
                server.fortress2.setHP(server.fortress2.getHP() - 5);
                changed = true;
            }
        }
        // Remove bullets that hit fortress
        server.bullets.removeIf(b -> (b.intersects(server.fortress1) && b.getTeam()) || (b.intersects(server.fortress2) && !b.getTeam()));
        server.mutexSpace.put("bulletsLock");

        // Look for a winner
        if (server.fortress1.getHP() <= 0) {
            server.gameOver(false);
        } else if (server.fortress2.getHP() <= 0) {
            server.gameOver(true);
        }

        if (changed) { changeFortress(); }
    }

    public void changeFortress() {
        try {
            server.fortressSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));

            // Update fortresses if they exist, otherwise build two new ones
            if (server.fortress1 != null) {
                server.fortressSpace.put(server.fortress1.getWood(), server.fortress1.getIron(), server.fortress1.getHP(), false);
                server.fortressSpace.put(server.fortress2.getWood(), server.fortress2.getIron(), server.fortress2.getHP(), true);
            } else {
                server.fortress1 = new Fortress(false);
                server.fortress2 = new Fortress(true);
                server.fortressSpace.put(0, 0, 100, false);
                server.fortressSpace.put(0, 0, 100, true);
            }
        } catch (InterruptedException e) {}
    }
}
