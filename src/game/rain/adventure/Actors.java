package game.rain.adventure;

import java.util.HashSet;
import java.util.Set;

public final class Actors {
    private Actors() {}
    public enum Kind { SLIME, LARGE_SLIME, STICKY_SLIME, BOMB_SLIME, GOBLIN, KNIGHT, ARCHER, EMBER, BOSS, CHICKEN }
    public enum Jewel { NONE, RUBY, EMERALD, SAPPHIRE }
    public enum PropKind { GRASS, POT, FENCE, CRACKED_WALL, WOOD, SIGN, ROPE }

    public static final class Enemy {
        public final int id;
        public final Kind kind;
        public double x, y, vx, vy, aimX, aimY;
        public int hp, maxHp, age, cooldown, windup, charging, stun, flash, burn, stolen, phase = 1;
        public boolean dead;
        public Enemy(int id, Kind kind, double x, double y, boolean hunt) {
            this.id = id; this.kind = kind; this.x = x; this.y = y;
            maxHp = switch (kind) {
                case BOSS -> 160; case KNIGHT -> 16; case LARGE_SLIME -> 12;
                case ARCHER, EMBER -> 8; case GOBLIN -> 7; case CHICKEN -> 4; default -> 5;
            };
            if (hunt && kind != Kind.BOSS && kind != Kind.CHICKEN) maxHp += 3;
            hp = maxHp; cooldown = 40 + id % 80;
        }
        public double radius() { return kind == Kind.BOSS ? 19 : kind == Kind.LARGE_SLIME ? 13 : 9; }
    }
    public static final class Shot {
        public double x, y, vx, vy;
        public int life = 180;
        public final int owner;
        public final boolean fire, royal;
        public Shot(double x, double y, double vx, double vy, int owner, boolean fire, boolean royal) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.owner = owner; this.fire = fire; this.royal = royal;
        }
    }
    public static final class Crown {
        public double x, y, vx, vy;
        public int age, bounces, hits;
        public boolean returning;
        public final Set<Integer> struck = new HashSet<>();
    }
    public static final class Particle {
        public double x, y, vx, vy;
        public int life, total, color, size;
        public Particle(double x, double y, double vx, double vy, int life, int color, int size) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.life=life; total=life; this.color=color; this.size=size;
        }
    }
    public static final class Loot {
        public double x, y;
        public final boolean heart;
        public int value, age;
        public Loot(double x, double y, int value, boolean heart) { this.x=x; this.y=y; this.value=value; this.heart=heart; }
    }
    public static final class Prop {
        public final PropKind kind;
        public final double x, y;
        public boolean broken;
        public int burning, bend;
        public Prop(PropKind kind, double x, double y) { this.kind=kind; this.x=x; this.y=y; }
        public boolean solid() { return !broken && kind != PropKind.GRASS && kind != PropKind.SIGN && kind != PropKind.ROPE; }
    }
    public static final class Patch {
        public final double x,y;
        public int life;
        public final boolean fire;
        public Patch(double x, double y, int life, boolean fire) { this.x=x; this.y=y; this.life=life; this.fire=fire; }
    }
    public record Portal(double x, double y, Area target, String label) {}
    public record Landmark(double x, double y, String id, String label) {}
}
