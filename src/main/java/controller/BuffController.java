package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.List;

public class BuffController {
    Server s;
    private double team1GhostTimer = 0;
    private double team2GhostTimer = 0;

    public BuffController(Server server){
        this.s = server;
    }

    public void initializeBuffs() throws InterruptedException {
        team1GhostTimer = 0;
        team2GhostTimer = 0;
        s.getBuffSpace().getAll(new FormalField(Boolean.class), new FormalField(String.class));
        s.setBuffSpace(new SequentialSpace());
    }

    public void updateBuffs() throws InterruptedException {
        if (team1GhostTimer > 0) {
            team1GhostTimer -= Server.S_BETWEEN_UPDATES;
        }
        else if (team2GhostTimer > 0) {
            team2GhostTimer -= Server.S_BETWEEN_UPDATES;
        }
        List<Object[]> buffs =  s.getBuffSpace().getAll(new FormalField(Boolean.class), new FormalField(String.class));
        for (Object[] buff : buffs) {
            switch ((String)buff[1]){
                case "heal":
                    if((boolean) buff[0]){
                        s.getFortress2().setHP(s.getFortress1().getHP() + 50);
                    } else {
                        s.getFortress1().setHP(s.getFortress2().getHP() + 50);
                    }
                    s.getFortressController().changeFortress();
                    break;
                case "ghost":
                    if((boolean) buff[0]){
                        team2GhostTimer = 5;
                    } else {
                        team1GhostTimer = 5;
                    }
                    break;
                case "bullets":
                    Bullet bullet;
                    double bulletHeight = Fortress.HEIGHT;
                    while (bulletHeight > Server.SCREEN_HEIGHT-Fortress.HEIGHT) {
                        if((boolean) buff[0]) {
                            bullet = new Bullet(s.getFortress2().x - Bullet.WIDTH * 2, bulletHeight, (boolean)buff[0]);
                        } else {
                            bullet = new Bullet(s.getFortress1().x + Fortress.WIDTH + Bullet.WIDTH * 2, bulletHeight, (boolean)buff[0]);
                        }
                        s.getMutexSpace().get(new ActualField("bulletsLock"));
                        s.getBullets().add(bullet);
                        s.getMutexSpace().put("bulletsLock");
                        s.getBulletSpace().put(bullet.x, bullet.y, bullet.getTeam());
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
}
