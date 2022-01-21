package game;

import menu.MenuPanel;

public class Main {
	public static void main(String[] args) {
		GameFrame frame = new GameFrame();
        frame.setPanel(new MenuPanel(frame));
    }
}
