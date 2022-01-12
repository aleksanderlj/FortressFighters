package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

public class FortressController {
    Server server;
    private Space fortressSpace;

    public FortressController(Server server){
        this.server = server;
        fortressSpace = new SequentialSpace();
    }

    public void updateFortresses() throws InterruptedException {
        if (server.getFortress1() == null) { return; }

        boolean changed = false;

        // Increase resources if player collides with fortress when holding resources
        for (Player p : server.getPlayers()) {
            if (p.wood == 0 && p.iron == 0) { continue; }

            if (p.team && p.intersects(server.getFortress2())) {
                server.getFortress2().setWood(server.getFortress2().getWood() + p.wood);
                server.getFortress2().setIron(server.getFortress2().getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            } else if (!p.team && p.intersects(server.getFortress1())) {
                server.getFortress1().setWood(server.getFortress1().getWood() + p.wood);
                server.getFortress1().setIron(server.getFortress1().getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            }
        }

        // Prevent player from going through enemy fortress
        for (Player p : server.getPlayers()) {
            if (!p.team && p.intersects(server.getFortress2())) {
                // TODO Move blue player out of red fortress
            } else if (p.team && p.intersects(server.getFortress1())) {
                // TODO Move red player out of blue fortress
            }
        }

        // Reduce HP of fortress if bullet or cannon collides with it
        server.getMutexSpace().get(new ActualField("bulletsLock"));
        for (Bullet b : server.getBullets()) {
            if (b.getTeam() && b.intersects(server.getFortress1())) {
                server.getFortress1().setHP(server.getFortress1().getHP() - 5);
                changed = true;
            } else if (!b.getTeam() && b.intersects(server.getFortress2())) {
                server.getFortress2().setHP(server.getFortress2().getHP() - 5);
                changed = true;
            }
        }
        // Remove bullets that hit fortress
        server.getBullets().removeIf(b -> (b.intersects(server.getFortress1()) && b.getTeam()) || (b.intersects(server.getFortress2()) && !b.getTeam()));
        server.getMutexSpace().put("bulletsLock");

        // Look for a winner
        if (server.getFortress1().getHP() <= 0) {
            server.gameOver(false);
        } else if (server.getFortress2().getHP() <= 0) {
            server.gameOver(true);
        }

        if (changed) { changeFortress(); }
    }

    public void changeFortress() {
        try {
            fortressSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));

            // Update fortresses if they exist, otherwise build two new ones
            if (server.getFortress1() != null) {
                fortressSpace.put(server.getFortress1().getWood(), server.getFortress1().getIron(), server.getFortress1().getHP(), false);
                fortressSpace.put(server.getFortress2().getWood(), server.getFortress2().getIron(), server.getFortress2().getHP(), true);
            } else {
                server.setFortress1(new Fortress(false));
                server.setFortress2(new Fortress(true));
                fortressSpace.put(0, 0, 100, false);
                fortressSpace.put(0, 0, 100, true);
            }
        } catch (InterruptedException e) {}
    }

    public Space getFortressSpace() {
        return fortressSpace;
    }
}
