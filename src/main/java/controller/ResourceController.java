package controller;

import game.Server;
import model.Fortress;
import model.Player;
import model.Resource;
import org.jspace.FormalField;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResourceController {
    Server s;
    public static final int INITIAL_RESOURCES = 10;

    public ResourceController(Server server){
        this.s = server;
    }

    public void initializeResources(){
        s.getResources().clear();
        for (int i = 0; i < INITIAL_RESOURCES; i++) {
            s.getResources().add(createRandomResource());
        }
        resourcesChanged();
    }

    public void updateResources() {
        List<Resource> newResources = new ArrayList<>();
        for (int i = 0; i < s.getResources().size(); i++) {
            boolean add = true;
            Resource r = s.getResources().get(i);
            for (int j = 0; j < s.getActualNumberOfPlayers(); j++) {
                Player p = s.getPlayers().get(j);
                if (p.intersects(r)) {
                    add = false;
                    if (r.getType() == 0) {
                        p.wood++;
                    }
                    else {
                        p.iron++;
                    }
                    break;
                }
            }
            if (add) {
                newResources.add(r);
            }
        }
        boolean update = s.getResources().size() != newResources.size();
        for (int i = newResources.size(); i < s.getResources().size(); i++) {
            newResources.add(createRandomResource());
        }
        s.setResources(newResources);
        if (update) {
            resourcesChanged();
        }
    }

    public void resourcesChanged() {
        try {
            s.getResourceSpace().getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class));
            for (Resource r : s.getResources()) {
                s.getResourceSpace().put((int)r.x, (int)r.y, r.getType());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Resource createRandomResource() {
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
        // TODO Balance wood vs iron ratio
        int type = (new Random()).nextInt(2);
        // TODO Check for collision with player, wall, cannon, resource ?
        return new Resource(pos[0], pos[1], type);
    }

    private int[] getRandomPosition() {
        Random r = new Random();
        int x = r.nextInt((int)(Server.SCREEN_WIDTH-2* Fortress.WIDTH-2*Resource.WIDTH))+(int)Fortress.WIDTH+(int)Resource.WIDTH;
        int y = r.nextInt((int)(Server.SCREEN_HEIGHT-2*Resource.WIDTH))+(int)Resource.WIDTH;
        return new int[] {x, y};
    }
}
