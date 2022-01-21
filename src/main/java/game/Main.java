package game;

import launcher.MenuPanel;

public class Main {
	public static void main(String[] args) {
		GameFrame frame = new GameFrame();
        frame.setPanel(new MenuPanel(frame));
    }
}
