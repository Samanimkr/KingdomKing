package game.rain.entity;

import game.rain.graphics.Screen;
import game.rain.graphics.Sprite;
import game.rain.input.Keyboard;

public class Player extends Mob {

	private Keyboard input;
	private Sprite sprite;
	private int anim = 0;
	private boolean walking = false;

	public Player(Keyboard input) {
		this.input = input;
		sprite = Sprite.player_up;
	}

	public Player(int x, int y, Keyboard input) {
		this.input = input;
		sprite = Sprite.player_up;
		this.x = x;
		this.y = y;
	}

	public void update() {
		if (anim < 500) {
			anim++;
		} else {
			anim = 0;
		}

		int xa = 0, ya = 0;

		if (input.dash) {
			if (input.up) ya -= 2;
			if (input.down) ya += 2;
			if (input.left) xa -= 2;
			if (input.right) xa += 2;
		} else {
			if (input.up) ya--;
			if (input.down) ya++;
			if (input.left) xa--;
			if (input.right) xa++;
		}
		
		if (xa != 0 || ya != 0) {
			move(xa, ya);
			walking = true;
		} else {
			walking = false;
		}
	}

	public void render(Screen screen) {
		int flip = 0;
		if (dir == 0) {
			sprite = Sprite.player_up;
			if (walking) {
				if (anim % 20 > 10) {
					sprite = Sprite.player_up_1;
				} else {
					sprite = Sprite.player_up_2;
				}
			}
		}
		if (dir == 1) {
			sprite = Sprite.player_side;
			if (walking) {
				if (anim % 21 > 7) {
					sprite = Sprite.player_side_1;
				} else if (anim % 21 > 20) {
					sprite = Sprite.player_side_2;
				} else {
					sprite = Sprite.player_side;
				}
			}
		}
		if (dir == 2) {
			sprite = Sprite.player_down;
			if (walking) {
				if (anim % 20 > 10) {
					sprite = Sprite.player_down_1;
				} else {
					sprite = Sprite.player_down_2;
				}
			}
		}
		if (dir == 3) {
			sprite = Sprite.player_side;
			if (walking) {
				if (anim % 21 > 7) {
					sprite = Sprite.player_side_1;
				} else if (anim % 21 > 20) {
					sprite = Sprite.player_side_2;
				} else {
					sprite = Sprite.player_side;
				}
			}
			flip = 1;
		}
		screen.renderPlayer(x - 16, y - 16, sprite, flip);
	}

}
