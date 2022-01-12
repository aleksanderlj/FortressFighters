package controller;

import game.Server;
import model.Bullet;
import model.Cannon;
import model.Player;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class CannonController {
    Server server;

    public CannonController(Server server){
        this.server = server;
    }

    public void updateCannons() throws InterruptedException {
        List<Object[]> cannonCommands = server.cannonSpace.getAll(new FormalField(Integer.class), new FormalField(String.class));
        Cannon newCannon;
        for (Object[] command : cannonCommands) {
            int id = (int) command[0];
            Player player = server.players.get(id);
            newCannon = new Cannon(player.x + player.width / 4, player.y + player.height / 2, player.team);

            // Only build cannon if it's not colliding with another cannon, wall, fortress
            if(
                    server.cannons.stream().noneMatch(newCannon::intersects) &&
                            server.walls.stream().noneMatch(newCannon::intersects) &&
                            !newCannon.intersects(server.fortress1) &&
                            !newCannon.intersects(server.fortress2)
            ){
                // Spend resources from fortress when building a cannon
                if (!newCannon.getTeam() && server.fortress1.getIron() >= Cannon.IRON_COST) {
                    server.fortress1.setIron(server.fortress1.getIron() - Cannon.IRON_COST);
                    server.changeFortress();
                } else if (newCannon.getTeam() && server.fortress2.getIron() >= Cannon.IRON_COST) {
                    server.fortress2.setIron(server.fortress2.getIron() - Cannon.IRON_COST);
                    server.changeFortress();
                } else {
                    return;
                }

                server.cannons.add(newCannon);
                server.cannonSpace.put("cannon", newCannon.x, newCannon.y, newCannon.getTeam());
                new Thread(new CannonShooter(newCannon)).start(); // TODO Need some way to stop and remove this when game is reset or cannon is destroyed
            }
        }
    }

    public void updateBullets() throws InterruptedException{
        server.bulletSpace.getAll(new FormalField(Double.class), new FormalField(Double.class), new FormalField(Boolean.class));
        server.mutexSpace.get(new ActualField("bulletsLock"));
        server.bullets.removeIf(b -> b.x < 0 || b.x > server.SCREEN_WIDTH); // Remove bullets that are out of bounds
        for (Bullet b : server.bullets) {
            if(b.getTeam()){
                b.x -= Bullet.SPEED * server.S_BETWEEN_UPDATES;
            } else {
                b.x += Bullet.SPEED * server.S_BETWEEN_UPDATES;
            }
            server.bulletSpace.put(b.x, b.y, b.getTeam());
        }
        server.mutexSpace.put("bulletsLock");
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
                while(!server.gameOver && cannon.isAlive()){
                    if(cannon.isActive()){
                        Bullet bullet;
                        if(cannon.getTeam()){
                            bullet = new Bullet(cannon.x + Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
                        } else {
                            bullet = new Bullet(cannon.x + Cannon.WIDTH - Bullet.WIDTH, cannon.y + Cannon.HEIGHT / 4, cannon.getTeam());
                        }
                        server.mutexSpace.get(new ActualField("bulletsLock"));
                        server.bullets.add(bullet);
                        server.mutexSpace.put("bulletsLock");
                        server.bulletSpace.put(bullet.x, bullet.y, bullet.getTeam());
                        Thread.sleep(COOLDOWN);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
