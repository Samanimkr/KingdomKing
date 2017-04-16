package game.rain.level.tile;

import game.rain.graphics.Screen;
import game.rain.graphics.Sprite;
import game.rain.level.tile.spawn_level.SpawnBushTile;
import game.rain.level.tile.spawn_level.SpawnFloorTile;
import game.rain.level.tile.spawn_level.SpawnGrassTile;
import game.rain.level.tile.spawn_level.SpawnWallTile;
import game.rain.level.tile.spawn_level.SpawnWaterTile;

public class Tile {

	public int x, y;
	public Sprite sprite;

	public static Tile GRASS = new GrassTile(Sprite.GRASS);
	public static Tile FLOWER = new FlowerTile(Sprite.FLOWER);
	public static Tile ROCK = new RockTile(Sprite.ROCK);
	public static Tile VOID = new VoidTile(Sprite.VOID);

	//Spawn Tiles here:
	public static Tile SPAWN_GRASS = new SpawnGrassTile(Sprite.SPAWN_GRASS);
	public static Tile SPAWN_BUSH = new SpawnBushTile(Sprite.SPAWN_BUSH);
	public static Tile SPAWN_WATER = new SpawnWaterTile(Sprite.SPAWN_WATER);
	public static Tile SPAWN_WALL = new SpawnWallTile(Sprite.SPAWN_WALL);
	public static Tile SPAWN_FLOOR = new SpawnFloorTile(Sprite.SPAWN_FLOOR);

	public final static int col_spawn_grass = 0xff00ff00;
	public final static int col_spawn_bush = 0xff00aa00; //unused
	public final static int col_spawn_water = 0; //unused
	public final static int col_spawn_wall = 0xff525252;
	public final static int col_spawn_floor = 0xffff7d00;

	public Tile(Sprite sprite) {
		this.sprite = sprite;
	}

	public void render(int x, int y, Screen screen) {

	}

	public boolean solid() {
		return false;
	}

}
