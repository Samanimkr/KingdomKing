package game.rain;

import game.rain.entity.Player;
import game.rain.graphics.Screen;
import game.rain.input.Keyboard;
import game.rain.level.Level;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.LockSupport;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/** The original software renderer, with a paced loop and a desktop launcher. */
@SuppressWarnings("serial")
public final class Game extends Canvas implements Runnable {
    private static final long serialVersionUID = 1L;
    public static final int WIDTH = 300;
    public static final int HEIGHT = WIDTH * 9 / 16;
    public static final int SCALE = 3;
    public static final String TITLE = "KingdomKing";
    private static final long STEP_NANOS = 1_000_000_000L / 60;
    private static final int SPAWN_X = 20 * 16, SPAWN_Y = 21 * 16;

    private final Keyboard key = new Keyboard();
    private final Level level = Level.spawn;
    private final Screen screen = new Screen(WIDTH, HEIGHT);
    private final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    private Player player;
    private Thread thread;
    private JFrame frame;
    private volatile boolean running;
    private volatile boolean focused = true;
    private boolean paused;

    public Game() {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setIgnoreRepaint(true);
        resetPlayer();
        addKeyListener(key);
        addFocusListener(key);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) { focused = false; }
            @Override public void focusGained(FocusEvent event) { focused = true; }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { requestFocusInWindow(); }
        });
    }

    private void resetPlayer() {
        player = new Player(SPAWN_X, SPAWN_Y, key);
        player.init(level);
    }

    public synchronized void start() {
        if (thread != null && thread.isAlive()) return;
        running = true;
        thread = new Thread(this, "KingdomKing game loop");
        thread.start();
    }

    public void stop() {
        Thread stopping;
        synchronized (this) {
            running = false;
            stopping = thread;
        }
        if (stopping == null || stopping == Thread.currentThread()) return;
        stopping.interrupt();
        try {
            stopping.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        long accumulator = 0;
        try {
            while (running) {
                long now = System.nanoTime();
                // Cap catch-up after sleep, a debugger pause, or a stalled window.
                accumulator += Math.min(now - previous, STEP_NANOS * 5);
                previous = now;
                if (accumulator >= STEP_NANOS) {
                    while (accumulator >= STEP_NANOS && running) {
                        update();
                        accumulator -= STEP_NANOS;
                    }
                    if (running) {
                        render();
                    }
                }
                LockSupport.parkNanos(Math.max(1, STEP_NANOS - accumulator - (System.nanoTime() - now)));
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(frame, e.toString(), TITLE + " error", JOptionPane.ERROR_MESSAGE);
                if (frame != null) frame.dispose();
            });
        } finally {
            running = false;
        }
    }

    public void update() {
        synchronized (key) {
            key.update();
            if (key.consumePress(KeyEvent.VK_ESCAPE)) paused = !paused;
            if (key.consumePress(KeyEvent.VK_R)) resetPlayer();
            if (focused && !paused) {
                level.update();
                player.update();
            }
        }
    }

    /** Also used by the headless screenshot command to verify packaged artwork. */
    public BufferedImage renderFrame() {
        screen.clear();
        int xScroll = Math.max(0, Math.min(player.x - WIDTH / 2, level.getWidth() * 16 - WIDTH));
        int yScroll = Math.max(0, Math.min(player.y - HEIGHT / 2, level.getHeight() * 16 - HEIGHT));
        level.render(xScroll, yScroll, screen);
        player.render(screen);
        System.arraycopy(screen.pixels, 0, pixels, 0, pixels.length);
        return image;
    }

    public void render() {
        if (!isDisplayable()) return;
        BufferStrategy buffers = getBufferStrategy();
        if (buffers == null) return;
        renderFrame();
        do {
            do {
                Graphics2D graphics = (Graphics2D) buffers.getDrawGraphics();
                try {
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    graphics.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                    graphics.setColor(new Color(0, 0, 0, 170));
                    graphics.fillRect(12, 12, 116, 28);
                    graphics.setColor(Color.WHITE);
                    graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
                    graphics.drawString("X " + (player.x >> 4) + "  Y " + (player.y >> 4), 22, 31);
                    if (paused || !focused) {
                        graphics.setColor(new Color(0, 0, 0, 155));
                        graphics.fillRect(0, 0, getWidth(), getHeight());
                        graphics.setColor(Color.WHITE);
                        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
                        drawCentered(graphics, "PAUSED", getHeight() / 2 - 8);
                        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
                        drawCentered(graphics, !focused ? "Click the game to return" : "Press Esc to resume",
                                getHeight() / 2 + 26);
                    }
                } finally {
                    graphics.dispose();
                }
            } while (buffers.contentsRestored() && running);
            buffers.show();
            Toolkit.getDefaultToolkit().sync();
        } while (buffers.contentsLost() && running);
    }

    private void drawCentered(Graphics2D graphics, String text, int y) {
        graphics.drawString(text, (getWidth() - graphics.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private void openWindow() {
        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        frame.add(this, BorderLayout.CENTER);
        JLabel controls = new JLabel("WASD / arrows: move    •    Shift / Ctrl: sprint    •    Esc: pause    •    R: respawn",
                SwingConstants.CENTER);
        controls.setOpaque(true);
        controls.setBackground(new Color(23, 28, 24));
        controls.setForeground(new Color(221, 228, 214));
        controls.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        controls.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        frame.add(controls, BorderLayout.SOUTH);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                stop();
                frame.dispose();
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        createBufferStrategy(3);
        requestFocusInWindow();
        start();
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println("KingdomKing: WASD/arrows move, Shift/Ctrl sprint, Esc pause, R respawn.");
            System.out.println("Usage: java -jar KingdomKing.jar [--screenshot <file.png> | --help]");
            return;
        }
        if (args.length == 2 && args[0].equals("--screenshot")) {
            Path output = Path.of(args[1]).toAbsolutePath();
            Files.createDirectories(output.getParent());
            ImageIO.write(new Game().renderFrame(), "png", output.toFile());
            System.out.println("Rendered KingdomKing to " + output);
            return;
        }
        if (args.length != 0) {
            System.err.println("Unknown arguments. Use --help for usage.");
            System.exit(2);
        }
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("KingdomKing needs a graphical desktop. Use --screenshot <file.png> for a headless render.");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
            try {
                new Game().openWindow();
            } catch (RuntimeException | LinkageError e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, e.toString(), TITLE + " could not start", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
