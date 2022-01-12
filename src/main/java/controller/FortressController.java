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
    private Fortress fortress1;
    private Fortress fortress2;

    public FortressController(Server server){
        this.server = server;
        fortressSpace = new SequentialSpace();
    }

    public void initializeFortresses(){
        fortress1 = null;
        fortress2 = null;
        changeFortress();
    }

    public void updateFortresses() throws InterruptedException {
        if (fortress1 == null) { return; }

        boolean changed = false;

        // Increase resources if player collides with fortress when holding resources
        for (Player p : server.getPlayerController().getPlayers()) {
            if (p.wood == 0 && p.iron == 0) { continue; }

            if (p.team && p.intersects(fortress2)) {
                fortress2.setWood(fortress2.getWood() + p.wood);
                fortress2.setIron(fortress2.getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            } else if (!p.team && p.intersects(fortress1)) {
                fortress1.setWood(fortress1.getWood() + p.wood);
                fortress1.setIron(fortress1.getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            }
        }

        // Reduce HP of fortress if bullet or cannon collides with it
        server.getMutexSpace().get(new ActualField("bulletsLock"));
        for (Bullet b : server.getCannonController().getBullets()) {
            if (b.getTeam() && b.intersects(fortress1)) {
                fortress1.setHP(fortress1.getHP() - 5);
                changed = true;
            } else if (!b.getTeam() && b.intersects(fortress2)) {
                fortress2.setHP(fortress2.getHP() - 5);
                changed = true;
            }
        }
        // Remove bullets that hit fortress
        server.getCannonController().getBullets().removeIf(b -> (b.intersects(fortress1) && b.getTeam()) || (b.intersects(fortress2) && !b.getTeam()));
        server.getMutexSpace().put("bulletsLock");

        // Look for a winner
        if (fortress1.getHP() <= 0) {
            server.gameOver(false);
        } else if (fortress2.getHP() <= 0) {
            server.gameOver(true);
        }

        if (changed) { changeFortress(); }
    }

    public void changeFortress() {
        try {
            fortressSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));

            // Update fortresses if they exist, otherwise build two new ones
            if (fortress1 != null) {
                fortressSpace.put(fortress1.getWood(), fortress1.getIron(), fortress1.getHP(), false);
                fortressSpace.put(fortress2.getWood(), fortress2.getIron(), fortress2.getHP(), true);
            } else {
                fortress1 = new Fortress(false);
                fortress2 = new Fortress(true);
                fortressSpace.put(0, 0, 100, false);
                fortressSpace.put(0, 0, 100, true);
            }
        } catch (InterruptedException e) {}
    }

    public Space getFortressSpace() {
        return fortressSpace;
    }

    public Fortress getFortress1() {
        return fortress1;
    }

    public Fortress getFortress2() {
        return fortress2;
    }
}
