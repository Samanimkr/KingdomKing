package game.rain.adventure;

public enum Area {
    CASTLE("The Quiet Crown", "A kingdom waiting to wake", 0x263e39, 0x617851),
    MEADOW("Briarfield", "The goblins have stolen the bells", 0x203d35, 0x607e44),
    MARSH("Mosswater Mill", "Turn the flood into a pathway", 0x243d49, 0x48685e),
    TEMPLE("Temple of Three Suns", "Light remembers the way", 0x413d39, 0x94866a),
    KEEP("The Pretender's Keep", "A crown must sometimes be relinquished", 0x292c42, 0x53566c),
    THRONE("The Hollow Throne", "Only one king leaves crowned", 0x322a3b, 0x695464),
    CRYPT("The Forgotten Treasury", "Listen for the hollow stone", 0x23343b, 0x496068);

    public final String title, subtitle;
    public final int dark, ground;
    Area(String title, String subtitle, int dark, int ground) {
        this.title = title; this.subtitle = subtitle; this.dark = dark; this.ground = ground;
    }
}
