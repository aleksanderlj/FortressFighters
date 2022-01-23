package client.message;

import client.Client;

public class Message {
    private String text;
    private double displayTime;

    public Message(String text, double displayTime){
        this.text = text;
        this.displayTime = displayTime;
    }

    public void update(long deltaTime){
        displayTime -= ((double)deltaTime)/1000;
    }

    public double getDisplayTime() {
        return displayTime;
    }

    public String getText() {
        return text;
    }
}