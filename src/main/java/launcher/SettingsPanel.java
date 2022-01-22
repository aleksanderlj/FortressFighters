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
import java.util.Arrays;

public class SettingsPanel extends JPanel {
    private BufferedImage background;
    private Font alagard;
    private JButton mainMenuButton;
    private JCheckBox fixedTeamsCheck, unevenTeamsCheck;
    private JComboBox<String> preferredTeamBox;

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
        mainMenuButton.addActionListener(e -> {
            Settings.save();
            gameFrame.setPanel(new MenuPanel(gameFrame));
        });
        mainMenuButton.setBounds(150, 400, 200, 50);
        add(mainMenuButton);

        fixedTeamsCheck = new JCheckBox("Fixed teams", false);
        fixedTeamsCheck.setToolTipText("Make teams not shuffle after a game ends.");
        fixedTeamsCheck.setBounds(50, 50, 200, 25);
        fixedTeamsCheck.setOpaque(false);
        fixedTeamsCheck.setForeground(Color.WHITE);
        fixedTeamsCheck.setFont(new Font("Arial", Font.BOLD, 15));
        fixedTeamsCheck.setSelected(Settings.fixedTeams);
        fixedTeamsCheck.addItemListener(e -> Settings.fixedTeams = e.getStateChange() == ItemEvent.SELECTED);
        add(fixedTeamsCheck);

        unevenTeamsCheck = new JCheckBox("Allow uneven teams", false);
        unevenTeamsCheck.setToolTipText("The game won't try to fill teams fairly, and players will always end up on their preferred team.");
        unevenTeamsCheck.setBounds(50, 70, 200, 25);
        unevenTeamsCheck.setOpaque(false);
        unevenTeamsCheck.setForeground(Color.WHITE);
        unevenTeamsCheck.setFont(new Font("Arial", Font.BOLD, 15));
        unevenTeamsCheck.setSelected(Settings.unevenTeams);
        unevenTeamsCheck.addItemListener(e -> Settings.unevenTeams = e.getStateChange() == ItemEvent.SELECTED);
        add(unevenTeamsCheck);

        String[] teamOptions = new String[]{"None", "Blue", "Red"};
        preferredTeamBox = new JComboBox<>(teamOptions);
        preferredTeamBox.setToolTipText("The game will try to put you on your preferred team if the server settings allow it. Otherwise it is randomized.");
        preferredTeamBox.setBounds(50, 170, 75, 25);
        preferredTeamBox.setSelectedIndex(Arrays.asList(teamOptions).indexOf(Settings.preferredTeam));
        preferredTeamBox.addActionListener(e -> Settings.preferredTeam = ((JComboBox<String>)e.getSource()).getSelectedItem().toString());
        add(preferredTeamBox);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2D = (Graphics2D) g;
        //g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        DrawUtility.paintBackground(g2D, background, true);

        g2D.setFont(new Font("Arial", Font.BOLD, 18));
        g2D.setColor(Color.WHITE);
        g2D.drawString("Server settings:", 50, 40);

        g2D.drawString("Personal:", 50, 140);

        g2D.setFont(new Font("Arial", Font.BOLD, 14));
        g2D.drawString("Preferred team", 50, 165);
    }
}
