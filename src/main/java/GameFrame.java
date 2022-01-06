import javax.swing.JFrame;
import javax.swing.JPanel;

public class GameFrame extends JFrame {
		public JPanel panel;

		public GameFrame() {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		
		public void setPanel(JPanel panel) {
			if (this.panel != null) {
				System.out.println(panel);
				remove(panel);
			}
			this.panel = panel;
			System.out.println(panel);
			add(panel);
			pack();
			setLocationRelativeTo(null);
			setVisible(true);
		}
	}