package controller;

import com.sun.org.apache.xpath.internal.operations.Or;
import game.OrbPetriNet;
import game.Server;
import model.*;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OrbController {
    Server server;
    private Space orbSpace;
    private List<Orb> orbs;
    private List<OrbHolder> orbHolders;
    private OrbPetriNet orbPetriNet1;
    private OrbPetriNet orbPetriNet2;

    public OrbController(Server server){
        this.server = server;
        orbSpace = new SequentialSpace();
        orbs = new ArrayList<>();
        orbHolders = new ArrayList<>();
    }

    public void initializeOrbPetriNets(){
        orbPetriNet1 = new OrbPetriNet(server, false);
        orbPetriNet2 = new OrbPetriNet(server, true);
        new Thread(orbPetriNet1).start();
        new Thread(orbPetriNet2).start();
        for (int i = 0; i < 3; i++) {
            createNewOrb();
        }
        orbHolders.add(new OrbHolder(false, true, false));
        orbHolders.add(new OrbHolder(true, false, false));
        orbHolders.add(new OrbHolder(false, false, false));
        orbHolders.add(new OrbHolder(true, true, false));
        for (OrbHolder oh : orbHolders) {
            try {
                orbSpace.put(oh.team, oh.top, oh.hasOrb);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void initializeOrbs(){
        orbs.clear();
    }

    public void initializeOrbHolders(){
        orbHolders.clear();
    }

    public void resetPetriNet() {
        try {
            server.getBuffController().getBuffSpace().put(false, false);
            server.getBuffController().getBuffSpace().put(false, true);
            server.getBuffController().getBuffSpace().put(true, false);
            server.getBuffController().getBuffSpace().put(true, true);
            orbPetriNet1.reset();
            orbPetriNet2.reset();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateOrbs() {
        List<Orb> newOrbs = new ArrayList<>();
        for (int i = 0; i < orbs.size(); i++) {
            boolean add = true;
            Orb o = orbs.get(i);
            for (int j = 0; j < server.getPlayerController().getPlayers().size(); j++) {
                Player p = server.getPlayerController().getPlayers().get(j);
                if (!p.disconnected && p.intersects(o) && !p.hasOrb) {
                    add = false;
                    p.hasOrb = true;
                    try {
                        orbSpace.get(new ActualField((int)o.x), new ActualField((int)o.y));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (add) {
                newOrbs.add(o);
            }
        }
        orbs = newOrbs;
        for (int i = 0; i < orbHolders.size(); i++) {
            OrbHolder oh = orbHolders.get(i);
            for (int j = 0; j < server.getPlayerController().getPlayers().size(); j++) {
                Player p = server.getPlayerController().getPlayers().get(j);
                if (!p.disconnected && p.intersects(oh) && p.hasOrb && !oh.hasOrb) {
                    p.hasOrb = true;
                    try {
                        orbSpace.get(new ActualField(oh.team), new ActualField(oh.top), new ActualField(oh.hasOrb));
                        oh.hasOrb = true;
                        p.hasOrb = false;
                        orbSpace.put(oh.team, oh.top, oh.hasOrb);
                        server.getBuffController().getBuffSpace().put(oh.team, oh.top);
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
            for (Player p : server.getPlayerController().getPlayers()) {
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
        orbs.add(o);
        try {
            orbSpace.put((int)o.x, (int)o.y);
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
        for (OrbHolder oh : orbHolders) {
            if (oh.team == team && oh.top == top) {
                oh.hasOrb = false;
                try {
                    orbSpace.get(new ActualField(oh.team), new ActualField(oh.top), new FormalField(Boolean.class));
                    orbSpace.put(oh.team, oh.top, oh.hasOrb);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Space getOrbSpace() {
        return orbSpace;
    }

    public void resetOrbSpace() throws InterruptedException {
        orbSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class));
        orbSpace.getAll(new FormalField(Boolean.class), new FormalField(Boolean.class), new FormalField(Boolean.class));
    }
}
