package game.rain;

import game.rain.entity.Player;
import game.rain.graphics.ImageResources;
import game.rain.graphics.Screen;
import game.rain.graphics.Sprite;
import game.rain.graphics.SpriteSheet;
import game.rain.input.Keyboard;
import game.rain.level.Level;
import game.rain.level.RandomLevel;
import game.rain.level.tile.Tile;
import java.awt.Canvas;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Behavioral regressions for the recovered game; no external test framework needed. */
public final class RegressionTests {
    private static int passed;
    private static final Canvas KEY_SOURCE = new Canvas();

    public static void main(String[] args) throws Exception {
        test("sharp presentation at normal, Retina and fractional display scales", RegressionTests::pixelViewport);
        test("bundled artwork and map palette", RegressionTests::resources);
        test("rectangular map boundaries", RegressionTests::rectangularMaps);
        test("generated levels contain renderable tiles", RegressionTests::generatedLevels);
        test("extended keys and focus reset", RegressionTests::keyboard);
        test("pause actions ignore key repeat", RegressionTests::keyRepeat);
        test("movement, sprint and key release", RegressionTests::movement);
        test("wall collision and sprint tunneling", RegressionTests::collision);
        test("all four world boundaries", RegressionTests::worldEdges);
        test("sprite clipping, transparency and flipping", RegressionTests::rendering);
        test("all three side-walking frames", RegressionTests::animation);
        test("headless game frame and controls", RegressionTests::gameControls);
        test("game loop starts once and stops without deadlock", RegressionTests::lifecycle);
        System.out.println("PASS: " + passed + " regression groups");
    }

    private static void test(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void pixelViewport() {
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int[] colors = {0xff0000, 0x00ff00, 0x0000ff, 0xffffff};
        source.setRGB(0, 0, 2, 2, colors, 0, 2);
        for (double dpi : new double[]{1, 1.25, 1.5, 2}) {
            int width = (int) Math.round(13 * dpi), height = (int) Math.round(9 * dpi);
            BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = output.createGraphics();
            graphics.scale(dpi, dpi);
            java.awt.geom.AffineTransform before = graphics.getTransform();
            PixelViewport.draw(graphics, source, 13, 9);
            check(graphics.getTransform().equals(before), "presentation changed caller transform");
            graphics.dispose();
            int scale = height / 2, left = (width - scale * 2) / 2, top = (height - scale * 2) / 2;
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int expected = x < left || x >= left + scale * 2 || y < top || y >= top + scale * 2
                        ? 0x091322 : colors[(y - top) / scale * 2 + (x - left) / scale];
                check((output.getRGB(x, y) & 0xffffff) == expected,
                        "blur, uneven pixels or stale border at display scale " + dpi + " pixel " + x + "," + y);
            }
        }
    }

