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
        ipTextField.setBounds(270, 310, 150, 25);
        nameTextField.setBounds(140, 90, 220, 25);
        
        // Buttons
        instructionButton = createButton("Instructions");
		mainMenuButton = createButton("Main Menu");
        hostButton = createButton("Host Game");
        joinButton = createButton("Join Game");
        
        instructionButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {showInstructions();}});
        mainMenuButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {hideInstructions();}});
        hostButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {start(1);}});
        joinButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {start(2);}});
        
        instructionButton.setBounds(150, 400, 200, 50);
        mainMenuButton.setBounds(150, 400, 200, 50);
        hostButton.setBounds(70, 240, 150, 50);
        joinButton.setBounds(270, 240, 150, 50);
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
            server = new Server();
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
//				g2D.setFont(alagard.deriveFont(Font.PLAIN, 15));
				g2D.setFont(new Font("TimesRoman", Font.BOLD, 25));
				g2D.drawString("How to play:", 20, 40);
				g2D.drawString("Controls:", 20, 290);
				
//				g2D.setFont(alagard.deriveFont(Font.PLAIN, 15));
				g2D.setFont(new Font("TimesRoman", Font.BOLD, 15));
				g2D.drawString("- Collect wood and iron and return them to your fortress", 20, 70);
				g2D.drawString("- Use iron to build cannons that shoot projectiles towards the enemy", 20, 100);
				g2D.drawString("- Use wood to build walls that stop hostile projectiles", 20, 130);
				g2D.drawString("- Destroy the enemy fortress with your cannons", 20, 160);
				g2D.drawString("- Mystical things might happen if you return the orbs on the battle", 20, 190);
				g2D.drawString("  field to your fortress' orb holders", 20, 210);
				
				g2D.drawString("- WASD or arrow keys to move", 20, 320);
				g2D.drawString("- Q to build a cannon", 20, 350);
				g2D.drawString("- E to build a wall", 20, 380);
			} else {
				g2D.drawImage(background, (int)((getWidth()/2) - ((background.getWidth()*backgroundScale)/2)), -70, (int)(background.getWidth() * backgroundScale), (int)(background.getHeight()*backgroundScale), null);
				g2D.setColor(Color.WHITE);
				g2D.setFont(alagard.deriveFont(Font.BOLD, 26));
				g2D.drawString("Enter your name", 140, 80);
				g2D.setFont(alagard.deriveFont(Font.BOLD, 35));
				g2D.drawString("Play Game", 160, 210);
				g2D.setFont(alagard.deriveFont(Font.BOLD, 12));
				g2D.drawString("Enter IP Address to join:", 270, 305);
				g2D.setColor(Color.BLACK);
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
    
	private static JButton createButton(String text) {
		JButton button = new JButton(text);
		button.setFont(alagard.deriveFont(Font.BOLD, 20));
		button.setBackground(new Color(219, 180, 132));
		return button;
	}
}
