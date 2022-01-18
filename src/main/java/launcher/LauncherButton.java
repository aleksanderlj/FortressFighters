package launcher;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class LauncherButton extends JButton {
	public LauncherButton(String text, Font font) {
		super(text);
		setFont(font.deriveFont(Font.BOLD, 20));
		setBackground(new Color(220, 180, 130));
		setBorder(new LineBorder(Color.BLACK, 2));
		
		addMouseListener(new java.awt.event.MouseAdapter() {
		    public void mouseEntered(java.awt.event.MouseEvent evt) {
				setBackground(new Color(245, 215, 170));
		    }

		    public void mouseExited(java.awt.event.MouseEvent evt) {
				setBackground(new Color(220, 180, 130));
		    }
		});
	}
}
