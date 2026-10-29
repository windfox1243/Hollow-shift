package hollowshift.game;

import hollowshift.engine.Rng;
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

    public static World build(Services services, ShiftDef shift, long seed) {
        return shift.finale() ? buildFinale(services, shift, seed) : buildGenerated(services, shift, seed);
    }

    private static World buildGenerated(Services services, ShiftDef shift, long seed) {
        WorldGenerator.Level level = new WorldGenerator(LIBRARY)
                .generate(seed, shift.cellsW(), shift.cellsH(), shift.rooms(), shift.loopChance());
        World world = new World(services, shift, level.map(), new FuseObjectives(shift.fuses()), seed);
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
        // fuse boxes: the deepest rooms first, at most one per room
        List<Marker> boxSpots = spreadPick(byType.getOrDefault('f', List.of()), shift.fuses(), rng, used, true);
        if (boxSpots.size() < shift.fuses()) {
            boxSpots.addAll(spreadPick(byType.getOrDefault('i', List.of()), shift.fuses() - boxSpots.size(), rng, used, true));
        }
        for (Marker m : boxSpots) world.add(new FuseBox(m.x(), m.y()));

        List<Marker> items = new ArrayList<>(byType.getOrDefault('i', List.of()));
        items.removeIf(used::contains);
        rng.shuffle(items);
        Deque<Marker> spots = new ArrayDeque<>(items);
        int fuses = shift.fuses() + Math.max(1, shift.fuses() / 2);
        placeFuses(world, spots, fuses, used);
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

        boolean cold = shift.rules().has(Rule.COLD);
        for (Marker m : byType.getOrDefault('h', List.of())) {
            if (cold ? rng.chance(0.7) : rng.chance(0.15)) world.add(new Heater(m.x(), m.y()));
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
        double[] spawn = openSpotInCell(level, far);
        Creature tenant = world.add(new Creature(spawn[0], spawn[1], shift.creatureSpeed()));
        tenant.setDormant(rng.range(20.0, 30.0));
        return world;
    }

    /** Fuses go to rooms other than the start room when possible. */
    private static void placeFuses(World world, Deque<Marker> spots, int count, Set<Marker> used) {
        List<Marker> deep = new ArrayList<>();
        for (Marker m : spots) if (m.depth() > 0) deep.add(m);
        for (int i = 0; i < count && !deep.isEmpty(); i++) {
            Marker m = deep.remove(0);
            spots.remove(m);
            used.add(m);
            world.add(new Fuse(m.x(), m.y()));
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

    // ---- final shift: hand-made map ---------------------------------------------------------------

    /** A hand-made map: tiles plus the marker letters found in it. */
    public record HandMap(TileMap map, List<Marker> markers) {
    }

    public static HandMap parseHandMap(String text) {
        List<String> rows = new ArrayList<>();
        for (String line : text.split("\r?\n")) {
            if (!line.startsWith(";") && !line.isBlank()) rows.add(line);
        }
        int w = rows.stream().mapToInt(String::length).max().orElse(0);
        TileMap map = new TileMap(w, rows.size());
        List<Marker> markers = new ArrayList<>();
        for (int y = 0; y < rows.size(); y++) {
            String r = rows.get(y);
            for (int x = 0; x < w; x++) {
                char c = x < r.length() ? r.charAt(x) : ' ';
                map.set(x, y, c == ' ' ? Tile.VOID : Tile.fromSymbol(c));
                if (Character.isLetter(c) && c != 'x') markers.add(new Marker(c, x, y, 0, 0));
            }
        }
        return new HandMap(map, markers);
    }

    private static World buildFinale(Services services, ShiftDef shift, long seed) {
        HandMap hand = parseHandMap(Resources.require("/chunks/finale.txt"));
        TileMap map = hand.map();
        List<Marker> markers = hand.markers();
        FinaleObjectives obj = new FinaleObjectives((int) markers.stream().filter(m -> m.type() == 'L').count());
        World world = new World(services, shift, map, obj, seed);
        Rng rng = new Rng(seed + 7);
        int noteIndex = 0, supplies = 0;
        for (Marker m : markers) {
            if (m.type() == 'i' && noteIndex < 2) {
                // the last two notes of the story wait in the final map
                world.add(new Note(m.x(), m.y(), shift.number() * 2 - 1 + noteIndex++));
            } else if (m.type() == 'i' && supplies < 3) {
                // two spare cells and a can for the long walk through the lever halls
                world.add(supplies++ < 2 ? new BatteryCell(m.x(), m.y()) : new FoodCan(m.x(), m.y()));
            }
            switch (m.type()) {
                case 'P' -> world.add(new Player(m.x(), m.y()));
                case 'E' -> world.add(new Elevator(m.x(), m.y() + 8));
                case 'L' -> world.add(new Lever(m.x(), m.y()));
                case 'K' -> world.add(new Crack(m.x(), m.y()));
                case 'h' -> world.add(new Heater(m.x(), m.y()));
                case 'l' -> world.add(new WallLamp(m.x(), m.y(), rng.chance(0.6) ? WallLamp.Mode.FLICKER : WallLamp.Mode.STEADY));
                case 'C' -> world.add(new Creature(m.x(), m.y(), shift.creatureSpeed())).setDormant(10);
                case 's' -> world.add(new Stone(m.x(), m.y()));
                case 'r' -> world.add(new Flare(m.x(), m.y()));
                default -> {
                    // 'i' and other markers only serve as wander points
                }
            }
            world.addRoomPoint(m.x(), m.y());
        }
        return world;
    }
}
