package hollowshift.world;

import hollowshift.engine.Rng;
import hollowshift.engine.io.Resources;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads chunk templates from /chunks/rooms.txt. Format:
 * <pre>
 * ; comment
 * @ name weight=3 minExits=1 [start]
 * (13 rows of 13 characters)
 * </pre>
 */
public final class ChunkLibrary {
    private final List<ChunkTemplate> rooms = new ArrayList<>();
    private final List<ChunkTemplate> starts = new ArrayList<>();

    public static ChunkLibrary load() {
        return parse(Resources.require("/chunks/rooms.txt"));
    }

    public static ChunkLibrary parse(String text) {
        ChunkLibrary lib = new ChunkLibrary();
        String name = null;
        int weight = 1, minExits = 1;
        boolean start = false;
        List<String> rows = new ArrayList<>();
        for (String raw : (text + "\n@").split("\r?\n")) {
            String line = raw.stripTrailing();
            if (line.startsWith(";")) continue;
            if (line.startsWith("@")) {
                if (name != null) lib.add(new ChunkTemplate(name, weight, minExits, start, List.copyOf(rows)));
                rows.clear();
                String[] parts = line.substring(1).trim().split("\\s+");
                name = parts[0].isEmpty() ? null : parts[0];
                weight = 1;
                minExits = 1;
                start = false;
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].startsWith("weight=")) weight = Integer.parseInt(parts[i].substring(7));
                    else if (parts[i].startsWith("minExits=")) minExits = Integer.parseInt(parts[i].substring(9));
                    else if (parts[i].equals("start")) start = true;
                }
            } else if (name != null && !line.isEmpty()) {
                rows.add(line);
            }
        }
        return lib;
    }

    private void add(ChunkTemplate t) {
        (t.start() ? starts : rooms).add(t);
    }

    public List<ChunkTemplate> rooms() {
        return rooms;
    }

    public List<ChunkTemplate> starts() {
        return starts;
    }

    public ChunkTemplate pickRoom(Rng rng, int exits) {
        int total = 0;
        for (ChunkTemplate t : rooms) if (t.minExits() <= exits) total += t.weight();
        int roll = rng.range(0, total);
        for (ChunkTemplate t : rooms) {
            if (t.minExits() > exits) continue;
            roll -= t.weight();
            if (roll < 0) return t;
        }
        return rooms.get(0);
    }

    public ChunkTemplate pickStart(Rng rng) {
        return rng.pick(starts);
    }
}
