package game.rain;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** Present each game pixel as a whole square of display pixels, including on HiDPI screens. */
public final class PixelViewport {
    private PixelViewport() {}

    public static void draw(Graphics2D target, BufferedImage image, int width, int height) {
        Graphics2D g = (Graphics2D) target.create();
        try {
            AffineTransform device = g.getTransform();
            int physicalWidth = (int) Math.round(width * device.getScaleX());
            int physicalHeight = (int) Math.round(height * device.getScaleY());
            int scale = Math.max(1, Math.min(physicalWidth / image.getWidth(), physicalHeight / image.getHeight()));
            int drawnWidth = image.getWidth() * scale;
            int drawnHeight = image.getHeight() * scale;
            // Work in actual device pixels, not fractional logical coordinates on scaled monitors.
            g.setTransform(new AffineTransform(1, 0, 0, 1,
                    Math.round(device.getTranslateX()), Math.round(device.getTranslateY())));
            g.setColor(new Color(0x091322));
            g.fillRect(0, 0, physicalWidth, physicalHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.drawImage(image, (physicalWidth - drawnWidth) / 2, (physicalHeight - drawnHeight) / 2,
                    drawnWidth, drawnHeight, null);
        } finally {
            g.dispose();
        }
    }
}
