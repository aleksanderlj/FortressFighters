import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
	
    public static void main(String[] args) {
        frame = new GameFrame();
		mainPanel = new MainPanel();
        frame.setPanel(mainPanel);
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
            client = new Client("localhost", frame);
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
