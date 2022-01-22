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
    private JCheckBox fixedTeamsCheck;
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
        fixedTeamsCheck.setBounds(50, 50, 100, 25);
        fixedTeamsCheck.setOpaque(false);
        fixedTeamsCheck.setForeground(Color.WHITE);
        fixedTeamsCheck.setSelected(Settings.fixedTeams);
        fixedTeamsCheck.addItemListener(e -> Settings.fixedTeams = e.getStateChange() == ItemEvent.SELECTED);
        add(fixedTeamsCheck);

        fixedTeamsCheck = new JCheckBox("Allow uneven teams", false);
        fixedTeamsCheck.setBounds(50, 70, 150, 25);
        fixedTeamsCheck.setOpaque(false);
        fixedTeamsCheck.setForeground(Color.WHITE);
        fixedTeamsCheck.setSelected(Settings.unevenTeams);
        fixedTeamsCheck.addItemListener(e -> Settings.unevenTeams = e.getStateChange() == ItemEvent.SELECTED);
        add(fixedTeamsCheck);

        String[] teamOptions = new String[]{"None", "Blue", "Red"};
        preferredTeamBox = new JComboBox<>(teamOptions);
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
        DrawUtility.paintBackground(g2D, background);

        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g2D.fillRect(0, 0, MenuPanel.WIDTH, MenuPanel.HEIGHT);
        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        g2D.setFont(new Font("TimesRoman", Font.BOLD, 15));
        g2D.setColor(Color.WHITE);
        g2D.drawString("Server settings:", 50, 40);

        g2D.drawString("Personal:", 50, 140);

        g2D.setFont(new Font("TimesRoman", Font.BOLD, 12));
        g2D.drawString("Preferred team", 50, 165);
    }
}
