package game.rain.level;

import game.rain.graphics.ImageResources;
import game.rain.graphics.Screen;
import game.rain.level.tile.Tile;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class Level {
    protected int width, height;
    protected int[] tiles;
    public static final Level spawn = new SpawnLevel("/levels/spawn.png");

    public Level(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Level dimensions must be positive");
        this.width = width;
        this.height = height;
        tiles = new int[width * height];
        Arrays.fill(tiles, Tile.col_spawn_grass);
    }

    public Level(String path) {
        BufferedImage image = ImageResources.load(path);
        width = image.getWidth();
        height = image.getHeight();
        tiles = image.getRGB(0, 0, width, height, null, 0, width);
    }

    protected void generateLevel() {}
    public void update() {}
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void render(int xScroll, int yScroll, Screen screen) {
        screen.setOffset(xScroll, yScroll);
        int x0 = xScroll >> 4;
        int x1 = (xScroll + screen.width + 16) >> 4;
        int y0 = yScroll >> 4;
        int y1 = (yScroll + screen.height + 16) >> 4;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) getTile(x, y).render(x, y, screen);
        }
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) return Tile.VOID;
        if (tiles[x + y * width] == Tile.col_spawn_grass) return Tile.SPAWN_GRASS;
        if (tiles[x + y * width] == Tile.col_spawn_bush) return Tile.SPAWN_BUSH;
        if (tiles[x + y * width] == Tile.col_spawn_water) return Tile.SPAWN_WATER;
        if (tiles[x + y * width] == Tile.col_spawn_wall) return Tile.SPAWN_WALL;
        if (tiles[x + y * width] == Tile.col_spawn_floor) return Tile.SPAWN_FLOOR;
        return Tile.VOID;
    }
}
