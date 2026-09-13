package game.rain.level;

import game.rain.level.tile.Tile;
import java.util.Random;

public final class RandomLevel extends Level {
    private static final Random random = new Random();
    private static final int[] PALETTE = {
        Tile.col_spawn_grass, Tile.col_spawn_floor, Tile.col_spawn_bush
    };

    public RandomLevel(int width, int height) {
        super(width, height);
        generateLevel();
    }

    @Override
    protected void generateLevel() {
        for (int i = 0; i < tiles.length; i++) tiles[i] = PALETTE[random.nextInt(PALETTE.length)];
    }
}
