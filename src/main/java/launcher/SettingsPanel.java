package launcher;

import game.GameFrame;
import game.Main;
import game.Settings;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class SettingsPanel extends JPanel {
    private BufferedImage background;
    private Font alagard;
    private JButton mainMenuButton;
    private JCheckBox fixedTeamsCheck;

    public SettingsPanel(GameFrame gameFrame){
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
        mainMenuButton.addActionListener(e -> gameFrame.setPanel(new MenuPanel(gameFrame)));
        mainMenuButton.setBounds(150, 400, 200, 50);
        add(mainMenuButton);

        fixedTeamsCheck = new JCheckBox("Fixed teams", false);
        fixedTeamsCheck.setBounds(50, 50, 100, 25);
        fixedTeamsCheck.setOpaque(false);
        fixedTeamsCheck.setForeground(Color.BLACK);
        fixedTeamsCheck.setSelected(Settings.fixedTeams);
        fixedTeamsCheck.addItemListener(e -> Settings.fixedTeams = e.getStateChange() == ItemEvent.SELECTED);
        add(fixedTeamsCheck);

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
    }
}
