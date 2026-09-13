package game.rain.input;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class Keyboard implements KeyListener, FocusListener {
    // AWT writes on the event thread; the game takes a synchronized snapshot.
    private final Set<Integer> keys = new HashSet<>();
    private final Set<Integer> presses = new HashSet<>();
    public boolean up, down, left, right, dash;

    public synchronized void update() {
        up = keys.contains(KeyEvent.VK_UP) || keys.contains(KeyEvent.VK_W);
        down = keys.contains(KeyEvent.VK_DOWN) || keys.contains(KeyEvent.VK_S);
        left = keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_A);
        right = keys.contains(KeyEvent.VK_RIGHT) || keys.contains(KeyEvent.VK_D);
        dash = keys.contains(KeyEvent.VK_SHIFT) || keys.contains(KeyEvent.VK_CONTROL);
    }

    public synchronized boolean consumePress(int keyCode) {
        return presses.remove(keyCode);
    }

    public synchronized void clear() {
        keys.clear();
        presses.clear();
        up = down = left = right = dash = false;
    }

    @Override
    public synchronized void keyPressed(KeyEvent event) {
        if (keys.add(event.getKeyCode())) presses.add(event.getKeyCode());
    }

    @Override
    public synchronized void keyReleased(KeyEvent event) {
        keys.remove(event.getKeyCode());
    }

    @Override public void keyTyped(KeyEvent event) {}
    @Override public void focusLost(FocusEvent event) { clear(); }
    @Override public void focusGained(FocusEvent event) {}
}
