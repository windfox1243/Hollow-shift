package hollowshift.entity;

import java.util.EnumMap;
import java.util.Map;

/** The miner's pockets: one pocket per kind of supply, each holding a few. */
public final class Pack {
    private final Map<Supply, Integer> counts = new EnumMap<>(Supply.class);
    private Supply selected = Supply.STONE;

    public int count(Supply s) {
        return counts.getOrDefault(s, 0);
    }

    public boolean hasRoom(Supply s) {
        return count(s) < s.max;
    }

    public boolean add(Supply s) {
        if (!hasRoom(s)) return false;
        counts.put(s, count(s) + 1);
        // an empty hand reaches for what was just picked up
        if (count(selected) == 0) selected = s;
        return true;
    }

    public boolean take(Supply s) {
        if (count(s) == 0) return false;
        counts.put(s, count(s) - 1);
        return true;
    }

    public Supply selected() {
        return selected;
    }

    public void select(Supply s) {
        selected = s;
    }

    /** Next pocket that has something in it (stays put if all are empty). */
    public void cycle() {
        Supply[] all = Supply.values();
        for (int i = 1; i <= all.length; i++) {
            Supply s = all[(selected.ordinal() + i) % all.length];
            if (count(s) > 0) {
                selected = s;
                return;
            }
        }
    }
}
