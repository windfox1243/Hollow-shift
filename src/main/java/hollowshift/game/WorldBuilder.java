package hollowshift.game;

import hollowshift.engine.Lang;
import hollowshift.engine.Rng;
import hollowshift.engine.gfx.Weathered;
import hollowshift.engine.io.Resources;
import hollowshift.entity.*;
import hollowshift.rules.Rule;
import hollowshift.story.ShiftDef;
import hollowshift.world.*;

import java.util.*;

/** Turns a {@link ShiftDef} into a populated {@link World}. */
public final class WorldBuilder {
    private static final ChunkLibrary LIBRARY = ChunkLibrary.load();

    private WorldBuilder() {
    }

    /** Seconds the Tenant sleeps at the start of every shift before it starts to hunt. */
    public static final double DORMANT = 10;

    /** Every shift, the final one included, is a freshly generated map. */
    public static World build(Services services, ShiftDef shift, long seed) {
        // vary the grid itself so the same shift never has quite the same shape twice
        Rng shape = new Rng(seed * 7 + 3);
        int gw = shift.cellsW() + shape.range(0, 2), gh = shift.cellsH() + shape.range(0, 2);
        if (shape.chance(0.4)) {
            int t = gw;
            gw = gh;
            gh = t;
        }
        WorldGenerator.Level level = new WorldGenerator(LIBRARY)
                .generate(seed, gw, gh, shift.rooms(), shift.loopChance());
        int breakers = Math.max(3, shift.fuses());
        Map<Job, Integer> jobs = drawJobs(shift, seed);
        Objectives objectives = shift.finale() ? new FinaleObjectives(breakers) : new ShiftObjectives(jobs);
        World world = new World(services, shift, level.map(), objectives, seed);
        Rng rng = new Rng(seed + 99);

        Map<Character, List<Marker>> byType = new HashMap<>();
        for (Marker m : level.markers()) byType.computeIfAbsent(m.type(), k -> new ArrayList<>()).add(m);

        // room centres for wandering
        for (int c = 0; c < level.depth().length; c++) {
            if (level.depth()[c] < 0) continue;
            int cx = c % level.cellsW() * WorldGenerator.CHUNK + WorldGenerator.CHUNK / 2;
            int cy = c / level.cellsW() * WorldGenerator.CHUNK + WorldGenerator.CHUNK / 2;
            if (!level.map().solid(cx, cy)) world.addRoomPoint(TileMap.center(cx), TileMap.center(cy));
        }
        for (Marker m : level.markers()) if (m.type() == 'i') world.addRoomPoint(m.x(), m.y());

        Marker playerSpot = first(byType, 'P');
        Marker liftSpot = first(byType, 'E');
        world.add(new Elevator(liftSpot.x(), liftSpot.y() + 8));
        world.add(new Player(playerSpot.x(), playerSpot.y()));

        Set<Marker> used = new HashSet<>();
        // fuse boxes (or the final shift's breakers): the deepest rooms first, at most one per room
        int machines = shift.finale() ? breakers : jobs.getOrDefault(Job.FUSE, 0);
        List<Marker> boxSpots = spreadPick(byType.getOrDefault('f', List.of()), machines, rng, used, true);
        if (boxSpots.size() < machines) {
            boxSpots.addAll(spreadPick(byType.getOrDefault('i', List.of()), machines - boxSpots.size(), rng, used, true));
        }
        for (Marker m : boxSpots) world.add(shift.finale() ? new Lever(m.x(), m.y()) : new FuseBox(m.x(), m.y()));
        // the other jobs, each machine in a room of its own; the gate key and the phone as deep as they get
        List<Marker> machineSpots = new ArrayList<>(byType.getOrDefault('f', List.of()));
        machineSpots.addAll(byType.getOrDefault('i', List.of()));
        for (Marker m : spreadPick(machineSpots, jobs.getOrDefault(Job.VALVE, 0), rng, used, false)) world.add(new Valve(m.x(), m.y()));
        for (Marker m : spreadPick(machineSpots, jobs.getOrDefault(Job.FUEL, 0), rng, used, false)) world.add(new Generator(m.x(), m.y()));
        for (Marker m : spreadPick(machineSpots, jobs.getOrDefault(Job.PHONE, 0), rng, used, true)) world.add(new Phone(m.x(), m.y()));
        for (Marker m : spreadPick(byType.getOrDefault('i', List.of()), jobs.getOrDefault(Job.KEY, 0), rng, used, true)) {
            world.add(new LiftKey(m.x(), m.y()));
        }
        if (shift.finale()) {
            // the knocking crack: somewhere deep, in a room of its own
            List<Marker> crack = spreadPick(byType.getOrDefault('i', List.of()), 1, rng, used, true);
            if (crack.isEmpty()) crack = spreadPick(byType.getOrDefault('f', List.of()), 1, rng, used, true);
            world.add(new Crack(crack.get(0).x(), crack.get(0).y()));
        }

        List<Marker> items = new ArrayList<>(byType.getOrDefault('i', List.of()));
        items.removeIf(used::contains);
        rng.shuffle(items);
        Deque<Marker> spots = new ArrayDeque<>(items);
        if (!shift.finale()) {
            int fuses = jobs.getOrDefault(Job.FUSE, 0);
            placeCarried(world, spots, fuses + Math.max(1, fuses / 2), used, Fuse::new);
            int generators = jobs.getOrDefault(Job.FUEL, 0);
            if (generators > 0) placeCarried(world, spots, generators + 1, used, FuelCan::new);
        }
        if (!shift.endless()) {
            // two notes per story shift, in the deepest free spots
            List<Marker> deep = new ArrayList<>(spots);
            deep.sort((a, b) -> Integer.compare(b.depth(), a.depth()));
            for (int i = 0; i < 2 && i < deep.size(); i++) {
                Marker m = deep.get(i);
                spots.remove(m);
                world.add(new Note(m.x(), m.y(), shift.number() * 2 - 1 + i));
            }
        }
        place(world, spots, 3 + shift.number(), Stone::new);
        place(world, spots, 1 + shift.number() / 2, Flare::new);
        // the lantern drains in every shift, so spare cells are always worth finding;
        // food is a second wind when running, and life itself under HUNGER
        place(world, spots, (shift.rules().has(Rule.BATTERY) ? 2 : 1) + shift.number() / 2, BatteryCell::new);
        place(world, spots, (shift.rules().has(Rule.HUNGER) ? 3 : 1) + shift.number() / 3, FoodCan::new);

        // under COLD the fires are few and far apart: the lift room's, then about one room in three,
        // spread out so you have to plan a route between them
        List<Marker> fires = byType.getOrDefault('h', List.of());
        if (shift.rules().has(Rule.COLD)) {
            for (Marker m : fires) if (m.depth() == 0) world.add(new Heater(m.x(), m.y()));
            // with SIGHT too the lantern must stay off, so the dark is colder company: one more fire
            int count = Math.max(2, countRooms(level) / 3) + (shift.rules().has(Rule.SIGHT) ? 1 : 0);
            for (Marker m : spreadPick(fires, count, rng, used, false)) {
                world.add(new Heater(m.x(), m.y()));
            }
        } else {
            for (Marker m : fires) if (rng.chance(0.15)) world.add(new Heater(m.x(), m.y()));
        }
        for (Marker m : byType.getOrDefault('l', List.of())) {
            double r = rng.next();
            // most of the old work lights are dead; a few still sputter. Even the lift room is dim.
            WallLamp.Mode mode = m.depth() == 0 ? (r < 0.5 ? WallLamp.Mode.FLICKER : WallLamp.Mode.DEAD)
                    : r < 0.12 ? WallLamp.Mode.STEADY : r < 0.4 ? WallLamp.Mode.FLICKER : WallLamp.Mode.DEAD;
            world.add(new WallLamp(m.x(), m.y(), mode));
        }

        // the Tenant starts in a deep room well away from the player, and only arrives
        // after a while: the first part of every shift is the not-knowing
        Marker start = playerSpot;
        int far = level.startCell();
        double farDist = -1;
        for (int c = 0; c < level.depth().length; c++) {
            if (level.depth()[c] < 0) continue;
            double[] p = openSpotInCell(level, c);
            double d = Math.hypot(p[0] - start.x(), p[1] - start.y());
            // prefer deep rooms, but distance on screen is what matters
            double score = d + level.depth()[c] * 40;
            if (score > farDist) {
                farDist = score;
                far = c;
            }
        }
        if (shift.rules().has(Rule.CONSUME)) placeGrowth(world, level, rng, playerSpot);
        placeWriting(world, level, rng);
        decorate(world, level, rng, playerSpot);
        double[] spawn = openSpotInCell(level, far);
        Creature tenant = world.add(new Creature(spawn[0], spawn[1], shift.creatureSpeed()));
        tenant.setDormant(DORMANT);
        return world;
    }

