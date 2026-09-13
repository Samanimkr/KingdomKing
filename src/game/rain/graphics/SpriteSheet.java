package game.rain.graphics;

import java.awt.image.BufferedImage;

public class SpriteSheet {
    public final int SIZE;
    public final int[] pixels;
    public static final SpriteSheet tiles = new SpriteSheet("/textures/spritesheet.png", 256);
    public static final SpriteSheet spawn_level = new SpriteSheet("/textures/spawn_level.png", 48);

    public SpriteSheet(String path, int size) {
        SIZE = size;
        BufferedImage image = ImageResources.load(path);
        if (image.getWidth() != size || image.getHeight() != size) {
            throw new IllegalStateException("Sprite sheet " + path + " must be " + size + "x" + size);
        }
        pixels = image.getRGB(0, 0, size, size, null, 0, size);
    }
}
