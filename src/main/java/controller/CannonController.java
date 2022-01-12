package controller;

import game.Server;
import model.Bullet;
import model.Cannon;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import java.util.List;

public class CannonController {
    Server server;
    private Space cannonSpace;
    private Space bulletSpace;

    public CannonController(Server server){
        this.server = server;
        cannonSpace = new SequentialSpace();
        bulletSpace = new SequentialSpace();
    }

    public void updateCannons() throws InterruptedException {
        List<Object[]> cannonCommands = cannonSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
        Cannon newCannon;
        for (Object[] command : cannonCommands) {
            int id = (int) command[0];
            Player player = server.getPlayers().get(id);
            newCannon = new Cannon(player.x + player.width / 4, player.y + player.height / 2, player.team);

            // Only build cannon if it's not colliding with another cannon, wall, fortress
            if(
                    server.getCannons().stream().noneMatch(newCannon::intersects) &&
                            server.getWalls().stream().noneMatch(newCannon::intersects) &&
                            !newCannon.intersects(server.getFortress1()) &&
                            !newCannon.intersects(server.getFortress2())
            ){
                // Spend resources from fortress when building a cannon
                if (!newCannon.getTeam() && server.getFortress1().getIron() >= Cannon.IRON_COST) {
                    server.getFortress1().setIron(server.getFortress1().getIron() - Cannon.IRON_COST);
                    server.getFortressController().changeFortress();
                } else if (newCannon.getTeam() && server.getFortress2().getIron() >= Cannon.IRON_COST) {
                    server.getFortress2().setIron(server.getFortress2().getIron() - Cannon.IRON_COST);
                    server.getFortressController().changeFortress();
                } else {
                    return;
                }

                server.getCannons().add(newCannon);
                cannonSpace.put("cannon", newCannon.x, newCannon.y, newCannon.getTeam());
                new Thread(new CannonShooter(newCannon)).start(); // TODO Need some way to stop and remove this when game is reset or cannon is destroyed
            }
        }
    }

    public void updateBullets() throws InterruptedException{
        bulletSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
        server.getMutexSpace().get(new ActualField("bulletsLock"));
        server.getBullets().removeIf(b -> b.x < 0 || b.x > server.SCREEN_WIDTH); // Remove bullets that are out of bounds
        for (Bullet b : server.getBullets()) {
            if(b.getTeam()){
                b.x -= Bullet.SPEED * server.S_BETWEEN_UPDATES;
            } else {
                b.x += Bullet.SPEED * server.S_BETWEEN_UPDATES;
            }
            bulletSpace.put(b.x, b.y, b.getTeam());
        }
        server.getMutexSpace().put("bulletsLock");
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
                while(!server.isGameOver() && cannon.isAlive()){
                    if(cannon.isActive()){
                        Bullet bullet;
                        if(cannon.getTeam()){
                            bullet = new Bullet(cannon.x + Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
                        } else {
                            bullet = new Bullet(cannon.x + Cannon.WIDTH - Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
                        }
                        server.getMutexSpace().get(new ActualField("bulletsLock"));
                        server.getBullets().add(bullet);
                        server.getMutexSpace().put("bulletsLock");
                        bulletSpace.put(bullet.x, bullet.y, bullet.getTeam());
                        Thread.sleep(COOLDOWN);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Space getCannonSpace() {
        return cannonSpace;
    }

    public Space getBulletSpace() {
        return bulletSpace;
    }

    public void resetCannonSpace() throws InterruptedException {
        cannonSpace.getAll(new FormalField(Integer.class), new ActualField(String.class));
        cannonSpace.getAll(new ActualField("cannon"), new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
    }

    public void resetBulletSpace() throws InterruptedException {
        bulletSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
    }
}
