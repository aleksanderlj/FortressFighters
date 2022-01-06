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
	
    public static void main(String[] args) {
        frame = new GameFrame();
        frame.setPanel(new MainPanel());
    }
    
    private static void start(int type) {
        Client client;
        Server server;
    	if (type == 1) {
            server = new Server();
            System.out.println(server.getAddress());
            client = new Client("localhost", frame);
    	}
    	else {
            System.out.println("Connect to address:");
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
