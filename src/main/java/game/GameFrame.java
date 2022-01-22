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
			Settings.load();
			try {
				setIconImage(ImageIO.read(getClass().getClassLoader().getResource("orb.png")));
			} catch (Exception e) {
				e.printStackTrace();
			}
			setLocationRelativeTo(null);
		}
		
		public void setPanel(JPanel panel) {
			if (this.panel != null) {
				remove(this.panel);
			}
			if (panel instanceof GamePanel && isHost) {
				setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			}
			add(panel);
			pack();
			setVisible(true);
			
			if(this.panel == null){
				setLocationRelativeTo(null); // put in center of screen when opened for the first time
			}
			this.panel = panel;
		}
		
	}