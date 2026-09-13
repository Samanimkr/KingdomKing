package game.rain.adventure;

import java.util.HashSet;
import java.util.Set;

public final class Progress {
    public int coins, weapon, hearts, jewels, decree, deaths, slotA, slotB, bestCombo;
    public final Set<String> flags = new HashSet<>();
    public boolean has(String flag) { return flags.contains(flag); }
    public boolean set(String flag) { return flags.add(flag); }
    public int maxHealth() { return 12 + hearts*2; }
    public int restored() { return (has("forge")?1:0)+(has("garden")?1:0)+(has("tower")?1:0)+(has("won")?1:0); }
    public boolean jewel(Actors.Jewel jewel) {
        return jewel != Actors.Jewel.NONE && (slotA==jewel.ordinal() || slotB==jewel.ordinal());
    }
}
