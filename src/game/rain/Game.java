package game.rain;

import game.rain.entity.Player;
import game.rain.graphics.Screen;
import game.rain.input.Keyboard;
import game.rain.input.Mouse;
import game.rain.level.Level;
import game.rain.level.TileCoordinate;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;

public class Game extends Canvas implements Runnable {
	private static final long serialVersionUID = 1L;

	public static int width = 300;
	public static int height = width / 16 * 9;
	public static int scale = 3;
	public static String Title = "Rain 2D Game";

	private Thread thread; //sets threads so multiple actions are done at the same time
	private JFrame frame;
	private Keyboard key;
	private Level level;
	private Player player;
	private boolean running = false;

	private Screen screen;

	private BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

	public Game() {
		Dimension size = new Dimension(width * scale, height * scale);
		setPreferredSize(size);

		screen = new Screen(width, height);
		frame = new JFrame(); //creative the JFrame
		key = new Keyboard(); //setting up the keyboard
		level = Level.spawn;
		TileCoordinate PlayerSpawn = new TileCoordinate(20, 21);
		player = new Player(PlayerSpawn.x(), PlayerSpawn.y(), key);
		player.init(level);
		
		addKeyListener(key);
		
		Mouse mouse = new Mouse();
		addMouseListener(mouse);
		addMouseMotionListener(mouse);		
	}

	//starts the threads
	public synchronized void start() {
		running = true;
		thread = new Thread(this, "Display"); //starts game on a thread
		thread.start();
	}

	//stops the thread - if not stopped then it can keep going after applet is closed
	public synchronized void stop() {
		running = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	//While running loop - needs to keep doing instructions or it will close
	public void run() {
		long lastTime = System.nanoTime();
		long timer = System.currentTimeMillis();
		final double ns = 1000000000.0 / 60.0;
		double delta = 0;
		int frames = 0;
		int updates = 0;
		requestFocus();
		while (running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns; //adds difference between lastTime and now to delta
			lastTime = now;
			while (delta >= 1) {
				update();
				updates++;
				delta--;
			}
			render();
			frames++;

			if (System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				frame.setTitle(Title + " - " + updates + " ups, " + frames + " fps");
				frames = 0;
				updates = 0;
			}
		}
		stop();
	}

	public void update() {
		key.update();
		player.update();
	}

	public void render() {
		//creates place for image to be rendered behind screen then shown
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(3); //3 "screens"
			return;
		}

		screen.clear();
		int xScroll = player.x - screen.width / 2;
		int yScroll = player.y - screen.height / 2;
		level.render(xScroll, yScroll, screen);
		player.render(screen);

		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = screen.pixels[i];
		}

		Graphics g = bs.getDrawGraphics(); //linking buffer with graphics
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		g.setColor(Color.WHITE);
		g.setFont(new Font("Verdena", 0, 20));
		g.drawString("X: " + (player.x >> 4) + ", Y: " + (player.y >> 4), 0, 16);
		g.dispose(); //remove graphics that are not used
		bs.show(); //swapping out the buffers
	}

	//Start point - what happens when game starts
	public static void main(String[] args) {
		Game game = new Game();
		game.frame.setResizable(false); //Can screw up graphics if resizable
		game.frame.setTitle(Game.Title);
		game.frame.add(game); //adding "game" to window
		game.frame.pack();
		game.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //Ends anything related to program
		game.frame.setLocationRelativeTo(null);
		game.frame.setVisible(true);

		game.start();
	}

}
