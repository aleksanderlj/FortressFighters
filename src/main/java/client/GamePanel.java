package client;

import client.message.MessageBox;
import controller.CannonController;
import game.Server;
import model.*;
import org.jspace.ActualField;
import org.jspace.FormalField;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GamePanel extends JPanel implements KeyListener {
    private Client c;
    public Graphics2D g2D;
    private long deltaTime;
    private long lastUpdate;
    private MessageBox buffMessageBox;
    private MessageBox connectionMessageBox;
    private BufferedImage manblue, manred,
            cannonblue, cannonred,
            fortressblue, fortressred,
            wood, iron, orb,
            bulletred, bulletblue,
            wallred1, wallred2, wallred3,
            wallblue1, wallblue2, wallblue3,
            orbholderempty, orbholderfull,
            manblueorb, manredorb,
            ironShadow, woodShadow, orbShadow,
            cannonblueShadow, cannonredShadow, wallShadow,
            fortressblueShadow, fortressredShadow,
            manShadow,
            shieldblue, shieldred;
    private Font alagard, alagard_small;
    private static final String DEFAULT_FONT = "Comic Sans MS";
    private long lastBulletUpdate = 0;

    public GamePanel(Client client) {
        this.c = client;
        setPreferredSize(new Dimension(Server.SCREEN_WIDTH, Server.SCREEN_HEIGHT));
        addKeyListener(this);
        setFocusable(true);
        setBackground(new Color(241, 209, 141));

        // Load image resources
        try {
            manblue = ImageIO.read(getClass().getClassLoader().getResource("manblue.png"));
            manred = ImageIO.read(getClass().getClassLoader().getResource("manred.png"));
            cannonblue = ImageIO.read(getClass().getClassLoader().getResource("cannonblue.png"));
            cannonred = ImageIO.read(getClass().getClassLoader().getResource("cannonred.png"));
            fortressblue = ImageIO.read(getClass().getClassLoader().getResource("fortressblue.png"));
            fortressred = ImageIO.read(getClass().getClassLoader().getResource("fortressred.png"));
            wood = ImageIO.read(getClass().getClassLoader().getResource("wood.png"));
            iron = ImageIO.read(getClass().getClassLoader().getResource("iron.png"));
            bulletred = ImageIO.read(getClass().getClassLoader().getResource("bulletred.png"));
            bulletblue = ImageIO.read(getClass().getClassLoader().getResource("bulletblue.png"));
            orb = ImageIO.read(getClass().getClassLoader().getResource("orb.png"));
            wallred1 = ImageIO.read(getClass().getClassLoader().getResource("wallred1.png"));
            wallred2 = ImageIO.read(getClass().getClassLoader().getResource("wallred2.png"));
            wallred3 = ImageIO.read(getClass().getClassLoader().getResource("wallred3.png"));
            wallblue1 = ImageIO.read(getClass().getClassLoader().getResource("wallblue1.png"));
            wallblue2 = ImageIO.read(getClass().getClassLoader().getResource("wallblue2.png"));
            wallblue3 = ImageIO.read(getClass().getClassLoader().getResource("wallblue3.png"));
            orbholderempty = ImageIO.read(getClass().getClassLoader().getResource("orbholderempty.png"));
            orbholderfull = ImageIO.read(getClass().getClassLoader().getResource("orbholderfull.png"));
            manblueorb = ImageIO.read(getClass().getClassLoader().getResource("manblueorb.png"));
            manredorb = ImageIO.read(getClass().getClassLoader().getResource("manredorb.png"));
            ironShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_iron.png"));
            woodShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_wood.png"));
            orbShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_orb.png"));
            cannonblueShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_cannonblue.png"));
            cannonredShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_cannonred.png"));
            wallShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_wall.png"));
            fortressblueShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_fortressblue.png"));
            fortressredShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_fortressred.png"));
            manShadow = ImageIO.read(getClass().getClassLoader().getResource("shadow_man.png"));
            shieldblue = ImageIO.read(getClass().getClassLoader().getResource("shieldblue.png"));
            shieldred = ImageIO.read(getClass().getClassLoader().getResource("shieldred.png"));
            alagard = Font.createFont(Font.TRUETYPE_FONT, getClass().getClassLoader().getResourceAsStream("alagard.ttf"));
            alagard = alagard.deriveFont(Font.PLAIN, 36);
            alagard_small = alagard.deriveFont(Font.PLAIN, 24);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        buffMessageBox = new MessageBox(
                Server.SCREEN_WIDTH/2,
                Server.SCREEN_HEIGHT - 20,
                3,
                alagard_small,
                false,
                true
                );

        connectionMessageBox = new MessageBox(
                Server.SCREEN_WIDTH - 250,
                40,
                2,
                new Font(DEFAULT_FONT, Font.BOLD, 15),
                true,
                false
        );
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        long currentTime = System.currentTimeMillis();
        deltaTime = currentTime - lastUpdate;
        lastUpdate = currentTime;
        g2D = (Graphics2D) g;
        if (c.isGameOver()) {
            g2D.setFont(alagard);
            String s = c.getWinningTeam().toUpperCase(Locale.ROOT) + " team has won!";
            g2D.drawString(s, Server.SCREEN_WIDTH/2 - (g2D.getFontMetrics().stringWidth(s)/2), Server.SCREEN_HEIGHT/2 - 100);
            s = "Restarting...";
            g2D.drawString(s, Server.SCREEN_WIDTH/2 - (g2D.getFontMetrics().stringWidth(s)/2), Server.SCREEN_HEIGHT/2);
        }
        else if (c.isGameStarted()) {
            // Render each object on the screen
            paintFortresses();
            paintResources();
            paintOrbs();
            paintCannons();
            paintWalls();
            paintPlayers();
            paintBullets();
            paintBuffs();
            paintConnectionMessages();
            paintScores();
            if (c.isGamePaused()) {
                g2D.setFont(alagard);
                String s = "Switching host...";
                g2D.drawString(s, Server.SCREEN_WIDTH/2 - (g2D.getFontMetrics().stringWidth(s)/2), Server.SCREEN_HEIGHT/2);
            }
        }
        else {
            g2D.setFont(alagard);
            String s = "Waiting for one more player to join...";
            g2D.drawString(s, Server.SCREEN_WIDTH/2 - (g2D.getFontMetrics().stringWidth(s)/2), Server.SCREEN_HEIGHT/2);
        }
    }

    public void paintPlayers(){
        g2D.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 12));
        for (int i = 0; i < c.getPlayers().length; i++) {
            Player p = c.getPlayers()[i];
            g2D.drawImage(manShadow, (int) p.x-2, (int) p.y, (int) p.width+4, (int) p.height+4, null);
            if(p.team){
                if(p.hasOrb){
                    g2D.drawImage(manredorb, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
                } else {
                    g2D.drawImage(manred, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
                }
            } else {
                if(p.hasOrb){
                    g2D.drawImage(manblueorb, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
                } else {
                    g2D.drawImage(manblue, (int) p.x, (int) p.y, (int) p.width, (int) p.height, null);
                }
            }
            //g2D.drawRect((int) p.x, (int) p.y, (int) p.width, (int) p.height);
            if (p.id == Client.id) {
                g2D.setFont(new Font(DEFAULT_FONT, Font.BOLD, 12));
            }
            g2D.drawString(p.name, (int)((p.x+Player.WIDTH/2)-(g2D.getFontMetrics().stringWidth(p.name)/2)), (int)p.y - 5);
            int x1 = (int)p.x + 31;
            int y1 = (int)(p.y + 38);
            g2D.setColor(Color.WHITE);
            g2D.drawString("" + p.wood, x1, y1);
            g2D.drawString("" + p.iron, x1, y1 + 15);
            g2D.setColor(Color.BLACK);
            g2D.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 12));
        }
    }

    public void paintCannons(){
        for (Cannon c : c.getCannons()) {
            if(c.getTeam()){
                g2D.drawImage(cannonredShadow, (int) c.x-2, (int) c.y, (int) c.width+4, (int) c.height+4, null);
                g2D.drawImage(cannonred, (int) c.x, (int) c.y, (int) c.width, (int) c.height, null);
            } else {
                g2D.drawImage(cannonblueShadow, (int) c.x-2, (int) c.y, (int) c.width+4, (int) c.height+4, null);
                g2D.drawImage(cannonblue, (int) c.x, (int) c.y, (int) c.width, (int) c.height, null);
            }
            //g2D.drawRect((int) c.x, (int) c.y, (int) c.width, (int) c.height);
        }
    }

    public void paintBullets(){
        try {
            c.getMutexSpace().get(new ActualField("bullets_lock"));
            if(c.getLastUpdate() == lastBulletUpdate){
                for (Bullet b : c.getBullets()) {
                    CannonController.moveBullet(b, deltaTime);
                }
            }

            for (Bullet b : c.getBullets()) {
                if(b.getTeam()){
                    g2D.drawImage(bulletred, (int) b.x, (int) b.y, (int) b.width, (int) b.height, null);
                } else {
                    g2D.drawImage(bulletblue, (int) b.x, (int) b.y, (int) b.width, (int) b.height, null);
                }
            }
            c.getMutexSpace().put("bullets_lock");
            lastBulletUpdate = c.getLastUpdate();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void paintWalls(){
        for (Wall w : c.getWalls()) {
            // Shield
            if(w.getHealth() > Wall.MAX_HEALTH){
                if(w.getTeam()){
                    g2D.drawImage(shieldred, (int) w.x, (int) w.y, (int) Wall.SHIELD_WIDTH, (int) Wall.SHIELD_HEIGHT, null);
                } else {
                    g2D.drawImage(shieldblue, (int) w.x, (int) w.y, (int) Wall.SHIELD_WIDTH, (int) Wall.SHIELD_HEIGHT, null);
                }
                continue;
            }

            // Walls
            g2D.drawImage(wallShadow, (int) w.x-2, (int) w.y, (int) w.width+4, (int) w.height+4, null);
            if(w.getTeam()){
                if (w.getHealth() == Wall.MAX_HEALTH){
                    g2D.drawImage(wallred1, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
                } else if(w.getHealth() >= 3){
                    g2D.drawImage(wallred2, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
                } else if(w.getHealth() > 0){
                    g2D.drawImage(wallred3, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
                }
            } else {
                if (w.getHealth() == Wall.MAX_HEALTH){
                    g2D.drawImage(wallblue1, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
                } else if(w.getHealth() >= 3){
                    g2D.drawImage(wallblue2, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
                } else if(w.getHealth() > 0){
                    g2D.drawImage(wallblue3, (int) w.x, (int) w.y, (int) w.width, (int) w.height, null);
                }
            }
        }
    }

    public void paintFortresses(){
        g2D.setFont(alagard);
        for (Fortress f : c.getFortresses()) {
            if (f.getTeam()) {
                g2D.drawImage(fortressredShadow, (int) f.x-6, (int) f.y, (int) f.width+12, (int) f.height+12, null);
                g2D.drawImage(fortressred, (int) f.x, (int) f.y, (int) f.width, (int) f.height, null);
                g2D.drawString("" + f.getHP(), (int) f.x + 30, (int) f.y + 217);
                g2D.drawString("" + f.getWood(), (int) f.x + 30, (int) f.y + 317);
                g2D.drawString("" + f.getIron(), (int) f.x + 30, (int) f.y + 417);
            } else {
                g2D.drawImage(fortressblueShadow, (int) f.x-6, (int) f.y, (int) f.width+12, (int) f.height+12, null);
                g2D.drawImage(fortressblue, (int) f.x, (int) f.y, (int) f.width, (int) f.height, null);
                g2D.drawString("" + f.getHP(), (int) f.x + 80, (int) f.y + 217);
                g2D.drawString("" + f.getWood(), (int) f.x + 80, (int) f.y + 317);
                g2D.drawString("" + f.getIron(), (int) f.x + 80, (int) f.y + 417);
            }
        }
    }

    public void paintResources(){
        for (Resource r : c.getResources()) {
            if (r.getType() == 0) {
                g2D.drawImage(woodShadow, (int) r.x-2, (int) r.y, 54, 54, null);
                g2D.drawImage(wood, (int) r.x, (int) r.y, (int) r.width, (int) r.height, null);
            }
            else {
                g2D.drawImage(ironShadow, (int) r.x-2, (int) r.y, 54, 54, null);
                g2D.drawImage(iron, (int) r.x, (int) r.y, (int) r.width, (int) r.height, null);
            }
        }
    }

    public void paintOrbs(){
        for (Orb o : c.getOrbs()) {
            g2D.drawImage(orbShadow, (int) o.x-2, (int) o.y, (int) o.width + 4, (int) o.height + 4, null);
            g2D.drawImage(orb, (int) o.x, (int) o.y, (int) o.width, (int) o.height, null);
        }
        for (OrbHolder oh : c.getOrbHolders()) {
            if (oh.hasOrb) {
                g2D.drawImage(orbholderfull, (int) oh.x, (int) oh.y, (int) oh.width, (int) oh.height, null);
            } else {
                g2D.drawImage(orbholderempty, (int) oh.x, (int) oh.y, (int) oh.width, (int) oh.height, null);
            }
        }
    }

    private void paintBuffs(){
        try {
            Object[] msg = c.getChannelFromServer().getp(new ActualField("buff_activated"), new FormalField(String.class), new FormalField(Boolean.class));
            buffMessageBox.update(deltaTime);
            if(msg != null){
                buffMessageBox.addMessage(((boolean)msg[2] ? "RED" : "BLUE") + " team got " + ((String) msg[1]).toUpperCase(Locale.ROOT) + "!");
            }
            buffMessageBox.paint(g2D);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void paintConnectionMessages(){
        connectionMessageBox.update(deltaTime);
        connectionMessageBox.paint(g2D);
    }

    private void paintScores(){
        g2D.setFont(alagard);
        String s = c.getTeam1Score() + " | " + c.getTeam2Score();
        g2D.drawString(s, Server.SCREEN_WIDTH/2 - g2D.getFontMetrics().stringWidth(s)/2, 40);
    }

    public void updatePanel() {
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {
        if (c.isGamePaused()) {
            return;
        }
        String input = getInput(e.getKeyCode());
        try {
            switch (input){
                case "left":
                case "right":
                case "down":
                case "up":
                    c.keyDown_Move(input);
                    break;
                case "createcannon":
                    c.keyDown_Cannon(input);
                    break;
                case "createwall":
                    c.keyDownWall(input);
                    break;
                case "dropOrb":
                    c.keyDown_DropOrb(input);
                default:
                    break;
            }
        } catch (InterruptedException e1) {e1.printStackTrace();}
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (c.isGamePaused()) {
            return;
        }
        String input = getInput(e.getKeyCode());
        try {
            switch (input){
                case "left":
                case "right":
                case "down":
                case "up":
                    c.keyUp_Move(input);
                    break;
                case "createcannon":
                    c.keyUp_Cannon();
                    break;
                case "createwall":
                    c.keyUp_Wall();
                    break;
                case "dropOrb":
                    c.keyUp_DropOrb();
                default:
                    break;
            }
        } catch (InterruptedException e1) {e1.printStackTrace();}
    }

    private String getInput(int keyCode){
        String input = "";
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                input = "left";
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                input = "right";
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                input = "down";
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                input = "up";
                break;
            case KeyEvent.VK_Q:
                input = "createcannon";
                break;
            case KeyEvent.VK_E:
                input = "createwall";
                break;
            case KeyEvent.VK_SPACE:
                input = "dropOrb";
            default:
                break;
        }
        return input;
    }

    public void clientDisconnected(String playerName) {
        connectionMessageBox.addMessage((playerName.trim().isEmpty() ? "A player" : playerName) + " has disconnected.");
    }

    public void clientConnected(String playerName) {
        connectionMessageBox.addMessage((playerName.trim().isEmpty() ? "A player" : playerName) + " has joined!");
    }
}
