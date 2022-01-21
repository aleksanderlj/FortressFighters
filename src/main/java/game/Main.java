package game;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;

import launcher.LauncherButton;
import menu.MenuPanel;
import model.Cannon;
import model.Wall;

public class Main {
	public static void main(String[] args) {
		GameFrame frame = new GameFrame();
        frame.setPanel(new MenuPanel(frame));
    }
}
