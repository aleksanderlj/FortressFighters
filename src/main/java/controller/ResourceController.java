package controller;

import game.Server;
import model.Fortress;
import model.Player;
import model.Resource;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResourceController {
    Server server;
    private Space resourceSpace;
    private List<Resource> resources;
    public static final int INITIAL_RESOURCES = 10;

    public ResourceController(Server server){
        this.server = server;
        resourceSpace = new SequentialSpace();
        resources = new ArrayList<Resource>();
    }

    public void initializeResources(){
        resources.clear();
        for (int i = 0; i < INITIAL_RESOURCES; i++) {
            resources.add(createRandomResource());
        }
        resourcesChanged();
    }

    public void updateResources() {
        List<Resource> newResources = new ArrayList<>();
        for (int i = 0; i < resources.size(); i++) {
            boolean add = true;
            Resource r = resources.get(i);
            for (int j = 0; j < server.getPlayerController().getPlayers().size(); j++) {
                Player p = server.getPlayerController().getPlayers().get(j);
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
        boolean update = resources.size() != newResources.size();
        for (int i = newResources.size(); i < resources.size(); i++) {
            newResources.add(createRandomResource());
        }
        resources = newResources;
        if (update) {
            resourcesChanged();
        }
    }

    public void resourcesChanged() {
        try {
            resourceSpace.getAll(new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class));
            for (Resource r : resources) {
                resourceSpace.put((int)r.x, (int)r.y, r.getType());
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

    public Space getResourceSpace() {
        return resourceSpace;
    }

    public List<Resource> getResources() {
        return resources;
    }
}
