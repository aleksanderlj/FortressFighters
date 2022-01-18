package game;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import launcher.LauncherButton;
import model.Cannon;
import model.Wall;

public class Main {
	private static int windowWidth = 500;
	private static int windowHeight = 500;
	private static GameFrame frame;
	private static MainPanel mainPanel;
	private static TextField ipTextField;
	private static TextField nameTextField;
	private static JButton instructionButton;
	private static JButton hostButton;
	private static JButton joinButton;
	private static JButton mainMenuButton;
	private static Font alagard;
	private static boolean instructions = false;
	
    public static void main(String[] args) {
		try {
			alagard = Font.createFont(Font.TRUETYPE_FONT, Main.class.getClassLoader().getResourceAsStream("alagard.ttf"));
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
        frame = new GameFrame();
		mainPanel = new MainPanel();
        frame.setPanel(mainPanel);
        mainPanel.setLayout(null);
        
        createComponents();
        addComponents(mainPanel);
        mainPanel.repaint();
    }
    
	private static void createComponents() {	
		// Textfields
        ipTextField = new TextField(Server.getIP());
        nameTextField = new TextField();
        ipTextField.setFont(alagard.deriveFont(Font.PLAIN, 15));
        nameTextField.setFont(alagard.deriveFont(Font.PLAIN, 15));
        ipTextField.setBounds(270, 320, 150, 25);
        nameTextField.setBounds(140, 100, 220, 25);
        
        // Buttons
        instructionButton = new LauncherButton("How To Play", alagard);
		mainMenuButton = new LauncherButton("Main Menu", alagard);
        hostButton = new LauncherButton("Host Game", alagard);
        joinButton = new LauncherButton("Join Game", alagard);
        
        instructionButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {showInstructions();}});
        mainMenuButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {hideInstructions();}});
        hostButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {start(1);}});
        joinButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {start(2);}});
        
        instructionButton.setBounds(150, 400, 200, 50);
        mainMenuButton.setBounds(150, 400, 200, 50);
        hostButton.setBounds(70, 250, 150, 50);
        joinButton.setBounds(270, 250, 150, 50);
	}
	
	private static void addComponents(MainPanel mainPanel) {
        mainPanel.add(ipTextField);
        mainPanel.add(nameTextField);
        mainPanel.add(instructionButton);
        mainPanel.add(hostButton);
        mainPanel.add(joinButton);
	}
	
	private static void removeComponents(MainPanel mainPanel) {
        mainPanel.remove(ipTextField);
        mainPanel.remove(nameTextField);
        mainPanel.remove(instructionButton);
        mainPanel.remove(hostButton);
        mainPanel.remove(joinButton);
	}
	
	private static void showInstructions() {
		instructions = true;
		removeComponents(mainPanel);
        mainPanel.add(mainMenuButton);
		mainPanel.repaint();
	}
	
	public static void hideInstructions() {
		instructions = false;
		mainPanel.remove(mainMenuButton);
		addComponents(mainPanel);
		mainPanel.repaint();
	}
    
    private static void start(int type) {
		frame.setVisible(false);
        frame.remove(mainPanel);
		Client client;
        Server server;

    	if (type == 1) {
            server = new Server(true);
            frame.isHost = true;
            client = new Client(Server.getIP(), frame, nameTextField.getText());
			System.out.println(Server.getIP());
    	}
    	else {
            frame.isHost = false;
            client = new Client(ipTextField.getText(), frame, nameTextField.getText());
    	}
    }
    
    private static class MainPanel extends JPanel implements KeyListener {
		public Graphics2D g2D;
		private BufferedImage background;
		private double backgroundScale = 1.3;

		public MainPanel() {
			setPreferredSize(new Dimension(windowWidth, windowHeight));
			addKeyListener(this);
	        setFocusable(true);
			try {
				background = ImageIO.read(getClass().getClassLoader().getResource("launcherbackground.jpg"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g2D = (Graphics2D) g;
			if (instructions) {
				g2D.drawImage(background, (int)((getWidth()/2) - ((background.getWidth()*backgroundScale)/2)), -70, (int)(background.getWidth() * backgroundScale), (int)(background.getHeight()*backgroundScale), null);
				
				g2D.setColor(Color.WHITE);
				g2D.setFont(new Font("TimesRoman", Font.BOLD, 25));
				drawStringWithShadow(g2D, "How to play:", 20, 40);
				drawStringWithShadow(g2D, "Controls:", 20, 290);
				drawStringWithShadow(g2D, "Costs:", 300, 290);

				g2D.setFont(new Font("TimesRoman", Font.BOLD, 15));
				drawStringWithShadow(g2D, "- Collect wood and iron and return them to your fortress", 20, 70);
				drawStringWithShadow(g2D, "- Use iron to build cannons that shoot projectiles towards the enemy", 20, 100);
				drawStringWithShadow(g2D, "- Use wood to build walls that stop hostile projectiles", 20, 130);
				drawStringWithShadow(g2D, "- Destroy the enemy fortress with your cannons", 20, 160);
				drawStringWithShadow(g2D, "- Mystical things might happen if you return the orbs on the battle-", 20, 190);
				drawStringWithShadow(g2D, "  field to your fortress' orb holders", 20, 210);
				
				drawStringWithShadow(g2D, "- WASD or arrow keys to move", 20, 320);
				drawStringWithShadow(g2D, "- Q to build a cannon", 20, 350);
				drawStringWithShadow(g2D, "- E to build a wall", 20, 380);
				
				drawStringWithShadow(g2D, "- Cannons cost " + Cannon.IRON_COST + " iron", 300, 320);
				drawStringWithShadow(g2D, "- Walls cost " + Wall.WOOD_COST + " wood", 300, 350);
			} else {
				g2D.drawImage(background, (int)((getWidth()/2) - ((background.getWidth()*backgroundScale)/2)), -70, (int)(background.getWidth() * backgroundScale), (int)(background.getHeight()*backgroundScale), null);
				g2D.setColor(Color.WHITE);
				g2D.setFont(alagard.deriveFont(Font.BOLD, 26));
				drawStringWithShadow(g2D, "Enter your name", 140, 90);
				g2D.setFont(alagard.deriveFont(Font.BOLD, 35));
				drawStringWithShadow(g2D, "Fortress Fighters", 100, 40);
				drawStringWithShadow(g2D, "Play Game", 160, 220);
				g2D.setFont(alagard.deriveFont(Font.BOLD, 12));
				drawStringWithShadow(g2D, "Enter IP Address to join:", 270, 315);
			}
			g2D.setColor(Color.BLACK);
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
		
		public void drawStringWithShadow(Graphics2D g2D, String text, int x, int y) {
			g2D.setColor(Color.BLACK);
			g2D.drawString(text, x+2, y+2);
			g2D.setColor(Color.WHITE);
			g2D.drawString(text, x, y);
		}
	}
}
