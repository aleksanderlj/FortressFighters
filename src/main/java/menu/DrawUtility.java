package menu;

import java.awt.*;

public class DrawUtility {
    public static void drawStringWithShadow(Graphics2D g2D, String text, int x, int y) {
        g2D.setColor(Color.BLACK);
        g2D.drawString(text, x+2, y+2);
        g2D.setColor(Color.WHITE);
        g2D.drawString(text, x, y);
    }
}
