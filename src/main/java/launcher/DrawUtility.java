package launcher;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DrawUtility {
    private static double backgroundScale = -1;

    public static void drawStringWithShadow(Graphics2D g2D, String text, int x, int y) {
        g2D.setColor(Color.BLACK);
        g2D.drawString(text, x+2, y+2);
        g2D.setColor(Color.WHITE);
        g2D.drawString(text, x, y);
    }

    public static void paintBackground(Graphics2D g2D, BufferedImage background){
        if(backgroundScale < 0){ // Make sure not to calculate on each update
            backgroundScale = MenuPanel.WIDTH/(double)background.getWidth();
        }

        int width = (int)(background.getWidth() * backgroundScale);
        g2D.drawImage(background,
                (int)((MenuPanel.WIDTH/2) - (width/2)),
                -70,
                width,
                (int)(background.getHeight() * backgroundScale),
                null);
    }
}
