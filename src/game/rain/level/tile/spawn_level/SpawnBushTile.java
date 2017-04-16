package game.rain.level.tile.spawn_level;

import game.rain.graphics.Screen;
import game.rain.graphics.Sprite;
import game.rain.level.tile.Tile;

public class SpawnBushTile extends Tile{

	public SpawnBushTile(Sprite sprite) {
		super(sprite);
	}
	
	public void render(int x, int y, Screen screen){
		screen.renderTile(x << 4, y << 4, this);
	}

	public boolean breakable(){
		return true;
	}
	
	public boolean solid(){
		return true;
	}
}
