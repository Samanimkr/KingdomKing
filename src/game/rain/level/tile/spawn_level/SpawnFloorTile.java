package game.rain.level.tile.spawn_level;

import game.rain.graphics.Screen;
import game.rain.graphics.Sprite;
import game.rain.level.tile.Tile;

public class SpawnFloorTile extends Tile{

	public SpawnFloorTile(Sprite sprite) {
		super(sprite);
	}
	
	public void render(int x, int y, Screen screen){
		screen.renderTile(x << 4, y << 4, this);
	}
	

}