    private static int countRooms(WorldGenerator.Level level) {
        int n = 0;
        for (int d : level.depth()) if (d >= 0) n++;
        return n;
    }

    /**
     * CONSUME: tentacle nests in the walls and knots of flesh on the floor, never in the lift room.
     */
    private static void placeGrowth(World world, WorldGenerator.Level level, Rng rng, Marker playerSpot) {
        TileMap map = level.map();
        int rooms = countRooms(level);
        int maws = 2 + rooms / 3, nodes = 4 + rooms * 2 / 3;
        int c = WorldGenerator.CHUNK, mid = c / 2;
        List<double[]> taken = new ArrayList<>();
        for (Entity e : world.pendingEntities()) taken.add(new double[]{e.x, e.y});
        taken.add(new double[]{playerSpot.x(), playerSpot.y()});

        List<int[]> wallSpots = new ArrayList<>(), floorSpots = new ArrayList<>(), doorSpots = new ArrayList<>();
        int[] dx = {0, 1, 0, -1}, dy = {-1, 0, 1, 0};
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                if (map.solid(tx, ty)) continue;
                int cell = ty / c * level.cellsW() + tx / c;
                if (cell == level.startCell()) continue;
                int lx = tx % c, ly = ty % c;
                boolean ring = lx == 0 || ly == 0 || lx == c - 1 || ly == c - 1;
                if (ring) {
                    if (lx == mid || ly == mid) doorSpots.add(new int[]{tx, ty});
                    continue;
                }
                // keep the ways in and out of a room clear of wall nests
                boolean nearDoor = (Math.abs(lx - mid) <= 2 && (ly <= 2 || ly >= c - 3)) || (Math.abs(ly - mid) <= 2 && (lx <= 2 || lx >= c - 3));
                int wall = -1, solids = 0;
                for (int d = 0; d < 4; d++) {
                    if (map.solid(tx + dx[d], ty + dy[d])) {
                        solids++;
                        wall = d;
                    }
                }
                if (solids == 1 && !nearDoor) wallSpots.add(new int[]{tx, ty, wall});
                else if (solids == 0) floorSpots.add(new int[]{tx, ty});
            }
        }
        rng.shuffle(wallSpots);
        rng.shuffle(floorSpots);
        rng.shuffle(doorSpots);
        int placed = 0;
        for (int[] t : wallSpots) {
            if (placed >= maws) break;
            double x = TileMap.center(t[0]), y = TileMap.center(t[1]);
            if (!clear(taken, x, y, 90)) continue;
            world.add(new WallMaw(x, y, dx[t[2]], dy[t[2]]));
            taken.add(new double[]{x, y});
            placed++;
        }
        // knots of flesh, many of them, in the spots that hurt: right beside the things you
        // have to pick up or use, in doorways you have to squeeze through, and some just lying about
        List<double[]> knots = new ArrayList<>();
        for (Entity e : world.pendingEntities()) if (e instanceof WallMaw) knots.add(new double[]{e.x, e.y});
        List<Entity> bait = new ArrayList<>();
        for (Entity e : world.pendingEntities()) {
            if (e instanceof Interactable && !(e instanceof Elevator) || e instanceof Heater) bait.add(e);
        }
        rng.shuffle(bait);
        int nearBait = nodes * 2 / 5, inDoors = nodes * 7 / 20;
        int placedNodes = 0;
        for (Entity e : bait) {
            if (placedNodes >= nearBait) break;
            double a = rng.range(0, Math.PI * 2);
            for (int k = 0; k < 8; k++, a += Math.PI / 4) {
                double x = e.x + Math.cos(a) * 13, y = e.y + Math.sin(a) * 11;
                int tx = TileMap.toTile(x), ty = TileMap.toTile(y);
                if (map.solid(tx, ty) || ty / c * level.cellsW() + tx / c == level.startCell() || !clear(knots, x, y, 40)) continue;
                world.add(new FleshNode(x, y));
                knots.add(new double[]{x, y});
                placedNodes++;
                break;
            }
        }
        for (List<int[]> pool : List.of(doorSpots, floorSpots)) {
            int want = pool == doorSpots ? placedNodes + inDoors : nodes;
            for (int[] t : pool) {
                if (placedNodes >= want) break;
                double x = TileMap.center(t[0]), y = TileMap.center(t[1]);
                if (!clear(knots, x, y, 48) || Math.hypot(x - playerSpot.x(), y - playerSpot.y()) < 120) continue;
                world.add(new FleshNode(x, y));
                knots.add(new double[]{x, y});
                placedNodes++;
            }
        }
    }

    /**
     * Furnishes every room according to what it was used for ({@link RoomTheme}). Heavy props
     * stand in short rows against the walls (a line of barrels, a row of graves) and block their
     * tiles, each checked not to cut off any part of the map. Clutter lies mostly along the walls
     * and beside the heavy props, so the middle of a room stays clear; cobwebs go in corners.
     */
    private static void decorate(World world, WorldGenerator.Level level, Rng rng, Marker playerSpot) {
        TileMap map = level.map();
        int c = WorldGenerator.CHUNK, mid = c / 2;
        List<double[]> taken = new ArrayList<>();
        for (Entity e : world.pendingEntities()) taken.add(new double[]{e.x, e.y});
        taken.add(new double[]{playerSpot.x(), playerSpot.y()});
        // per room: wall spots {tx, ty, wallDir}, corner spots, open floor spots
        Map<Integer, List<int[]>> walls = new HashMap<>(), floors = new HashMap<>(), corners = new HashMap<>();
        int[] dx = {0, 1, 0, -1}, dy = {-1, 0, 1, 0};
        for (int ty = 0; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                if (map.solid(tx, ty)) continue;
                int lx = tx % c, ly = ty % c;
                if (lx == 0 || ly == 0 || lx == c - 1 || ly == c - 1) continue;
                // doorway lanes stay clear
                if (Math.abs(lx - mid) <= 1 && (ly <= 2 || ly >= c - 3) || Math.abs(ly - mid) <= 1 && (lx <= 2 || lx >= c - 3)) continue;
                int cell = ty / c * level.cellsW() + tx / c;
                int solids = 0, wall = -1;
                for (int d = 0; d < 4; d++) {
                    if (map.solid(tx + dx[d], ty + dy[d])) {
                        solids++;
                        wall = d;
                    }
                }
                if (solids >= 2) corners.computeIfAbsent(cell, k -> new ArrayList<>()).add(new int[]{tx, ty});
                else if (solids == 1) walls.computeIfAbsent(cell, k -> new ArrayList<>()).add(new int[]{tx, ty, wall});
                else floors.computeIfAbsent(cell, k -> new ArrayList<>()).add(new int[]{tx, ty});
            }
        }
        int start = TileMap.toTile(playerSpot.x()) + TileMap.toTile(playerSpot.y()) * map.width;
        int[] reachable = {flood(map, start)};
        for (int cell = 0; cell < level.depth().length; cell++) {
            if (level.depth()[cell] < 0) continue;
            RoomTheme theme = RoomTheme.of(level.roomNames()[cell]);
            List<int[]> heavySpots = placeHeavy(world, map, rng, taken, walls.get(cell), theme, start, reachable);
            placeFlat(world, rng, taken, corners.get(cell), rng.range(0, 3), new String[]{"decor_web"}, false);
            // clutter: most of it by the walls and the heavy props, a little out in the open
            int clutter = rng.range(theme.flatMin, theme.flatMax);
            List<int[]> edge = new ArrayList<>();
            for (int[] t : walls.getOrDefault(cell, List.of())) if (map.get(t[0], t[1]) != Tile.PROP) edge.add(t);
            for (int[] h : heavySpots) {
                for (int d = 0; d < 4; d++) {
                    int nx = h[0] + dx[d], ny = h[1] + dy[d];
                    if (!map.solid(nx, ny) && ny / c * level.cellsW() + nx / c == cell) edge.add(new int[]{nx, ny});
                }
            }
            int near = placeFlat(world, rng, taken, edge, clutter - clutter / 4, theme.flat, true);
            placeFlat(world, rng, taken, floors.get(cell), clutter - near, theme.flat, true);
        }
    }

    /** Rows of the theme's heavy props along the walls. Returns the tiles they stand on. */
    private static List<int[]> placeHeavy(World world, TileMap map, Rng rng, List<double[]> taken, List<int[]> spots,
                                          RoomTheme theme, int start, int[] reachable) {
        List<int[]> placed = new ArrayList<>();
        if (spots == null || theme.heavy.length == 0) return placed;
        Map<Long, int[]> byTile = new HashMap<>();
        for (int[] t : spots) byTile.put(key(t[0], t[1]), t);
        List<int[]> pool = new ArrayList<>(spots);
        rng.shuffle(pool);
        int want = rng.range(theme.heavyMin, theme.heavyMax);
        for (int[] anchor : pool) {
            if (placed.size() >= want) break;
            if (map.get(anchor[0], anchor[1]) != Tile.FLOOR) continue;
            // a row along the wall: the same prop, or its neighbour in the list
            int run = rng.range(1, theme.maxRun + 1);
            String sprite = theme.heavy[rng.range(0, theme.heavy.length)];
            int stepX = anchor[2] % 2 == 0 ? 1 : 0, stepY = 1 - stepX; // along a N/S wall step in x, along E/W in y
            int[] cur = anchor;
            for (int i = 0; i < run && cur != null && placed.size() < want; i++) {
                if (!putHeavy(world, map, taken, cur, sprite, rng, start, reachable)) break;
                placed.add(cur);
                int[] next = byTile.get(key(cur[0] + stepX, cur[1] + stepY));
                cur = next != null && next[2] == anchor[2] ? next : null;
            }
        }
        return placed;
    }

    private static boolean putHeavy(World world, TileMap map, List<double[]> taken, int[] t, String sprite, Rng rng,
                                    int start, int[] reachable) {
        double x = TileMap.center(t[0]), y = TileMap.center(t[1]);
        if (map.get(t[0], t[1]) != Tile.FLOOR || !clear(taken, x, y, 15)) return false;
        map.set(t[0], t[1], Tile.PROP);
        // never wall off a tile: everything that was reachable must stay reachable
        if (flood(map, start) != reachable[0] - 1) {
            map.set(t[0], t[1], Tile.FLOOR);
            return false;
        }
        reachable[0]--;
        world.add(new Decor(x, y, sprite, false, rng.chance(0.5)));
        taken.add(new double[]{x, y});
        return true;
    }

    private static long key(int x, int y) {
        return (long) x << 32 | (y & 0xffffffffL);
    }

    private static int placeFlat(World world, Rng rng, List<double[]> taken, List<int[]> spots, int count, String[] sprites, boolean jitter) {
        if (spots == null || count <= 0) return 0;
        List<int[]> pool = new ArrayList<>(spots);
        rng.shuffle(pool);
        int placed = 0;
        for (int[] t : pool) {
            if (placed >= count) break;
            double x = TileMap.center(t[0]) + (jitter ? rng.range(-4, 4) : 0), y = TileMap.center(t[1]) + (jitter ? rng.range(-3, 3) : 0);
            if (!clear(taken, x, y, 20)) continue;
            world.add(new Decor(x, y, sprites[rng.range(0, sprites.length)], true, rng.chance(0.5)));
            taken.add(new double[]{x, y});
            placed++;
        }
        return placed;
    }

    /**
     * A painted sign by the way into every room, named for what the room was ("STORE 3",
     * "CREW ROOM B"), and scrawls on the walls of about half the rooms, matching the room.
     */
    private static void placeWriting(World world, WorldGenerator.Level level, Rng rng) {
        TileMap map = level.map();
        int c = WorldGenerator.CHUNK, mid = c / 2;
        List<double[]> taken = new ArrayList<>();
        for (Entity e : world.pendingEntities()) taken.add(new double[]{e.x, e.y});
        Map<RoomTheme, Integer> numbers = new EnumMap<>(RoomTheme.class);
        Map<Integer, double[]> signs = new HashMap<>();
        Map<Integer, List<int[]>> northWalls = new HashMap<>();
        for (int ty = 1; ty < map.height; ty++) {
            for (int tx = 0; tx < map.width; tx++) {
                int lx = tx % c, ly = ty % c;
                // real rock above (not a crate), so the words are on the wall face
                if (map.solid(tx, ty) || map.get(tx, ty - 1) != Tile.WALL || lx == 0 || ly == 0 || lx == c - 1 || ly == c - 1) continue;
                if (Math.abs(lx - mid) <= 1 && ly <= 2) continue; // not over the north doorway
                northWalls.computeIfAbsent(ty / c * level.cellsW() + tx / c, k -> new ArrayList<>()).add(new int[]{tx, ty});
            }
        }
        for (int cell = 0; cell < level.depth().length; cell++) {
            if (level.depth()[cell] < 0 || !northWalls.containsKey(cell)) continue;
            RoomTheme theme = RoomTheme.of(level.roomNames()[cell]);
            double rcx = TileMap.center(cell % level.cellsW() * c + mid), rcy = TileMap.center(cell / level.cellsW() * c + mid);
            List<double[]> doors = WorldGenerator.doorways(map, rcx, rcy);
            // numbered like a real mine: crew rooms A, B, C...; the rest count up with a few gaps
            int n = theme == RoomTheme.QUARTERS ? numbers.merge(theme, 1, Integer::sum)
                    : numbers.merge(theme, rng.range(1, 4), (prev, step) -> prev + 1 + rng.range(0, 2));
            String label = theme == RoomTheme.QUARTERS ? Lang.t("sign.QUARTERS", (char) ('A' + n - 1)) : Lang.t("sign." + theme.name(), n);
            // the spot on the north wall nearest a way in
            double[] best = null;
            double bestD = Double.MAX_VALUE;
            for (int[] t : northWalls.get(cell)) {
                double x = TileMap.center(t[0]), y = TileMap.center(t[1]);
                if (!clear(taken, x, y, 20) || !fitsOnWall(map, t[0], t[1], Weathered.width(label))) continue;
                double d = Double.MAX_VALUE;
                for (double[] dr : doors) d = Math.min(d, Math.hypot(dr[0] - x, dr[1] - y));
                if (d < bestD) {
                    bestD = d;
                    best = new double[]{x, y};
                }
            }
            if (best == null) continue;
            RoomSign.Damage damage = theme == RoomTheme.SHRINE ? RoomSign.Damage.CLAWED
                    : theme == RoomTheme.FLOODED ? RoomSign.Damage.MUDDY : RoomSign.Damage.NONE;
            world.add(new RoomSign(best[0], best[1], label, damage, theme == RoomTheme.LIFT));
            taken.add(best);
            signs.put(cell, best);
        }
        // scrawls: never the same line twice in one map
        List<Integer> rooms = new ArrayList<>(northWalls.keySet());
        rooms.removeIf(cell -> level.depth()[cell] < 0);
        rng.shuffle(rooms);
        Set<String> used = new HashSet<>();
        int want = Math.max(2, rooms.size() / 2);
        for (int cell : rooms) {
            if (want <= 0) break;
            RoomTheme theme = RoomTheme.of(level.roomNames()[cell]);
            // a line tagged "[RULE] ..." hints at a rule, so it only turns up while that rule is in force
            List<String> lines = new ArrayList<>();
            for (String l : Lang.list("scrawl." + theme.name())) {
                if (l.startsWith("[")) {
                    if (!world.rules.has(Rule.valueOf(l.substring(1, l.indexOf(']'))))) continue;
                    l = l.substring(l.indexOf(']') + 1).trim();
                }
                lines.add(l);
            }
            lines.removeAll(used);
            if (lines.isEmpty()) continue;
            List<int[]> spots = new ArrayList<>(northWalls.get(cell));
            rng.shuffle(spots);
            for (int[] t : spots) {
                double x = TileMap.center(t[0]), y = TileMap.center(t[1]);
                double[] sign = signs.get(cell);
                String line = lines.get(rng.range(0, lines.size()));
                if (sign != null && Math.hypot(sign[0] - x, sign[1] - y) < 48 || !clear(taken, x, y, 20)
                        || !fitsOnWall(map, t[0], t[1], Weathered.width(line))) continue;
                used.add(line);
                world.add(new Scrawl(x, y, line, rng.chance(0.7), rng.range(-2, 3)));
                taken.add(new double[]{x, y});
                want--;
                break;
            }
        }
    }

    /**
     * Whether lettering this many pixels wide, centred over tile (tx, ty), lies wholly on rock:
     * every tile it covers has a wall above it with its face showing.
     */
    private static boolean fitsOnWall(TileMap map, int tx, int ty, int width) {
        int half = (width + 1) / 2;
        int from = TileMap.toTile(TileMap.center(tx) - half), to = TileMap.toTile(TileMap.center(tx) + half - 1);
        for (int x = from; x <= to; x++) {
            if (map.get(x, ty - 1) != Tile.WALL || map.get(x, ty).opaque) return false;
        }
        return true;
    }

    /** Number of open tiles reachable from tile index start. */
    private static int flood(TileMap map, int start) {
        boolean[] seen = new boolean[map.width * map.height];
        int[] queue = new int[map.width * map.height];
        int head = 0, tail = 0, count = 0;
        queue[tail++] = start;
        seen[start] = true;
        while (head < tail) {
            int i = queue[head++];
            count++;
            int x = i % map.width, y = i / map.width;
            int[] next = {i - 1, i + 1, i - map.width, i + map.width};
            boolean[] ok = {x > 0, x < map.width - 1, y > 0, y < map.height - 1};
            for (int k = 0; k < 4; k++) {
                if (!ok[k] || seen[next[k]]) continue;
                if (map.solid(next[k] % map.width, next[k] / map.width)) continue;
                seen[next[k]] = true;
                queue[tail++] = next[k];
            }
        }
        return count;
    }

    private static boolean clear(List<double[]> taken, double x, double y, double gap) {
        for (double[] p : taken) if (Math.hypot(p[0] - x, p[1] - y) < gap) return false;
        return true;
    }

    /**
     * The lift's jobs for one run: the fuses (one more on some runs) plus {@code shift.jobs()}
     * other kinds drawn at random, so the same shift asks for different work each time.
     */
    public static Map<Job, Integer> drawJobs(ShiftDef shift, long seed) {
        Rng rng = new Rng(seed * 13 + 5);
        Map<Job, Integer> jobs = new EnumMap<>(Job.class);
        jobs.put(Job.FUSE, shift.fuses() + (shift.number() > 1 && rng.chance(0.5) ? 1 : 0));
        List<Job> pool = new ArrayList<>(List.of(Job.EXTRAS));
        rng.shuffle(pool);
        boolean late = shift.number() >= 4;
        for (int i = 0; i < Math.min(shift.jobs(), pool.size()); i++) {
            Job j = pool.get(i);
            jobs.put(j, switch (j) {
                case VALVE -> 1 + (rng.chance(0.5) ? 1 : 0) + (late ? 1 : 0);
                case FUEL -> 1 + (late && rng.chance(0.4) ? 1 : 0);
                default -> 1;
            });
        }
        return jobs;
    }

    /** Things carried in both hands (fuses, fuel) go to rooms other than the start room when possible. */
    private static void placeCarried(World world, Deque<Marker> spots, int count, Set<Marker> used, ItemFactory f) {
        List<Marker> deep = new ArrayList<>();
        for (Marker m : spots) if (m.depth() > 0) deep.add(m);
        for (int i = 0; i < count && !deep.isEmpty(); i++) {
            Marker m = deep.remove(0);
            spots.remove(m);
            used.add(m);
            world.add(f.create(m.x(), m.y()));
        }
    }

    private interface ItemFactory {
        Entity create(double x, double y);
    }

    private static void place(World world, Deque<Marker> spots, int count, ItemFactory f) {
        for (int i = 0; i < count && !spots.isEmpty(); i++) {
            Marker m = spots.poll();
            world.add(f.create(m.x(), m.y()));
        }
    }

    /** Picks up to n markers in distinct rooms, preferring deep rooms. */
    private static List<Marker> spreadPick(List<Marker> candidates, int n, Rng rng, Set<Marker> used, boolean deepFirst) {
        List<Marker> pool = new ArrayList<>(candidates);
        pool.removeIf(m -> used.contains(m) || m.depth() == 0);
        rng.shuffle(pool);
        if (deepFirst) {
            // deepest first, with a little jitter so the same rooms are not always chosen
            Map<Marker, Double> key = new HashMap<>();
            for (Marker m : pool) key.put(m, m.depth() + rng.range(0, 1.5));
            pool.sort((a, b) -> Double.compare(key.get(b), key.get(a)));
        }
        List<Marker> out = new ArrayList<>();
        Set<Integer> cells = new HashSet<>();
        for (Marker m : pool) {
            if (out.size() >= n) break;
            if (cells.add(m.cell())) {
                out.add(m);
                used.add(m);
            }
        }
        return out;
    }

    private static Marker first(Map<Character, List<Marker>> byType, char type) {
        List<Marker> l = byType.get(type);
        if (l == null || l.isEmpty()) throw new IllegalStateException("start room is missing marker " + type);
        return l.get(0);
    }

    private static double[] openSpotInCell(WorldGenerator.Level level, int cell) {
        int ox = cell % level.cellsW() * WorldGenerator.CHUNK, oy = cell / level.cellsW() * WorldGenerator.CHUNK;
        int mid = WorldGenerator.CHUNK / 2;
        for (int r = 0; r < mid; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    int tx = ox + mid + dx, ty = oy + mid + dy;
                    if (!level.map().solid(tx, ty) && !level.map().solid(tx + 1, ty) && !level.map().solid(tx - 1, ty))
                        return new double[]{TileMap.center(tx), TileMap.center(ty)};
                }
        return new double[]{TileMap.center(ox + mid), TileMap.center(oy + mid)};
    }
}
