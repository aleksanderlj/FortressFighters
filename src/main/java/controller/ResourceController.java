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
    Server server;

    public ResourceController(Server server){
        this.server = server;
    }

    public void updateResources() {
        List<Resource> newResources = new ArrayList<>();
        for (int i = 0; i < server.resources.size(); i++) {
            boolean add = true;
            Resource r = server.resources.get(i);
            for (int j = 0; j < server.players.size(); j++) {
                Player p = server.players.get(j);
                if (!p.disconnected && p.intersects(r)) {
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
        boolean update = server.resources.size() != newResources.size();
        for (int i = newResources.size(); i < server.resources.size(); i++) {
            newResources.add(createRandomResource());
        }
        server.resources = newResources;
        if (update) {
            resourcesChanged();
        }
    }

    public void resourcesChanged() {
        try {
            server.resourceSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class));
            for (Resource r : server.resources) {
                server.resourceSpace.put((int)r.x, (int)r.y, r.getType());
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
            for (Player p : server.players) {
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
