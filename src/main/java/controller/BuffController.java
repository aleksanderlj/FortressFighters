package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class BuffController {
    Server server;
    private double team1GhostTimer = 0;
    private double team2GhostTimer = 0;

    public BuffController(Server server){
        this.server = server;
    }

    public void updateBuffs() throws InterruptedException {
        if (team1GhostTimer > 0) {
            team1GhostTimer -= server.S_BETWEEN_UPDATES;
        }
        else if (team2GhostTimer > 0) {
            team2GhostTimer -= server.S_BETWEEN_UPDATES;
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
                        team1GhostTimer = 5;
                    } else {
                        team2GhostTimer = 5;
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

    public boolean isGhost(Player p) {
        return (team1GhostTimer > 0 && !p.team) || (team2GhostTimer > 0 && p.team);
    }

    public void resetTimers(){
        team1GhostTimer = 0;
        team2GhostTimer = 0;
    }
}
