package game;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class Main {
	private static int windowWidth = 500;
	private static int windowHeight = 500;
	private static GameFrame frame;
	private static MainPanel mainPanel;
	private static TextField tf;
	private static Button hostButton;
	private static Button joinButton;
	
    public static void main(String[] args) {
        frame = new GameFrame();
		mainPanel = new MainPanel();
        frame.setPanel(mainPanel);
        mainPanel.setLayout(null);
        tf = new TextField(Server.getIP());
        tf.setBounds(200, 250, 100, 25);
        hostButton = new Button("Host");
        joinButton = new Button("Join");
        hostButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {start(1);}});
        joinButton.addActionListener(new ActionListener() {public void actionPerformed (ActionEvent e) {start(2);}});
        hostButton.setBounds(200, 100, 100, 25);
        joinButton.setBounds(200, 280, 100, 25);
        mainPanel.add(tf);
        mainPanel.add(hostButton);
        mainPanel.add(joinButton);
    }
    
    private static void start(int type) {
		frame.setVisible(false);
        frame.remove(mainPanel);
		Client client;
        Server server;

    	if (type == 1) {
            server = new Server();
            frame.isHost = true;
            client = new Client(Server.getIP(), frame);
			System.out.println(Server.getIP());
    	}
    	else {
            frame.isHost = false;
            client = new Client(tf.getText(), frame);
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

		public void paint(Graphics g) {
			super.paint(g);
			g2D = (Graphics2D) g;
			g2D.drawImage(background, (int)((getWidth()/2) - ((background.getWidth()*backgroundScale)/2)), -70, (int)(background.getWidth() * backgroundScale), (int)(background.getHeight()*backgroundScale), null);
			g2D.setColor(Color.WHITE);
			g2D.drawString("Press 1 to host and play or 2 to join", 150, 50);
			g2D.drawString("Enter Address to join:", 190, 235);
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
	}
}
