package game.rain.graphics;

public class Sprite {

	public final int SIZE;
	private int x, y;
	public int[] pixels;
	private SpriteSheet sheet;

	public static Sprite GRASS  = new Sprite(16, 0, 0, SpriteSheet.tiles);
	public static Sprite FLOWER = new Sprite(16, 1, 0, SpriteSheet.tiles);
	public static Sprite ROCK   = new Sprite(16, 2, 0, SpriteSheet.tiles);
	public static Sprite VOID   = new Sprite(16, 0x1B87E0);

	//Spawn level Sprites:
	public static Sprite SPAWN_GRASS = new Sprite(16, 0, 0, SpriteSheet.spawn_level);
	public static Sprite SPAWN_BUSH  = new Sprite(16, 1, 0, SpriteSheet.spawn_level);
	public static Sprite SPAWN_WATER = new Sprite(16, 2, 0, SpriteSheet.spawn_level);
	public static Sprite SPAWN_WALL  = new Sprite(16, 0, 1, SpriteSheet.spawn_level);
	public static Sprite SPAWN_FLOOR = new Sprite(16, 1, 1, SpriteSheet.spawn_level);
	public static Sprite SPAWN_SAND  = new Sprite(16, 2, 1, SpriteSheet.spawn_level);

	//Player Sprites here:
	public static Sprite player_up = new Sprite(32, 0, 5, SpriteSheet.tiles);
	public static Sprite player_up_1 = new Sprite(32, 0, 6, SpriteSheet.tiles);
	public static Sprite player_up_2 = new Sprite(32, 0, 7, SpriteSheet.tiles);

	public static Sprite player_down = new Sprite(32, 2, 5, SpriteSheet.tiles);
	public static Sprite player_down_1 = new Sprite(32, 2, 6, SpriteSheet.tiles);
	public static Sprite player_down_2 = new Sprite(32, 2, 7, SpriteSheet.tiles);

	public static Sprite player_side = new Sprite(32, 1, 5, SpriteSheet.tiles);
	public static Sprite player_side_1 = new Sprite(32, 1, 6, SpriteSheet.tiles);
	public static Sprite player_side_2 = new Sprite(32, 1, 7, SpriteSheet.tiles);

	public Sprite(int size, int x, int y, SpriteSheet sheet) {
		SIZE = size;
		pixels = new int[SIZE * SIZE];
		this.x = x * size;
		this.y = y * size;
		this.sheet = sheet;
		load();
	}

	public Sprite(int size, int colour) {
		SIZE = size;
		pixels = new int[SIZE * SIZE];
		setColour(colour);
	}

	private void setColour(int colour) {
		for (int i = 0; i < SIZE * SIZE; i++) {
			pixels[i] = colour;
		}
	}

	private void load() {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				pixels[x + y * SIZE] = sheet.pixels[(x + this.x) + (y + this.y) * sheet.SIZE];
			}
		}
	}
}
