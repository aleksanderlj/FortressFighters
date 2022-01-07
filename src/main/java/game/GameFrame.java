package game;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class GameFrame extends JFrame {
		public JPanel panel;

		public GameFrame() {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		public void setPanel(JPanel panel) {
			if (this.panel != null) {
				remove(panel);
			}
			this.panel = panel;
			add(panel);
			pack();
			setLocationRelativeTo(null);
			setVisible(true);
		}
	}