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

public class launcherButton extends JButton {
	public launcherButton(String text, Font font) {
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

class RoundedBorder implements Border {
	    private int r;
	    
	    RoundedBorder(int r) {
	        this.r = r;
	    }
	    
	    @Override
	    public Insets getBorderInsets(Component c) {
	        return new Insets(this.r+1, this.r+1, this.r+2, this.r);
	    }
	    
	    @Override
	    public boolean isBorderOpaque() {
	        return true;
	    }
	    
	    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
	    	for (int i = 0; i < r; i++) {
		        g.drawRoundRect(x, y, width-1, height-1, i, i);
	    	}
	    }
	}