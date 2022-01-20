package controller;

import game.Server;
import model.Bullet;
import model.Fortress;
import model.Player;
import model.Wall;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.List;

public class BuffController {
    Server s;
    private static final double GHOST_TIME = 5;
    private double team1GhostTimer = 0;
    private double team2GhostTimer = 0;
    private static final double SHIELD_TIME = 8;
    private double team1ShieldTimer = 0;
    private double team2ShieldTimer = 0;
    private Wall team1Shield = null;
    private Wall team2Shield = null;

    public BuffController(Server server){
        this.s = server;
    }

    public void initializeBuffs() throws InterruptedException {
        team1GhostTimer = 0;
        team2GhostTimer = 0;
        team1ShieldTimer = 0;
        team2ShieldTimer = 0;
        s.getBuffSpace().getAll(new FormalField(Boolean.class), new FormalField(String.class));
        s.setBuffSpace(new SequentialSpace());
    }

    public void updateBuffs() throws InterruptedException {
        if (team1GhostTimer > 0) {
            team1GhostTimer -= Server.S_BETWEEN_UPDATES;
        }
        if (team2GhostTimer > 0) {
            team2GhostTimer -= Server.S_BETWEEN_UPDATES;
        }

        // Decrement shield timer, remove shield if it ran out
        if (team1ShieldTimer > 0) {
            team1ShieldTimer -= Server.S_BETWEEN_UPDATES;
        } else if(team1Shield != null) {
            s.getWalls().remove(team1Shield);
            s.getWallSpace().getp(new ActualField("wall"), new ActualField(team1Shield.getId()), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
            team1Shield = null;
        }
        if (team2ShieldTimer > 0) {
            team2ShieldTimer -= Server.S_BETWEEN_UPDATES;
        } else if(team2Shield != null) {
            s.getWalls().remove(team2Shield);
            s.getWallSpace().getp(new ActualField("wall"), new ActualField(team2Shield.getId()), new FormalField(Integer.class), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
            team2Shield = null;
        }
        List<Object[]> buffs =  s.getBuffSpace().getAll(new FormalField(Boolean.class), new FormalField(String.class));
        for (Object[] buff : buffs) {
            switch ((String)buff[1]){
                case "heal":
                    if((boolean) buff[0]){
                        s.getFortress2().setHP(s.getFortress2().getHP() + 50);
                    } else {
                        s.getFortress1().setHP(s.getFortress1().getHP() + 50);
                    }
                    s.getFortressController().changeFortress();
                    break;
                case "ghost":
                    if((boolean) buff[0]){
                        team2GhostTimer = GHOST_TIME;
                    } else {
                        team1GhostTimer = GHOST_TIME;
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
                case "shield":
                    Wall shield = null;
                    if((boolean) buff[0]) {
                        team2ShieldTimer = SHIELD_TIME;
                        if(team2Shield == null ){
                            shield = new Wall(s.getFortress2().x - Wall.SHIELD_WIDTH, s.getFortress2().y, Wall.SHIELD_WIDTH, Wall.SHIELD_HEIGHT, (boolean) buff[0]);
                            team2Shield = shield;
                            s.getWalls().add(shield);
                            s.getWallSpace().put("wall", shield.getId(), shield.getHealth(), shield.x, shield.y, shield.getTeam());
                        }
                    } else {
                        team1ShieldTimer = SHIELD_TIME;
                        if(team1Shield == null){
                            shield = new Wall(s.getFortress1().x + Fortress.WIDTH, s.getFortress1().y, Wall.SHIELD_WIDTH, Wall.SHIELD_HEIGHT, (boolean) buff[0]);
                            team1Shield = shield;
                            s.getWalls().add(shield);
                            s.getWallSpace().put("wall", shield.getId(), shield.getHealth(), shield.x, shield.y, shield.getTeam());
                        }
                    }
                    break;
            }

            for (Player p : s.getPlayers()) {
                p.serverToClient.put("buff_activated", (String)buff[1], (boolean) buff[0]);
            }
        }
    }

    public boolean isGhost(Player p) {
        return (team1GhostTimer > 0 && !p.team) || (team2GhostTimer > 0 && p.team);
    }
}
