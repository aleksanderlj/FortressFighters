package game;
import client.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class GameFrame extends JFrame {
		public JPanel panel;
		public boolean isHost = false;
		
		public GameFrame() {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setTitle("Fortress Fighters");
			try {
				setIconImage(ImageIO.read(getClass().getClassLoader().getResource("orb.png")));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void setPanel(JPanel panel) {
			if (this.panel != null) {
				remove(this.panel);
			}
			this.panel = panel;
			if (panel instanceof GamePanel && isHost) {
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			}
			add(panel);
			pack();
			setLocationRelativeTo(null);
			setVisible(true);
		}
		
	}