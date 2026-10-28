package hollowshift.world;

import hollowshift.engine.Rng;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds a level from room templates.
 * <ol>
 *   <li>Grow a random spanning tree of rooms from the start cell (top centre)
 *       until the requested number of rooms is reached ("growing tree").</li>
 *   <li>Add a few extra doors between neighbouring rooms so there are loops
 *       to escape through.</li>
 *   <li>Stamp a template into every room, wall it off and open the doorways.</li>
 * </ol>
 * The same seed always produces the same level.
 */
public final class WorldGenerator {
    public static final int N = 1, E = 2, S = 4, W = 8;
    private static final int[] DIRS = {N, E, S, W};
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};
    public static final int CHUNK = ChunkTemplate.SIZE + 2;

    private final ChunkLibrary library;

    public WorldGenerator(ChunkLibrary library) {
        this.library = library;
    }

    /** Result of generation. */
    public record Level(TileMap map, List<Marker> markers, int cellsW, int cellsH, int[] doors, int[] depth, int startCell) {
        public int maxDepth() {
            return Arrays.stream(depth).max().orElse(0);
        }
    }

    public Level generate(long seed, int cellsW, int cellsH, int rooms, double loopChance) {
        Rng rng = new Rng(seed);
        int cells = cellsW * cellsH;
        rooms = Math.max(2, Math.min(rooms, cells));
        int[] doors = new int[cells];
        boolean[] active = new boolean[cells];
        int start = cellsW / 2;
        active[start] = true;
        List<Integer> open = new ArrayList<>(List.of(start));
        int count = 1;

        // 1. growing tree: mostly extend the newest room (long corridors), sometimes a random one (branches)
        while (count < rooms && !open.isEmpty()) {
            int idx = rng.chance(0.6) ? open.size() - 1 : rng.range(0, open.size());
            int c = open.get(idx);
            List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
            rng.shuffle(dirs);
            boolean grew = false;
            for (int d : dirs) {
                int nx = c % cellsW + DX[d], ny = c / cellsW + DY[d];
                if (nx < 0 || ny < 0 || nx >= cellsW || ny >= cellsH) continue;
                int n = ny * cellsW + nx;
                if (active[n]) continue;
                active[n] = true;
                doors[c] |= DIRS[d];
                doors[n] |= DIRS[(d + 2) % 4];
                open.add(n);
                count++;
                grew = true;
                break;
            }
            if (!grew) open.remove(idx);
        }

        // 2. extra loops (never into the start room so the elevator stays a single choke point)
        for (int c = 0; c < cells; c++) {
            if (!active[c] || c == start) continue;
            for (int d = 1; d <= 2; d++) { // E and S only, each pair checked once
                int nx = c % cellsW + DX[d], ny = c / cellsW + DY[d];
                if (nx >= cellsW || ny >= cellsH) continue;
                int n = ny * cellsW + nx;
                if (!active[n] || n == start || (doors[c] & DIRS[d]) != 0) continue;
                if (rng.chance(loopChance)) {
                    doors[c] |= DIRS[d];
                    doors[n] |= DIRS[(d + 2) % 4];
                }
            }
        }

        int[] depth = bfsDepth(doors, active, cellsW, cellsH, start);

        // 3. stamp rooms
        TileMap map = new TileMap(cellsW * CHUNK, cellsH * CHUNK);
        List<Marker> markers = new ArrayList<>();
        for (int c = 0; c < cells; c++) {
            if (!active[c]) continue;
            ChunkTemplate t = c == start ? library.pickStart(rng) : library.pickRoom(rng, Integer.bitCount(doors[c]));
            boolean flipX = rng.chance(0.5);
            boolean flipY = c != start && rng.chance(0.5);
            stamp(map, markers, t, c % cellsW * CHUNK, c / cellsW * CHUNK, doors[c], flipX, flipY, depth[c], c);
        }
        return new Level(map, markers, cellsW, cellsH, doors, depth, start);
    }

    private static int[] bfsDepth(int[] doors, boolean[] active, int w, int h, int start) {
        int[] depth = new int[doors.length];
        Arrays.fill(depth, -1);
        depth[start] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>(List.of(start));
        while (!q.isEmpty()) {
            int c = q.poll();
            for (int d = 0; d < 4; d++) {
                if ((doors[c] & DIRS[d]) == 0) continue;
                int n = (c / w + DY[d]) * w + c % w + DX[d];
                if (depth[n] < 0) {
                    depth[n] = depth[c] + 1;
                    q.add(n);
                }
            }
        }
        for (int i = 0; i < depth.length; i++) if (!active[i]) depth[i] = -1;
        return depth;
    }

    public static void stamp(TileMap map, List<Marker> markers, ChunkTemplate t, int ox, int oy, int doors,
                      boolean flipX, boolean flipY, int depth, int cell) {
        // wall ring with doorways
        for (int i = 0; i < CHUNK; i++) {
            map.set(ox + i, oy, Tile.WALL);
            map.set(ox + i, oy + CHUNK - 1, Tile.WALL);
            map.set(ox, oy + i, Tile.WALL);
            map.set(ox + CHUNK - 1, oy + i, Tile.WALL);
        }
        for (int i = ChunkTemplate.DOOR_LO + 1; i <= ChunkTemplate.DOOR_HI + 1; i++) {
            if ((doors & N) != 0) map.set(ox + i, oy, Tile.FLOOR);
            if ((doors & S) != 0) map.set(ox + i, oy + CHUNK - 1, Tile.FLOOR);
            if ((doors & W) != 0) map.set(ox, oy + i, Tile.FLOOR);
            if ((doors & E) != 0) map.set(ox + CHUNK - 1, oy + i, Tile.FLOOR);
        }
        for (int y = 0; y < ChunkTemplate.SIZE; y++) {
            for (int x = 0; x < ChunkTemplate.SIZE; x++) {
                char ch = t.at(x, y, flipX, flipY);
                int tx = ox + 1 + x, ty = oy + 1 + y;
                map.set(tx, ty, Tile.fromSymbol(ch));
                if (Character.isLetter(ch) && ch != 'x') markers.add(new Marker(ch, tx, ty, depth, cell));
            }
        }
    }
}
