package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

public class FortressController {
    Server s;

    public FortressController(Server server){
        this.s = server;
    }

    public void initializeFortresses(){
        s.setFortress1(null);
        s.setFortress2(null);
        changeFortress();
    }

    public void updateFortresses() throws InterruptedException {
        if (s.getFortress1() == null) { return; }

        boolean changed = false;

        // Increase resources if player collides with fortress when holding resources
        for (Player p : s.getPlayers()) {
            if (p.wood == 0 && p.iron == 0) { continue; }

            if (p.team && p.intersects(s.getFortress2())) {
                s.getFortress2().setWood(s.getFortress2().getWood() + p.wood);
                s.getFortress2().setIron(s.getFortress2().getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            } else if (!p.team && p.intersects(s.getFortress1())) {
                s.getFortress1().setWood(s.getFortress1().getWood() + p.wood);
                s.getFortress1().setIron(s.getFortress1().getIron() + p.iron);
                p.wood = 0;
                p.iron = 0;
                changed = true;
            }
        }

        // Prevent player from going through enemy fortress
        for (Player p : s.getPlayers()) {
            if (!p.team && p.intersects(s.getFortress2())) {
                // TODO Move blue player out of red fortress
            } else if (p.team && p.intersects(s.getFortress1())) {
                // TODO Move red player out of blue fortress
            }
        }

        // Reduce HP of fortress if bullet or cannon collides with it
        s.getMutexSpace().get(new ActualField("bulletsLock"));
        for (Bullet b : s.getBullets()) {
            if (b.getTeam() && b.intersects(s.getFortress1())) {
                s.getFortress1().setHP(s.getFortress1().getHP() - 5);
                changed = true;
            } else if (!b.getTeam() && b.intersects(s.getFortress2())) {
                s.getFortress2().setHP(s.getFortress2().getHP() - 5);
                changed = true;
            }
        }
        // Remove bullets that hit fortress
        s.getBullets().removeIf(b -> (b.intersects(s.getFortress1()) && b.getTeam()) || (b.intersects(s.getFortress2()) && !b.getTeam()));
        s.getMutexSpace().put("bulletsLock");

        // Look for a winner
        if (s.getFortress1().getHP() <= 0) {
            s.gameOver(false);
        } else if (s.getFortress2().getHP() <= 0) {
            s.gameOver(true);
        }

        if (changed) { changeFortress(); }
    }

    public void changeFortress() {
        try {
            s.getFortressSpace().getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Boolean.class));

            // Update fortresses if they exist, otherwise build two new ones
            if (s.getFortress1() != null) {
                s.getFortressSpace().put(s.getFortress1().getWood(), s.getFortress1().getIron(), s.getFortress1().getHP(), false);
                s.getFortressSpace().put(s.getFortress2().getWood(), s.getFortress2().getIron(), s.getFortress2().getHP(), true);
            } else {
                s.setFortress1(new Fortress(false));
                s.setFortress2(new Fortress(true));
                s.getFortressSpace().put(0, 0, 100, false);
                s.getFortressSpace().put(0, 0, 100, true);
            }
        } catch (InterruptedException e) {}
    }
}
