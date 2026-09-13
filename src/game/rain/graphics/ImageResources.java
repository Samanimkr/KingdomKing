package game.rain.graphics;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Loads bundled artwork from an IDE, a JAR, or a packaged app. */
public final class ImageResources {
    private ImageResources() {}

    public static BufferedImage load(String path) {
        try (InputStream input = ImageResources.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing game resource: " + path
                        + ". Run the game with its bundled resources (see README.MD).");
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IllegalStateException("Unsupported game image: " + path);
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read game image: " + path, e);
        }
    }
}
