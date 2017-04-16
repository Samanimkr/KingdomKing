package game.rain.level;

import game.rain.graphics.Screen;
import game.rain.level.tile.Tile;

public class Level {

	protected int width, height;
	protected int[] tilesInt;
	protected int[] tiles;
	public static Level spawn = new SpawnLevel("/levels/spawn.png");

	public Level(int width, int height) {
		this.width = width;
		this.height = height;
		tilesInt = new int[width * height];
		generateLevel();
	}

	public Level(String path) {
		loadLevel(path);
		generateLevel();
	}

	protected void generateLevel() {
	}

	protected void loadLevel(String path) {
	}

	//60 Updates Per Second - if we want bots 
	public void update() {
	}

	// day-night cycles
	@SuppressWarnings("unused")
	private void time() {
	}

	public void render(int xScroll, int yScroll, Screen screen) {
		screen.setOffset(xScroll, yScroll);
		int x0 = xScroll >> 4;
		int x1 = (xScroll + screen.width + 16) >> 4;
		int y0 = yScroll >> 4;
		int y1 = (yScroll + screen.height + 16) >> 4;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				getTile(x, y).render(x, y, screen);
			}
		}
	}

	//grass = 0xFF00FF00
	//flower = 0xFFFFFF00
	//rock = 0xFFF7F700
	public Tile getTile(int x, int y) {
		if (x < 0 || y < 0 || x >= width || y >= width) return Tile.VOID;
		if (tiles[x + y * width] == Tile.col_spawn_grass) return Tile.SPAWN_GRASS;
		if (tiles[x + y * width] == Tile.col_spawn_bush) return Tile.SPAWN_BUSH;
		if (tiles[x + y * width] == Tile.col_spawn_water) return Tile.SPAWN_WATER;
		if (tiles[x + y * width] == Tile.col_spawn_wall) return Tile.SPAWN_WALL;
		if (tiles[x + y * width] == Tile.col_spawn_floor) return Tile.SPAWN_FLOOR;
		return Tile.VOID;
	}
}
