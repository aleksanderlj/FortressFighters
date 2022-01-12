package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.util.List;

public class BuffController {
    Server server;
    private Space buffSpace;
    private double team1GhostTimer = 0;
    private double team2GhostTimer = 0;

    public BuffController(Server server){
        this.server = server;
        buffSpace = new SequentialSpace();
    }

    public void updateBuffs() throws InterruptedException {
        if (team1GhostTimer > 0) {
            team1GhostTimer -= server.S_BETWEEN_UPDATES;
        }
        else if (team2GhostTimer > 0) {
            team2GhostTimer -= server.S_BETWEEN_UPDATES;
        }
        List<Object[]> buffs =  buffSpace.getAll(new FormalField(Boolean.class), new FormalField(String.class));
        for (Object[] buff : buffs) {
            switch ((String)buff[1]){
                case "heal":
                    if((boolean) buff[0]){
                        server.getFortress1().setHP(server.getFortress1().getHP() + 50);
                    } else {
                        server.getFortress2().setHP(server.getFortress2().getHP() + 50);
                    }
                    server.getFortressController().changeFortress();
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
                            bullet = new Bullet(server.getFortress1().x + Fortress.WIDTH + Bullet.WIDTH * 2, bulletHeight, !(boolean)buff[0]);
                        } else {
                            bullet = new Bullet(server.getFortress2().x - Bullet.WIDTH * 2, bulletHeight, !(boolean)buff[0]);
                        }
                        server.getMutexSpace().get(new ActualField("bulletsLock"));
                        server.getBullets().add(bullet);
                        server.getMutexSpace().put("bulletsLock");
                        server.getCannonController().getBulletSpace().put(bullet.x, bullet.y, bullet.getTeam());
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

    public Space getBuffSpace() {
        return buffSpace;
    }

    public void setBuffSpace(Space buffSpace) {
        this.buffSpace = buffSpace;
    }

    public void resetBuffSpace() throws InterruptedException {
        buffSpace.getAll(new FormalField(Boolean.class), new FormalField(String.class));
    }
}
