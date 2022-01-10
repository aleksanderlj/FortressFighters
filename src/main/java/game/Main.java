package game;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Scanner;

import javax.swing.JPanel;

import org.jspace.ActualField;

import model.Player;

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
        tf = new TextField("localhost");
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
            client = new Client("localhost", frame);
    	}
    	else {
            client = new Client(tf.getText(), frame);
    	}
    }
    
    private static class MainPanel extends JPanel implements KeyListener {
		public Graphics2D g2D;

		public MainPanel() {
			setPreferredSize(new Dimension(windowWidth, windowHeight));
			addKeyListener(this);
	        setFocusable(true);
		}

		public void paint(Graphics g) {
			super.paint(g);
			g2D = (Graphics2D) g;
			g2D.drawString("Press 1 to host and play or 2 to join", 150, 50);
			g2D.drawString("Enter Address to join:", 190, 235);
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
