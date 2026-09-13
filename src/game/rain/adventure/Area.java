package game.rain.adventure;

public enum Area {
    CASTLE("The Quiet Crown", "A kingdom waiting to wake", 0x163e42, 0x51a45a),
    MEADOW("Briarfield", "The goblins have stolen the bells", 0x174c3d, 0x65b63e),
    MARSH("Mosswater Mill", "Turn the flood into a pathway", 0x143d60, 0x348e83),
    TEMPLE("Temple of Three Suns", "Light remembers the way", 0x534269, 0xc6a16d),
    KEEP("The Pretender's Keep", "A crown must sometimes be relinquished", 0x242650, 0x6666a8),
    THRONE("The Hollow Throne", "Only one king leaves crowned", 0x392047, 0x94578c),
    CRYPT("The Forgotten Treasury", "Listen for the hollow stone", 0x142f49, 0x397f95);

    public final String title, subtitle;
    public final int dark, ground;
    Area(String title, String subtitle, int dark, int ground) {
        this.title = title; this.subtitle = subtitle; this.dark = dark; this.ground = ground;
    }
}
