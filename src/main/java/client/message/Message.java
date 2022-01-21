package client.message;

import client.Client;

public class Message {
    private String text;
    private double displayTime;

    public Message(String text, double displayTime){
        this.text = text;
        this.displayTime = displayTime;
    }

    public void update(){
        displayTime -= Client.S_BETWEEN_UPDATES;
    }

    public double getDisplayTime() {
        return displayTime;
    }

    public String getText() {
        return text;
    }
}