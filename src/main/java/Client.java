import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.Space;

import com.google.gson.internal.LinkedTreeMap;

public class Client {

	public static final double S_BETWEEN_UPDATES = 0.01;
	private TestFrame frame;
	private List<Rectangle.Double> players = new ArrayList<Rectangle.Double>();
	private Space centralSpace;
	private Space objectPositionsSpace;
	private Space playerMovementSpace;
	private int id;

	public Client() {
		try {
			centralSpace = new RemoteSpace("tcp://localhost:9001/central?keep");
			objectPositionsSpace = new RemoteSpace("tcp://localhost:9001/objectpositions?keep");
			playerMovementSpace = new RemoteSpace("tcp://localhost:9001/playermovement?keep");
			centralSpace.put("joined");
			id = (Integer) centralSpace.get(new FormalField(Integer.class))[0];
		} catch (IOException | InterruptedException e) {}
		frame = new TestFrame();
		new Thread(new Timer()).start();
	}

	public void update() {
		try {
			players = (List<Rectangle.Double>) objectPositionsSpace.query(new FormalField(List.class))[0];
		} catch (InterruptedException e) {}
		frame.updateFrame();
	}

	private class TestFrame extends JFrame implements KeyListener {
		public TestPanel panel;

		public TestFrame() {
			addKeyListener(this);
			panel = new TestPanel();
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.add(panel);
			this.pack();
			setLocationRelativeTo(null);
			setVisible(true);
		}

		public void updateFrame() {
			panel.updatePanel();
		}

		@Override
		public void keyTyped(KeyEvent e) {}
		@Override
		public void keyPressed(KeyEvent e) {
			String direction = getInput(e.getKeyCode());
			try {
				if (!direction.equals("") && playerMovementSpace.queryp(new ActualField(id), new ActualField(direction)) == null) {
					playerMovementSpace.put(id, direction);
				}
			} catch (InterruptedException e1) {}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			String direction = getInput(e.getKeyCode());
			try {
				playerMovementSpace.getp(new ActualField(id), new ActualField(direction));
			} catch (InterruptedException e1) {}
		}

		private String getInput(int keyCode){
			String direction = "";
			switch (keyCode) {
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_A:
					direction = "left";
					break;
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_D:
					direction = "right";
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S:
					direction = "down";
					break;
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W:
					direction = "up";
					break;
				default:
					break;
			}
			return direction;
		}
	}

	private class TestPanel extends JPanel {
		public Graphics2D g2D;

		public TestPanel() {
			setPreferredSize(new Dimension(500, 500));
		}

		public void paint(Graphics g) {
			super.paint(g);
			g2D = (Graphics2D) g;
			for (int i = 0; i < players.size(); i++) {
				LinkedTreeMap t = (LinkedTreeMap)((Object)players.get(i));
				g2D.drawRect((int)((double)t.get("x")), (int)((double)t.get("y")), (int)((double)t.get("width")), (int)((double)t.get("height")));
			}
		}

		public void updatePanel() {
			repaint();
		}
	}

	private class Timer implements Runnable {
		public void run() {
			try {
				while (true) {
					Thread.sleep((long)(S_BETWEEN_UPDATES*1000));
					update();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}