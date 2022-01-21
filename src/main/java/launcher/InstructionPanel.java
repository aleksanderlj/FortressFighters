package launcher;

import game.GameFrame;
import game.Main;
import model.Cannon;
import model.Wall;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class InstructionPanel extends JPanel {
    GameFrame gameFrame;
    private BufferedImage background;
    private Font alagard;
    private JButton mainMenuButton;

    public InstructionPanel(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
        setPreferredSize(new Dimension(MenuPanel.WIDTH, MenuPanel.HEIGHT));
        setFocusable(true);
        setLayout(null);
        try {
            background = ImageIO.read(getClass().getClassLoader().getResource("launcherbackground.jpg"));
            alagard = Font.createFont(Font.TRUETYPE_FONT, Main.class.getClassLoader().getResourceAsStream("alagard.ttf"));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        // Create Components
        mainMenuButton = new LauncherButton("Main Menu", alagard);
        mainMenuButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {gameFrame.setPanel(new MenuPanel(gameFrame));}});
        mainMenuButton.setBounds(150, 400, 200, 50);

        // Add components
        add(mainMenuButton);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2D = (Graphics2D) g;
        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2D.drawImage(background,
                (int)((getWidth()/2) - ((background.getWidth() * MenuPanel.BACKGROUND_SCALE)/2)),
                -70,
                (int)(background.getWidth() * MenuPanel.BACKGROUND_SCALE),
                (int)(background.getHeight() * MenuPanel.BACKGROUND_SCALE),
                null);
        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        g2D.setColor(Color.WHITE);
        g2D.setFont(new Font("TimesRoman", Font.BOLD, 25));
        DrawUtility.drawStringWithShadow(g2D, "How to play:", 20, 40);
        DrawUtility.drawStringWithShadow(g2D, "Controls:", 20, 260);
        DrawUtility.drawStringWithShadow(g2D, "Costs:", 300, 260);

        g2D.setFont(new Font("TimesRoman", Font.BOLD, 15));
        DrawUtility.drawStringWithShadow(g2D, "- Collect wood and iron and return them to your fortress", 20, 70);
        DrawUtility.drawStringWithShadow(g2D, "- Use iron to build cannons that shoot projectiles towards the enemy", 20, 100);
        DrawUtility.drawStringWithShadow(g2D, "- Use wood to build walls that stop hostile projectiles", 20, 130);
        DrawUtility.drawStringWithShadow(g2D, "- Destroy the enemy fortress with your cannons", 20, 160);
        DrawUtility.drawStringWithShadow(g2D, "- Mystical things might happen if you return the orbs on the battle-", 20, 190);
        DrawUtility.drawStringWithShadow(g2D, "  field to your fortress' orb holders", 20, 210);

        DrawUtility.drawStringWithShadow(g2D, "- WASD or arrow keys to move", 20, 290);
        DrawUtility.drawStringWithShadow(g2D, "- Q to build a cannon", 20, 320);
        DrawUtility.drawStringWithShadow(g2D, "- E to build a wall", 20, 350);
        DrawUtility.drawStringWithShadow(g2D, "- SPACE to drop an orb", 20, 380);

        DrawUtility.drawStringWithShadow(g2D, "- Cannons cost " + Cannon.IRON_COST + " iron", 300, 290);
        DrawUtility.drawStringWithShadow(g2D, "- Walls cost " + Wall.WOOD_COST + " wood", 300, 320);
    }
}
