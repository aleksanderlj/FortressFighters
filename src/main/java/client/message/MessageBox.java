package client.message;

import game.Server;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MessageBox {
    private int x;
    private int y;
    private double displayTime;
    private Font font;
    private boolean stackDown; // If true, will put each message under the next, otherwise it will put it above
    private boolean center; // If true, will use x as the middle of the box, otherwise x will be the left side of the box
    private List<Message> messages = new ArrayList<>();

    public MessageBox(int x, int y, double displayTime, Font font, boolean stackDown, boolean center){
        this.x = x;
        this.y = y;
        this.displayTime = displayTime;
        this.font = font;
        this.stackDown = stackDown;
        this.center = center;
    }

    public void update(){
        for (Message m : messages) {
            m.update();
        }
        messages.removeIf(m -> m.getDisplayTime() < 0);
    }

    public void paint(Graphics2D g2D){
        g2D.setFont(font);
        Message m;
        for(int i = 0 ; i < messages.size() ; i++){
            m = messages.get(i);
            g2D.drawString(m.getText(),
                    center ? x - (g2D.getFontMetrics().stringWidth(m.getText())/2) : x,
                    stackDown ? y + ( i * (g2D.getFontMetrics(font).getHeight() + 5) ) : y - ( i * (g2D.getFontMetrics(font).getHeight() - 5) ));
        }
    }

    public void addMessage(String message){
        messages.add(new Message(message, displayTime));
    }
}
