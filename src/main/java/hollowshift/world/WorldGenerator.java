package hollowshift.world;

import hollowshift.engine.Rng;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds a level from room templates. Every room lies on a loop, so there is
 * always a second way out: a dead-end fuse room with the Tenant in the doorway
 * would be a death sentence.
 * <ol>
 *   <li>Lay a loop skeleton, picked at random: one ring ("O"), two rings sharing
 *       a side ("8"), a ring of corridors with spokes into a central core, or a
 *       small seed ring that grows organically.</li>
 *   <li>Grow it to the requested size with "ears": runs of 1-4 new rooms that
 *       join the level at both ends, so no room is ever a dead end.</li>
 *   <li>Add a few chords between neighbouring rooms for extra ways round.</li>
 *   <li>Stamp a template into every room (mirrored and/or turned), wall it off
 *       and open the doorways.</li>
 * </ol>
 * The same seed always produces the same level.
 */
public final class WorldGenerator {
    public static final int N = 1, E = 2, S = 4, W = 8;
    private static final int[] DIRS = {N, E, S, W};
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};
    public static final int CHUNK = ChunkTemplate.SIZE + 2;
    /** Interior offset of the middle doorway tile on each edge of a cell. */
    private static final int DOOR_MID = (ChunkTemplate.DOOR_LO + ChunkTemplate.DOOR_HI) / 2 + 1;

    /** Loop skeletons the generator chooses from. */
    public enum Layout { RING, EIGHT, CORE, ORGANIC }

    private final ChunkLibrary library;

    public WorldGenerator(ChunkLibrary library) {
        this.library = library;
    }

    /** Result of generation. roomNames holds the template stamped into each cell (null if unused). */
    public record Level(TileMap map, List<Marker> markers, int cellsW, int cellsH, int[] doors, int[] depth, int startCell,
                        Layout layout, String[] roomNames) {
        public int maxDepth() {
            return Arrays.stream(depth).max().orElse(0);
        }
    }

    /** The room graph while it is laid out. */
    private static final class Graph {
        final int w, h;
        final int[] doors;
        final boolean[] active;
        int count;

        Graph(int w, int h) {
            this.w = w;
            this.h = h;
            doors = new int[w * h];
            active = new boolean[w * h];
        }

        int neighbour(int c, int d) {
            int nx = c % w + DX[d], ny = c / w + DY[d];
            return nx < 0 || ny < 0 || nx >= w || ny >= h ? -1 : ny * w + nx;
        }

        void activate(int c) {
            if (!active[c]) {
                active[c] = true;
                count++;
            }
        }

        void connect(int a, int b) {
            for (int d = 0; d < 4; d++) {
                if (neighbour(a, d) != b) continue;
                activate(a);
                activate(b);
                doors[a] |= DIRS[d];
                doors[b] |= DIRS[(d + 2) % 4];
                return;
            }
            throw new IllegalArgumentException("cells " + a + " and " + b + " are not neighbours");
        }

        boolean connected(int a, int d) {
            return (doors[a] & DIRS[d]) != 0;
        }

        /** Activates the border of a rectangle of cells and links it into a loop. */
        void ring(int x0, int y0, int x1, int y1) {
            for (int x = x0; x < x1; x++) {
                connect(y0 * w + x, y0 * w + x + 1);
                connect(y1 * w + x, y1 * w + x + 1);
            }
            for (int y = y0; y < y1; y++) {
                connect(y * w + x0, (y + 1) * w + x0);
                connect(y * w + x1, (y + 1) * w + x1);
            }
        }

        /** A straight corridor of rooms from c in direction d until it meets an active room. */
        boolean spoke(int c, int d) {
            List<Integer> path = new ArrayList<>();
            int cur = neighbour(c, d);
            while (cur >= 0 && !active[cur]) {
                path.add(cur);
                cur = neighbour(cur, d);
            }
            if (cur < 0) return false;
            int from = c;
            for (int p : path) {
                connect(from, p);
                from = p;
            }
            connect(from, cur);
            return true;
        }
    }

    public Level generate(long seed, int cellsW, int cellsH, int rooms, double loopChance) {
        Rng rng = new Rng(seed);
        cellsW = Math.max(2, cellsW);
        cellsH = Math.max(2, cellsH);
        rooms = Math.max(4, Math.min(rooms, cellsW * cellsH));
        Graph g = new Graph(cellsW, cellsH);

        // 1. loop skeleton, 2. ears until big enough
        Layout layout = skeleton(g, rng, rooms);
        while (g.count < rooms && addEar(g, rng)) {
            // keep growing
        }

        // 3. a few chords for extra ways round
        for (int c = 0; c < g.active.length; c++) {
            if (!g.active[c]) continue;
            for (int d = 1; d <= 2; d++) { // E and S only, each pair checked once
                int n = g.neighbour(c, d);
                if (n < 0 || !g.active[n] || g.connected(c, d)) continue;
                if (rng.chance(loopChance)) g.connect(c, n);
            }
        }

        // the lift sits in the top-most room, the one nearest the middle
        int start = -1;
        for (int c = 0; c < g.active.length; c++) {
            if (!g.active[c]) continue;
            if (start < 0 || c / cellsW < start / cellsW
                    || c / cellsW == start / cellsW && Math.abs(c % cellsW - cellsW / 2) < Math.abs(start % cellsW - cellsW / 2)) {
                start = c;
            }
        }
        int[] doors = g.doors;
        boolean[] active = g.active;
        int[] depth = bfsDepth(doors, active, cellsW, cellsH, start);

        // 4. stamp rooms
        TileMap map = new TileMap(cellsW * CHUNK, cellsH * CHUNK);
        List<Marker> markers = new ArrayList<>();
        String[] names = new String[active.length];
        for (int c = 0; c < active.length; c++) {
            if (!active[c]) continue;
            ChunkTemplate t = c == start ? library.pickStart(rng) : library.pickRoom(rng, Integer.bitCount(doors[c]));
            names[c] = t.name();
            boolean flipX = rng.chance(0.5);
            boolean flipY = c != start && rng.chance(0.5);
            boolean turn = c != start && rng.chance(0.5);
            stamp(map, markers, t, c % cellsW * CHUNK, c / cellsW * CHUNK, doors[c], flipX, flipY, turn, depth[c], c);
        }
        return new Level(map, markers, cellsW, cellsH, doors, depth, start, layout, names);
    }

    /** Lays the first loop(s); a shape that does not fit the grid or the room budget is skipped. */
    private static Layout skeleton(Graph g, Rng rng, int rooms) {
        int w = g.w, h = g.h;
        List<Layout> options = new ArrayList<>(List.of(Layout.RING, Layout.RING, Layout.EIGHT, Layout.EIGHT, Layout.CORE, Layout.CORE, Layout.ORGANIC));
        rng.shuffle(options);
        for (Layout l : options) {
            switch (l) {
                case RING -> {
                    // a big ring: at least a third of the rooms, at most all of them
                    List<int[]> sizes = new ArrayList<>();
                    for (int rw = 2; rw <= w; rw++)
                        for (int rh = 2; rh <= h; rh++) {
                            int cells = 2 * (rw + rh) - 4;
                            if (cells <= rooms && cells >= Math.min(rooms, 4 + rooms / 3)) sizes.add(new int[]{rw, rh});
                        }
                    if (sizes.isEmpty()) continue;
                    int[] sz = rng.pick(sizes);
                    int x0 = rng.range(0, w - sz[0] + 1), y0 = rng.range(0, h - sz[1] + 1);
                    g.ring(x0, y0, x0 + sz[0] - 1, y0 + sz[1] - 1);
                    return l;
                }
                case EIGHT -> {
                    // two rings sharing a side, stacked or side by side
                    boolean vertical = h >= 3 && (w < 3 || rng.chance(0.5));
                    int along = vertical ? h : w, across = vertical ? w : h;
                    if (along < 3) continue;
                    int span = Math.min(across, rng.range(2, 4));
                    int len = Math.min(along, rng.range(3, 6));
                    int mid = rng.range(1, len - 1);
                    // border of each ring, minus the shared side counted twice
                    int cells = (2 * (span + mid + 1) - 4) + (2 * (span + len - mid) - 4) - span;
                    if (cells > rooms) continue;
                    int a0 = rng.range(0, along - len + 1), c0 = rng.range(0, across - span + 1);
                    if (vertical) {
                        g.ring(c0, a0, c0 + span - 1, a0 + mid);
                        g.ring(c0, a0 + mid, c0 + span - 1, a0 + len - 1);
                    } else {
                        g.ring(a0, c0, a0 + mid, c0 + span - 1);
                        g.ring(a0 + mid, c0, a0 + len - 1, c0 + span - 1);
                    }
                    return l;
                }
                case CORE -> {
                    // corridors round a central chamber, reached by two to four spokes
                    if (w < 3 || h < 3) continue;
                    int rw = Math.min(w, rng.range(3, 6)), rh = Math.min(h, rng.range(3, 6));
                    if (2 * (rw + rh) - 4 + 1 > rooms) continue;
                    int x0 = rng.range(0, w - rw + 1), y0 = rng.range(0, h - rh + 1);
                    g.ring(x0, y0, x0 + rw - 1, y0 + rh - 1);
                    int core = (y0 + rh / 2) * w + x0 + rw / 2;
                    g.activate(core);
                    List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
                    rng.shuffle(dirs);
                    int want = rng.chance(0.25) ? 4 : rng.chance(0.4) ? 3 : 2, spokes = 0;
                    for (int d : dirs) {
                        if (spokes >= 2 && g.count >= rooms) break;
                        if (spokes < want && g.spoke(core, d)) spokes++;
                    }
                    return l;
                }
                case ORGANIC -> {
                    int x0 = rng.range(0, w - 1), y0 = rng.range(0, h - 1);
                    g.ring(x0, y0, x0 + 1, y0 + 1);
                    return l;
                }
            }
        }
        g.ring(0, 0, 1, 1);
        return Layout.ORGANIC;
    }

    /** Adds one ear, the shortest kind available: a run of new rooms joined to the level at both ends. */
    private static boolean addEar(Graph g, Rng rng) {
        for (int len = 1; len <= 4; len++) {
            List<int[]> ears = new ArrayList<>();
            for (int c = 0; c < g.active.length; c++) {
                if (!g.active[c]) continue;
                for (int d = 0; d < 4; d++) {
                    int a = g.neighbour(c, d);
                    if (a >= 0 && !g.active[a]) collectEars(g, c, new int[]{a}, len, ears);
                }
            }
            if (ears.isEmpty()) continue;
            int[] ear = rng.pick(ears); // [anchor, new rooms..., anchor]
            for (int i = 0; i + 1 < ear.length; i++) g.connect(ear[i], ear[i + 1]);
            return true;
        }
        return false;
    }

    private static void collectEars(Graph g, int anchor, int[] path, int len, List<int[]> out) {
        int last = path[path.length - 1];
        if (path.length == len) {
            for (int d = 0; d < 4; d++) {
                int end = g.neighbour(last, d);
                if (end < 0 || !g.active[end] || end == anchor) continue;
                int[] ear = new int[len + 2];
                ear[0] = anchor;
                System.arraycopy(path, 0, ear, 1, len);
                ear[len + 1] = end;
                out.add(ear);
            }
            return;
        }
        for (int d = 0; d < 4; d++) {
            int n = g.neighbour(last, d);
            if (n < 0 || g.active[n] || contains(path, n)) continue;
            int[] next = Arrays.copyOf(path, path.length + 1);
            next[path.length] = n;
            collectEars(g, anchor, next, len, out);
        }
    }

    private static boolean contains(int[] a, int v) {
        for (int x : a) if (x == v) return true;
        return false;
    }

    /**
     * The doorway openings of the room containing (x, y), as world positions in the middle
     * of each gap in the wall ring. Empty when (x, y) is not inside a generated room grid.
     */
    public static List<double[]> doorways(TileMap map, double x, double y) {
        int cx = TileMap.toTile(x) / CHUNK, cy = TileMap.toTile(y) / CHUNK;
        List<double[]> out = new ArrayList<>();
        if (map.width % CHUNK != 0 || map.height % CHUNK != 0) return out;
        int ox = cx * CHUNK, oy = cy * CHUNK, last = CHUNK - 1;
        int[][] gaps = {{ox + DOOR_MID, oy}, {ox + last, oy + DOOR_MID}, {ox + DOOR_MID, oy + last}, {ox, oy + DOOR_MID}};
        for (int[] t : gaps) {
            if (map.inside(t[0], t[1]) && !map.solid(t[0], t[1])) out.add(new double[]{TileMap.center(t[0]), TileMap.center(t[1])});
        }
        return out;
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
        stamp(map, markers, t, ox, oy, doors, flipX, flipY, false, depth, cell);
    }

    /** turn swaps rows and columns (a quarter turn plus a mirror), so one template gives eight rooms. */
    public static void stamp(TileMap map, List<Marker> markers, ChunkTemplate t, int ox, int oy, int doors,
                             boolean flipX, boolean flipY, boolean turn, int depth, int cell) {
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
                char ch = turn ? t.at(y, x, flipX, flipY) : t.at(x, y, flipX, flipY);
                int tx = ox + 1 + x, ty = oy + 1 + y;
                map.set(tx, ty, Tile.fromSymbol(ch));
                if (Character.isLetter(ch) && ch != 'x') markers.add(new Marker(ch, tx, ty, depth, cell));
            }
        }
    }
}
