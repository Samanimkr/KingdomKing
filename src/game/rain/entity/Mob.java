package game.rain.entity;

import game.rain.graphics.Sprite;

public abstract class Mob extends Entity {

	protected Sprite sprite;
	protected int dir = 0;
	protected boolean moving = false;
	protected boolean walking = false;

	public void move(int xa, int ya) {

		if (xa != 0 && ya != 0) {
			move(xa, 0);
			move(0, ya);
			return;
		}

		if (xa > 0) dir = 1;
		if (xa < 0) dir = 3;
		if (ya > 0) dir = 2;
		if (ya < 0) dir = 0;

		// Check every pixel so faster movement cannot skip a solid tile.
		int steps = Math.max(Math.abs(xa), Math.abs(ya));
		int dx = Integer.signum(xa), dy = Integer.signum(ya);
		for (int step = 0; step < steps; step++) {
			if (collision(dx, dy)) break;
			x += dx;
			y += dy;
		}
	}

	public void update() {
	}

	private boolean collision(int xa, int ya) {
		boolean solid = false;
		for(int c = 0; c < 4; c++){
			int xt = Math.floorDiv(x + xa + c % 2 * 10 - 5, 16); //collision box
			int yt = Math.floorDiv(y + ya + c / 2 * 12 + 3, 16);
			if (level.getTile(xt, yt).solid()) solid = true;
		}
		return solid;
	}

	public void render() {
	}

}
