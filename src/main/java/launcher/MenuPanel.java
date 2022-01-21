package launcher;

import client.Client;
import game.GameFrame;
import game.Main;
import game.Server;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class MenuPanel extends JPanel implements KeyListener {
    GameFrame gameFrame;
    public Graphics2D g2D;
    private BufferedImage background;
    public static final double BACKGROUND_SCALE = 1.3;
    public static final int WIDTH = 500;
    public static final int HEIGHT = 500;
    private TextField ipTextField;
    private TextField nameTextField;
    private JButton instructionButton;
    private JButton hostButton;
    private JButton joinButton;
    private JButton settingsButton;
    private Font alagard;


    public MenuPanel(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        addKeyListener(this);
        setFocusable(true);
        setLayout(null);
        try {
            background = ImageIO.read(getClass().getClassLoader().getResource("launcherbackground.jpg"));
            alagard = Font.createFont(Font.TRUETYPE_FONT, Main.class.getClassLoader().getResourceAsStream("alagard.ttf"));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }

        // Create Components
        // Textfields
        ipTextField = new TextField(Server.getIP());
        nameTextField = new TextField();
        ipTextField.setFont(alagard.deriveFont(Font.PLAIN, 15));
        nameTextField.setFont(alagard.deriveFont(Font.PLAIN, 15));
        ipTextField.setBounds(270, 320, 150, 25);
        nameTextField.setBounds(140, 100, 220, 25);

        // Buttons
        instructionButton = new LauncherButton("How To Play", alagard);
        hostButton = new LauncherButton("Host Game", alagard);
        joinButton = new LauncherButton("Join Game", alagard);
        settingsButton = new LauncherButton("Settings", alagard);

        instructionButton.addActionListener(e -> gameFrame.setPanel(new InstructionPanel(gameFrame)));
        hostButton.addActionListener(e -> start(1));
        joinButton.addActionListener(e -> start(2));
        settingsButton.addActionListener(e -> gameFrame.setPanel(new SettingsPanel(gameFrame)));

        instructionButton.setBounds(150, 400, 200, 50);
        hostButton.setBounds(70, 250, 150, 50);
        joinButton.setBounds(270, 250, 150, 50);
        settingsButton.setBounds(150, 450, 200, 50);

        // Add components
        add(ipTextField);
        add(nameTextField);
        add(instructionButton);
        add(hostButton);
        add(joinButton);
        add(settingsButton);

        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g2D = (Graphics2D) g;
        g2D.drawImage(background, (int)((getWidth()/2) - ((background.getWidth()*BACKGROUND_SCALE)/2)), -70, (int)(background.getWidth() * BACKGROUND_SCALE), (int)(background.getHeight()*BACKGROUND_SCALE), null);
        g2D.setColor(Color.WHITE);
        g2D.setFont(alagard.deriveFont(Font.BOLD, 26));
        DrawUtility.drawStringWithShadow(g2D, "Enter your name", 140, 90);
        g2D.setFont(alagard.deriveFont(Font.BOLD, 35));
        DrawUtility.drawStringWithShadow(g2D, "Fortress Fighters", 100, 40);
        DrawUtility.drawStringWithShadow(g2D, "Play Game", 160, 220);
        g2D.setFont(alagard.deriveFont(Font.BOLD, 12));
        DrawUtility.drawStringWithShadow(g2D, "Enter IP Address to join:", 270, 315);
        g2D.setColor(Color.BLACK);
    }

    private void start(int type) {
        gameFrame.setVisible(false);
        gameFrame.remove(this);

        if (type == 1) {
            new Server(true);
            gameFrame.isHost = true;
            new Client(Server.getIP(), gameFrame, nameTextField.getText());
            System.out.println(Server.getIP());
        }
        else {
            gameFrame.isHost = false;
            new Client(ipTextField.getText(), gameFrame, nameTextField.getText());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_1) {
            start(1);
        }
        else if (e.getKeyCode() == KeyEvent.VK_2) {
            start(2);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}