    private static void resources() {
        check(SpriteSheet.tiles.pixels.length == 256 * 256, "player sprites missing");
        check(SpriteSheet.spawn_level.pixels.length == 48 * 48, "map sprites missing");
        Level level = Level.spawn;
        check(level.getWidth() == 64 && level.getHeight() == 48, "original map dimensions changed");
        int water = 0;
        for (int y = 0; y < level.getHeight(); y++) {
            for (int x = 0; x < level.getWidth(); x++) {
                Tile tile = level.getTile(x, y);
                check(tile != Tile.VOID, "unrecognized original map color at " + x + "," + y);
                if (tile == Tile.SPAWN_WATER) water++;
            }
        }
        check(water == 83 && Tile.SPAWN_WATER.solid(), "original water must render and block movement");
        try {
            ImageResources.load("/missing.png");
            throw new AssertionError("missing image was accepted");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("/missing.png"), "resource error must name the missing file");
        }
        try {
            new SpriteSheet("/textures/spawn_level.png", 16);
            throw new AssertionError("invalid sprite sheet dimensions accepted");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("16x16"), "sheet size error should explain the expected size");
        }
    }

    private static void rectangularMaps() {
        check(Level.spawn.getTile(0, 48) == Tile.VOID, "bottom edge should not index past the map array");
        Level tall = new Level(2, 5);
        check(tall.getTile(1, 4) == Tile.SPAWN_GRASS, "valid tall map rows must be reachable");
        for (Level level : new Level[]{Level.spawn, tall, new Level(5, 2)}) {
            check(level.getTile(-1, 0).solid(), "left boundary");
            check(level.getTile(0, -1).solid(), "top boundary");
            check(level.getTile(level.getWidth(), 0).solid(), "right boundary");
            check(level.getTile(0, level.getHeight()).solid(), "bottom boundary");
            level.render(-160, -160, new Screen(300, 168));
            level.render(level.getWidth() * 16, level.getHeight() * 16, new Screen(300, 168));
        }
    }

    private static void generatedLevels() {
        Level level = new RandomLevel(7, 12);
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 7; x++) check(level.getTile(x, y) != Tile.VOID, "generated map tile is missing");
        }
        level.render(0, 0, new Screen(128, 200));
    }

    private static KeyEvent event(int id, int code) {
        return new KeyEvent(KEY_SOURCE, id, System.currentTimeMillis(), 0, code, KeyEvent.CHAR_UNDEFINED);
    }

    private static void press(Keyboard key, int code) { key.keyPressed(event(KeyEvent.KEY_PRESSED, code)); }
    private static void release(Keyboard key, int code) { key.keyReleased(event(KeyEvent.KEY_RELEASED, code)); }

    private static void keyboard() {
        Keyboard key = new Keyboard();
        for (int code : new int[]{KeyEvent.VK_F12, KeyEvent.VK_META, KeyEvent.VK_WINDOWS, KeyEvent.VK_CONTEXT_MENU, 65535}) {
            press(key, code);
            release(key, code);
        }
        press(key, KeyEvent.VK_W);
        press(key, KeyEvent.VK_SHIFT);
        key.update();
        check(key.up && key.dash, "W + Shift should move and sprint");
        key.focusLost(new FocusEvent(KEY_SOURCE, FocusEvent.FOCUS_LOST));
        key.update();
        check(!key.up && !key.dash, "switching apps must clear held movement");
    }

    private static void keyRepeat() {
        Keyboard key = new Keyboard();
        press(key, KeyEvent.VK_ESCAPE);
        check(key.consumePress(KeyEvent.VK_ESCAPE), "first press should pause");
        press(key, KeyEvent.VK_ESCAPE);
        check(!key.consumePress(KeyEvent.VK_ESCAPE), "key repeat should not toggle pause again");
        release(key, KeyEvent.VK_ESCAPE);
        press(key, KeyEvent.VK_ESCAPE);
        check(key.consumePress(KeyEvent.VK_ESCAPE), "next physical press should toggle again");
    }

    private static Player player(Level level, Keyboard key, int x, int y) {
        Player player = new Player(x, y, key);
        player.init(level);
        return player;
    }

    private static void movement() {
        Keyboard key = new Keyboard();
        Player player = player(new Level(20, 20), key, 64, 64);
        press(key, KeyEvent.VK_D);
        key.update();
        player.update();
        check(player.x == 65, "walk one pixel per update");
        press(key, KeyEvent.VK_CONTROL);
        key.update();
        player.update();
        check(player.x == 67, "sprint two pixels per update");
        release(key, KeyEvent.VK_D);
        key.update();
        player.update();
        check(player.x == 67, "released key should stop movement");
        press(key, KeyEvent.VK_LEFT);
        press(key, KeyEvent.VK_RIGHT);
        key.update();
        player.update();
        check(player.x == 67, "opposite keys should cancel");
    }

    private static final class TestLevel extends Level {
        TestLevel(int width, int height) { super(width, height); }
        void wall(int x, int y) { tiles[x + y * width] = Tile.col_spawn_wall; }
    }

    private static void collision() {
        TestLevel level = new TestLevel(12, 8);
        for (int y = 0; y < 8; y++) level.wall(5, y);
        Player player = player(level, new Keyboard(), 64, 48);
        player.move(100, 0);
        check(player.x == 74, "movement must stop before a wall, even with a large step");
        player.move(2, 2);
        check(player.x == 74 && player.y == 50, "diagonal input should slide along walls");
    }

    private static void worldEdges() {
        Player player = player(new Level(10, 6), new Keyboard(), 64, 32);
        player.move(-1000, 0);
        check(player.x == 5, "left edge must not truncate negative coordinates to zero");
        player.move(0, -1000);
        check(player.y == -3, "top of the foot collision box must remain in the map");
        player.move(1000, 0);
        check(player.x == 154, "right edge");
        player.move(0, 1000);
        check(player.y == 80, "bottom edge");
    }

    private static void rendering() {
        Sprite sprite = new Sprite(4, 0xff000000);
        for (int i = 0; i < 16; i++) sprite.pixels[i] = 0xff000000 | (i + 1);
        Screen screen = new Screen(2, 2);
        screen.renderPlayer(-2, -2, sprite, 0);
        check(screen.pixels[0] == sprite.pixels[10] && screen.pixels[3] == sprite.pixels[15], "clip from correct source pixels");
        screen.clear();
        screen.renderPlayer(0, 0, sprite, 3);
        check(screen.pixels[0] == sprite.pixels[15] && screen.pixels[3] == sprite.pixels[10], "both flip axes");
        sprite.pixels[0] = 0xffff00ff;
        sprite.pixels[1] = 0x00000000;
        Arrays.fill(screen.pixels, 123);
        screen.renderPlayer(0, 0, sprite, 0);
        check(screen.pixels[0] == 123 && screen.pixels[1] == 123, "transparent pixels must preserve the background");
        screen.renderPlayer(-100, 0, sprite, 0);
        screen.renderPlayer(100, 100, sprite, 0);
        screen.clear();
        screen.renderTile(-2, -2, new Tile(sprite));
        check(screen.pixels[0] == sprite.pixels[10], "tile clipping");
        screen.setOffset(10, 10);
        screen.renderTile(8, 8, new Tile(sprite));
        check(screen.pixels[3] == sprite.pixels[15], "camera offset");
    }

    private static void animation() {
        Keyboard key = new Keyboard();
        Player player = player(new Level(100, 10), key, 64, 64);
        Screen screen = new Screen(32, 32);
        Set<Integer> frames = new HashSet<>();
        press(key, KeyEvent.VK_D);
        for (int i = 0; i < 21; i++) {
            key.update();
            player.update();
            screen.clear();
            screen.setOffset(player.x - 16, player.y - 16);
            player.render(screen);
            frames.add(Arrays.hashCode(screen.pixels));
        }
        check(frames.size() == 3, "all three right-facing frames should be reachable, saw " + frames.size());
    }

    private static int frameHash(ClassicGame game) {
        BufferedImage image = game.renderFrame();
        return Arrays.hashCode(image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
    }

    private static void gameControls() {
        ClassicGame game = new ClassicGame();
        Keyboard key = (Keyboard) game.getKeyListeners()[0];
        int initial = frameHash(game);
        press(key, KeyEvent.VK_DOWN);
        for (int i = 0; i < 6; i++) game.update();
        check(frameHash(game) != initial, "movement should change the rendered world");
        press(key, KeyEvent.VK_ESCAPE);
        game.update();
        int paused = frameHash(game);
        for (int i = 0; i < 10; i++) game.update();
        check(frameHash(game) == paused, "paused scene must not move");
        press(key, KeyEvent.VK_R);
        game.update();
        check(frameHash(game) == initial, "respawn should restore the starting scene");
        key.clear();
    }

    private static void lifecycle() {
        ClassicGame game = new ClassicGame();
        game.stop(); // Stopping before startup must be safe.
        game.start();
        game.start();
        long loops = Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().equals("KingdomKing game loop") && t.isAlive()).count();
        check(loops == 1, "start must not create duplicate game threads");
        game.stop();
        check(Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.getName().equals("KingdomKing game loop") && t.isAlive()),
                "stop must finish the game thread without joining itself");
        game.start();
        game.stop();
    }
}
