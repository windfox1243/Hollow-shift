package hollowshift.world;

import java.util.List;

/**
 * A hand-authored room interior of {@link #SIZE}x{@link #SIZE} characters.
 * The generator surrounds it with a wall ring and opens doorways for the
 * connections the cell needs, so any template fits any combination of exits.
 */
public record ChunkTemplate(String name, int weight, int minExits, boolean start, List<String> rows) {
    public static final int SIZE = 13;
    /** Doorway occupies interior columns/rows DOOR_LO..DOOR_HI on each edge. */
    public static final int DOOR_LO = 5;
    public static final int DOOR_HI = 7;

    public ChunkTemplate {
        if (rows.size() != SIZE) throw new IllegalArgumentException("chunk " + name + " needs " + SIZE + " rows, has " + rows.size());
        for (String r : rows)
            if (r.length() != SIZE) throw new IllegalArgumentException("chunk " + name + " row '" + r + "' must be " + SIZE + " wide");
    }

    /** Character at interior (x, y) with optional mirroring. */
    public char at(int x, int y, boolean flipX, boolean flipY) {
        int fx = flipX ? SIZE - 1 - x : x;
        int fy = flipY ? SIZE - 1 - y : y;
        return rows.get(fy).charAt(fx);
    }
}
