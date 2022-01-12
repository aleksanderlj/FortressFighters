package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class BuffController {
    Server server;

    public BuffController(Server server){
        this.server = server;
    }

    public void updateBuffs() throws InterruptedException {
        if (server.team1GhostTimer > 0) {
            server.team1GhostTimer -= server.S_BETWEEN_UPDATES;
        }
        else if (server.team2GhostTimer > 0) {
            server.team2GhostTimer -= server.S_BETWEEN_UPDATES;
        }
        List<Object[]> buffs =  server.buffSpace.getAll(new FormalField(Boolean.class), new FormalField(String.class));
        for (Object[] buff : buffs) {
            switch ((String)buff[1]){
                case "heal":
                    if((boolean) buff[0]){
                        server.fortress1.setHP(server.fortress1.getHP() + 50);
                    } else {
                        server.fortress2.setHP(server.fortress2.getHP() + 50);
                    }
                    server.fortressController.changeFortress();
                    break;
                case "ghost":
                    if((boolean) buff[0]){
                        server.team1GhostTimer = 5;
                    } else {
                        server.team2GhostTimer = 5;
                    }
                    break;
                case "bullets":
                    Bullet bullet;
                    double bulletHeight = Fortress.HEIGHT;
                    while (bulletHeight > server.SCREEN_HEIGHT-Fortress.HEIGHT) {
                        if((boolean) buff[0]) {
                            bullet = new Bullet(server.fortress1.x + Fortress.WIDTH + Bullet.WIDTH * 2, bulletHeight, !(boolean)buff[0]);
                        } else {
                            bullet = new Bullet(server.fortress2.x - Bullet.WIDTH * 2, bulletHeight, !(boolean)buff[0]);
                        }
                        server.mutexSpace.get(new ActualField("bulletsLock"));
                        server.bullets.add(bullet);
                        server.mutexSpace.put("bulletsLock");
                        server.bulletSpace.put(bullet.x, bullet.y, bullet.getTeam());
                        bulletHeight -= 40;
                        Thread.sleep(50);
                    }
                    break;
            }
        }
    }
}
