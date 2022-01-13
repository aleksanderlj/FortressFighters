package controller;

import game.Server;
import model.Bullet;
import model.Cannon;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class CannonController {
    Server s;

    public CannonController(Server server){
        this.s = server;
    }

    public void initializeCannons() throws InterruptedException {
        s.getCannons().forEach(c -> c.setAlive(false));
        s.getCannons().clear();
        s.getCannonSpace().getAll(new FormalField(Integer.class), new ActualField(String.class));
        s.getCannonSpace().getAll(new ActualField("cannon"), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
    }

    public void initializeBullets() throws InterruptedException {
        s.getMutexSpace().get(new ActualField("bulletsLock"));
        s.getBullets().clear();
        s.getBulletSpace().getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
        s.getMutexSpace().put("bulletsLock");
    }

    public void updateCannons() throws InterruptedException {
        List<Object[]> cannonCommands = s.getCannonSpace().getAll(new FormalField(Integer.class), new FormalField(String.class));
        Cannon newCannon;
        for (Object[] command : cannonCommands) {
            int id = (int) command[0];
            Player player = s.getPlayers().get(id);
            newCannon = new Cannon(player.x + player.width / 4, player.y + player.height / 2, player.team);

            // Only build cannon if it's not colliding with another cannon, wall, fortress
            if(
                    s.getCannons().stream().noneMatch(newCannon::intersects) &&
                            s.getWalls().stream().noneMatch(newCannon::intersects) &&
                            !newCannon.intersects(s.getFortress1()) &&
                            !newCannon.intersects(s.getFortress2())
            ){
                // Spend resources from fortress when building a cannon
                if (!newCannon.getTeam() && s.getFortress1().getIron() >= Cannon.IRON_COST) {
                    s.getFortress1().setIron(s.getFortress1().getIron() - Cannon.IRON_COST);
                    s.getFortressController().changeFortress();
                } else if (newCannon.getTeam() && s.getFortress2().getIron() >= Cannon.IRON_COST) {
                    s.getFortress2().setIron(s.getFortress2().getIron() - Cannon.IRON_COST);
                    s.getFortressController().changeFortress();
                } else {
                    return;
                }

                s.getCannons().add(newCannon);
                s.getCannonSpace().put("cannon", newCannon.x, newCannon.y, newCannon.getTeam());
                new Thread(new CannonShooter(newCannon)).start(); // TODO Need some way to stop and remove this when game is reset or cannon is destroyed
            }
        }
    }

    public void updateBullets() throws InterruptedException{
        s.getBulletSpace().getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
        s.getMutexSpace().get(new ActualField("bulletsLock"));
        s.getBullets().removeIf(b -> b.x < 0 || b.x > s.SCREEN_WIDTH); // Remove bullets that are out of bounds
        for (Bullet b : s.getBullets()) {
            if(b.getTeam()){
                b.x -= Bullet.SPEED * s.S_BETWEEN_UPDATES;
            } else {
                b.x += Bullet.SPEED * s.S_BETWEEN_UPDATES;
            }
            s.getBulletSpace().put(b.x, b.y, b.getTeam());
        }
        s.getMutexSpace().put("bulletsLock");
    }

    public class CannonShooter implements Runnable {
        public static final int COOLDOWN = 3000;
        Cannon cannon;

        public CannonShooter(Cannon cannon){
            this.cannon = cannon;
        }

        public void run() {
            try {
                Thread.sleep(COOLDOWN);
                while(!s.isGameOver() && cannon.isAlive()){
                    if(cannon.isActive()){
                        Bullet bullet;
                        if(cannon.getTeam()){
                            bullet = new Bullet(cannon.x + Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
                        } else {
                            bullet = new Bullet(cannon.x + Cannon.WIDTH - Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
                        }
                        s.getMutexSpace().get(new ActualField("bulletsLock"));
                        s.getBullets().add(bullet);
                        s.getMutexSpace().put("bulletsLock");
                        s.getBulletSpace().put(bullet.x, bullet.y, bullet.getTeam());
                        Thread.sleep(COOLDOWN);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
