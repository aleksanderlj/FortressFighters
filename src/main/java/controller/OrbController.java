package controller;

import game.Server;
import model.Orb;
import model.OrbHolder;
import model.Player;
import org.jspace.ActualField;

import java.util.ArrayList;
import java.util.List;

public class OrbController {
    Server server;

    public OrbController(Server server){
        this.server = server;
    }

    public void updateOrbs() {
        List<Orb> newOrbs = new ArrayList<>();
        for (int i = 0; i < server.orbs.size(); i++) {
            boolean add = true;
            Orb o = server.orbs.get(i);
            for (int j = 0; j < server.players.size(); j++) {
                Player p = server.players.get(j);
                if (!p.disconnected && p.intersects(o) && !p.hasOrb) {
                    add = false;
                    p.hasOrb = true;
                    try {
                        server.orbSpace.get(new ActualField((int)o.x), new ActualField((int)o.y));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (add) {
                newOrbs.add(o);
            }
        }
        server.orbs = newOrbs;
        for (int i = 0; i < server.orbHolders.size(); i++) {
            OrbHolder oh = server.orbHolders.get(i);
            for (int j = 0; j < server.players.size(); j++) {
                Player p = server.players.get(j);
                if (!p.disconnected && p.intersects(oh) && p.hasOrb && !oh.hasOrb) {
                    p.hasOrb = true;
                    try {
                        server.orbSpace.get(new ActualField(oh.team), new ActualField(oh.top), new ActualField(oh.hasOrb));
                        oh.hasOrb = true;
                        p.hasOrb = false;
                        server.orbSpace.put(oh.team, oh.top, oh.hasOrb);
                        server.buffSpace.put(oh.team, oh.top);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
