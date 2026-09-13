package game.rain.graphics;

import game.rain.level.tile.Tile;
import java.util.Arrays;

public class Screen {
    public final int width, height;
    public final int[] pixels;
    public int xOffset, yOffset;

    public Screen(int width, int height) {
        this.width = width;
        this.height = height;
        pixels = new int[width * height];
    }

    public void clear() { Arrays.fill(pixels, 0); }

    public void renderTile(int x, int y, Tile tile) {
        renderSprite(x, y, tile.sprite, 0, false);
    }

    public void renderPlayer(int x, int y, Sprite sprite, int flip) {
        renderSprite(x, y, sprite, flip, true);
    }

    private void renderSprite(int x, int y, Sprite sprite, int flip, boolean transparent) {
        int xp = x - xOffset, yp = y - yOffset;
        int size = sprite.SIZE;
        for (int sy = Math.max(0, -yp); sy < Math.min(size, height - yp); sy++) {
            int sourceY = (flip & 2) != 0 ? size - 1 - sy : sy;
            for (int sx = Math.max(0, -xp); sx < Math.min(size, width - xp); sx++) {
                int sourceX = (flip & 1) != 0 ? size - 1 - sx : sx;
                int color = sprite.pixels[sourceX + sourceY * size];
                if (!transparent || (color != 0xffff00ff && (color >>> 24) != 0)) {
                    pixels[xp + sx + (yp + sy) * width] = color;
                }
            }
        }
    }

    public void setOffset(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }
}
