package controller;

import game.Server;
import model.Player;
import model.Resource;

import java.util.ArrayList;
import java.util.List;

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
            newResources.add(server.createRandomResource());
        }
        server.resources = newResources;
        if (update) {
            server.resourcesChanged();
        }
    }
}
