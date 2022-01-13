package controller;

import game.Server;
import model.*;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OrbController {
    Server s;

    public OrbController(Server server){
        this.s = server;
    }

    public void updateOrbs() {
        List<Orb> newOrbs = new ArrayList<>();
        for (int i = 0; i < s.getOrbs().size(); i++) {
            boolean add = true;
            Orb o = s.getOrbs().get(i);
            for (int j = 0; j < s.getPlayers().size(); j++) {
                Player p = s.getPlayers().get(j);
                if (!p.disconnected && p.intersects(o) && !p.hasOrb) {
                    add = false;
                    p.hasOrb = true;
                    try {
                        s.getOrbSpace().get(new ActualField((int)o.x), new ActualField((int)o.y));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (add) {
                newOrbs.add(o);
            }
        }
        s.setOrbs(newOrbs);
        for (int i = 0; i < s.getOrbHolders().size(); i++) {
            OrbHolder oh = s.getOrbHolders().get(i);
            for (int j = 0; j < s.getPlayers().size(); j++) {
                Player p = s.getPlayers().get(j);
                if (!p.disconnected && p.intersects(oh) && p.hasOrb && !oh.hasOrb) {
                    p.hasOrb = true;
                    try {
                        s.getOrbSpace().get(new ActualField(oh.team), new ActualField(oh.top), new ActualField(oh.hasOrb));
                        oh.hasOrb = true;
                        p.hasOrb = false;
                        s.getOrbSpace().put(oh.team, oh.top, oh.hasOrb);
                        s.getBuffSpace().put(oh.team, oh.top);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void createNewOrb() {
        int[] pos;
        while (true) {
            pos = getRandomPosition();
            boolean breakWhile = true;
            for (Player p : s.getPlayers()) {
                if (p.intersects(new Rectangle.Double(pos[0], pos[1], 0, 0))) {
                    breakWhile = false;
                    break;
                }
            }
            if (breakWhile) {
                break;
            }
        }
        Orb o = new Orb(pos[0], pos[1]);
        s.getOrbs().add(o);
        try {
            s.getOrbSpace().put((int)o.x, (int)o.y);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int[] getRandomPosition() {
        Random r = new Random();
        int x = r.nextInt((int)(Server.SCREEN_WIDTH-2* Fortress.WIDTH-2* Orb.WIDTH))+(int)Fortress.WIDTH+(int) Orb.WIDTH;
        int y = r.nextInt((int)(Server.SCREEN_HEIGHT-2*Orb.WIDTH))+(int)Orb.WIDTH;
        return new int[] {x, y};
    }

    public void resetOrbHolder(boolean team, boolean top) {
        for (OrbHolder oh : s.getOrbHolders()) {
            if (oh.team == team && oh.top == top) {
                oh.hasOrb = false;
                try {
                    s.getOrbSpace().get(new ActualField(oh.team), new ActualField(oh.top), new FormalField(Boolean.class));
                    s.getOrbSpace().put(oh.team, oh.top, oh.hasOrb);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
