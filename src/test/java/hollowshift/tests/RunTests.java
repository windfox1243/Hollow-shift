package hollowshift.tests;

import hollowshift.ai.ChaseState;
import hollowshift.engine.Input;
import hollowshift.engine.Rng;
import hollowshift.engine.audio.Audio;
import hollowshift.engine.gfx.Art;
import hollowshift.engine.io.Json;
import hollowshift.engine.io.Resources;
import hollowshift.entity.*;
import hollowshift.entity.Player;
import hollowshift.game.*;
import hollowshift.rules.*;
import hollowshift.story.Campaign;
import hollowshift.story.SaveData;
import hollowshift.story.ShiftDef;
import hollowshift.world.*;

import java.awt.event.KeyEvent;
import java.util.*;

/** Self-contained test runner (no JUnit needed): java tools/Build.java test */
public final class RunTests {
    private static int passed;
    private static int failed;

    private interface Test {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        test("json parses, tolerates comments and round-trips", RunTests::json);
        test("room templates keep doorways open and connected", RunTests::templates);
        test("same seed builds the same level", RunTests::determinism);
        test("generated levels are fully connected", RunTests::connectivity);
        test("pathfinder reaches every fuse box spot", RunTests::pathfinding);
        test("rule deck deals valid rule sets", RunTests::ruleDeck);
        test("campaign and final map load and are reachable", RunTests::campaign);
        test("English and Vietnamese text files have the same keys", RunTests::languages);
        test("SIGHT: dark player is safe, lit player is seen", RunTests::sightRule);
        test("no SIGHT rule: a lit player is not seen", RunTests::noSightRule);
        test("HEARING: running nearby is heard, walking far is not", RunTests::hearingRule);
        test("touching the Tenant kills the player", RunTests::touchKills);
        test("COLD rule freezes a player in the dark", RunTests::coldRule);
        test("the Tenant strides straight through a solid wall", RunTests::throughWalls);
        test("up close the Tenant grabs with its hands", RunTests::grab);
        test("SMELL: a player who runs can shake off the Tenant", RunTests::outrunSmell);
        test("hidden ending: the crack opens only with all notes read", RunTests::hiddenEnding);
        test("pockets: items are stashed, capped and used from the pack", RunTests::pockets);
        test("a note picked up is kept in the journal", RunTests::notesKept);
        test("the lantern drains in every shift, faster under BATTERY", RunTests::lanternDrains);
        test("every shift runs 90 simulated seconds with a random bot", RunTests::simulation);
        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void test(String name, Test t) {
        try {
            t.run();
            passed++;
            System.out.println("  ok    " + name);
        } catch (Throwable e) {
            failed++;
            System.out.println("  FAIL  " + name + "\n        " + e);
            for (StackTraceElement el : e.getStackTrace()) {
                if (el.getClassName().startsWith("hollowshift")) {
                    System.out.println("        at " + el);
                    break;
                }
            }
        }
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    // ---- data ---------------------------------------------------------------------------

    private static void json() {
        Map<String, Object> m = Json.parseObject("// c\n{\"a\": [1, 2.5, \"x\\ny\", true, null,], /* b */ \"b\": {\"c\": -3}}");
        check(Json.list(m.get("a")).size() == 5, "array size");
        check(Json.integer(Json.obj(m.get("b")).get("c"), 0) == -3, "nested number");
        Object again = Json.parse(Json.write(m));
        check(Json.write(again).equals(Json.write(m)), "round trip");
    }

    private static void templates() {
        ChunkLibrary lib = ChunkLibrary.load();
        check(lib.rooms().size() >= 10, "expected at least 10 room templates, got " + lib.rooms().size());
        check(!lib.starts().isEmpty(), "missing start template");
        List<ChunkTemplate> all = new ArrayList<>(lib.rooms());
        all.addAll(lib.starts());
        for (ChunkTemplate t : all) {
            for (int flip = 0; flip < 4; flip++) {
                // every template is checked inside a room with all four doorways open
                TileMap map = new TileMap(WorldGenerator.CHUNK, WorldGenerator.CHUNK);
                List<Marker> markers = new ArrayList<>();
                stamp(map, markers, t, flip);
                int[] start = {WorldGenerator.CHUNK / 2, 0};
                boolean[] seen = flood(map, start[0], start[1]);
                int c = WorldGenerator.CHUNK;
                int[][] doors = {{c / 2, 0}, {c / 2, c - 1}, {0, c / 2}, {c - 1, c / 2}};
                for (int[] d : doors) check(seen[d[1] * c + d[0]], t.name() + " flip " + flip + ": doorway " + Arrays.toString(d) + " unreachable");
                for (Marker m : markers) check(seen[m.ty() * c + m.tx()], t.name() + ": marker " + m.type() + " unreachable");
            }
        }
        check(lib.starts().get(0).rows().stream().anyMatch(r -> r.indexOf('E') >= 0), "start room needs an elevator marker");
        check(lib.starts().get(0).rows().stream().anyMatch(r -> r.indexOf('P') >= 0), "start room needs a player marker");
    }

    /** Stamps a template with all four doorways open; flip bit 0 mirrors X, bit 1 mirrors Y. */
    private static void stamp(TileMap map, List<Marker> markers, ChunkTemplate t, int flip) {
        int all = WorldGenerator.N | WorldGenerator.E | WorldGenerator.S | WorldGenerator.W;
        WorldGenerator.stamp(map, markers, t, 0, 0, all, (flip & 1) != 0, (flip & 2) != 0, 0, 0);
    }

    private static boolean[] flood(TileMap map, int sx, int sy) {
        boolean[] seen = new boolean[map.width * map.height];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sx, sy});
        seen[sy * map.width + sx] = true;
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int d = 0; d < 4; d++) {
                int nx = p[0] + dx[d], ny = p[1] + dy[d];
                if (!map.inside(nx, ny) || map.solid(nx, ny) || seen[ny * map.width + nx]) continue;
                seen[ny * map.width + nx] = true;
                q.add(new int[]{nx, ny});
            }
        }
        return seen;
    }

    private static void determinism() {
        WorldGenerator gen = new WorldGenerator(ChunkLibrary.load());
        WorldGenerator.Level a = gen.generate(42, 5, 4, 12, 0.25);
        WorldGenerator.Level b = gen.generate(42, 5, 4, 12, 0.25);
        WorldGenerator.Level c = gen.generate(43, 5, 4, 12, 0.25);
        check(a.markers().equals(b.markers()), "markers differ for same seed");
        boolean differs = false;
        for (int y = 0; y < a.map().height; y++)
            for (int x = 0; x < a.map().width; x++) {
                check(a.map().get(x, y) == b.map().get(x, y), "tile differs at " + x + "," + y);
                differs |= a.map().get(x, y) != c.map().get(x, y);
            }
        check(differs, "different seeds produced identical maps");
    }

    private static void connectivity() {
        WorldGenerator gen = new WorldGenerator(ChunkLibrary.load());
        for (int seed = 0; seed < 60; seed++) {
            int w = 3 + seed % 6, h = 3 + seed % 5;
            int rooms = Math.max(4, w * h * 2 / 3);
            WorldGenerator.Level lvl = gen.generate(seed, w, h, rooms, 0.3);
            Marker p = lvl.markers().stream().filter(m -> m.type() == 'P').findFirst().orElseThrow();
            boolean[] seen = flood(lvl.map(), p.tx(), p.ty());
            for (int y = 0; y < lvl.map().height; y++)
                for (int x = 0; x < lvl.map().width; x++)
                    if (!lvl.map().solid(x, y)) check(seen[y * lvl.map().width + x], "seed " + seed + ": tile " + x + "," + y + " unreachable");
            long count = Arrays.stream(lvl.depth()).filter(d -> d >= 0).count();
            check(count == rooms, "seed " + seed + ": expected " + rooms + " rooms, got " + count);
            long boxes = lvl.markers().stream().filter(m -> (m.type() == 'f' || m.type() == 'i') && m.depth() > 0).count();
            check(boxes >= 7, "seed " + seed + ": too few spots for fuses/boxes: " + boxes);
        }
    }

    private static void pathfinding() {
        WorldGenerator.Level lvl = new WorldGenerator(ChunkLibrary.load()).generate(7, 6, 5, 20, 0.25);
        Pathfinder pf = new Pathfinder(lvl.map());
        Marker p = lvl.markers().stream().filter(m -> m.type() == 'P').findFirst().orElseThrow();
        for (Marker m : lvl.markers()) {
            if (m.type() != 'f') continue;
            List<double[]> path = pf.find(p.x(), p.y(), m.x(), m.y(), 50_000);
            check(!path.isEmpty(), "no path to fuse box spot at " + m.tx() + "," + m.ty());
            double[] end = path.get(path.size() - 1);
            check(TileMap.toTile(end[0]) == m.tx() && TileMap.toTile(end[1]) == m.ty(), "path ends at wrong tile");
        }
    }

    private static void ruleDeck() {
        for (int shift = 1; shift <= 12; shift++) {
            for (int s = 0; s < 50; s++) {
                RuleSet rs = RuleDeck.draw(shift, new Rng(s * 101L + shift));
                List<Rule> list = rs.ordered();
                check(new HashSet<>(list).size() == list.size(), "duplicate rule");
                if (shift == 1) check(list.isEmpty(), "shift 1 should have no rules");
                else check(list.stream().anyMatch(Rule::isSense), "shift " + shift + " needs a sense rule: " + list);
                check(list.size() <= RuleDeck.ruleCount(shift), "too many rules");
                check(list.stream().filter(r -> r.subject == Rule.Subject.YOU).count() <= 2, "too many survival rules");
                if (shift < 3) check(!rs.has(Rule.ECHO), "ECHO too early");
                if (shift < 4) check(!rs.has(Rule.HASTE), "HASTE too early");
            }
        }
    }

    private static void campaign() {
        Campaign c = Campaign.load();
        check(c.shifts().size() == 5, "expected 5 story shifts");
        check(c.shifts().get(c.shifts().size() - 1).finale(), "last shift should be the finale");
        check(!c.tips().isEmpty() && !c.ending().isEmpty(), "tips/ending missing");
        ShiftDef a = c.endless(4, 99), b = c.endless(4, 99);
        check(a.rules().ordered().equals(b.rules().ordered()), "endless rules not deterministic");
        WorldBuilder.HandMap hand = WorldBuilder.parseHandMap(Resources.require("/chunks/finale.txt"));
        Marker p = hand.markers().stream().filter(m -> m.type() == 'P').findFirst().orElseThrow();
        boolean[] seen = flood(hand.map(), p.tx(), p.ty());
        for (Marker m : hand.markers()) check(seen[m.ty() * hand.map().width + m.tx()], "finale marker " + m.type() + " unreachable");
        check(hand.markers().stream().filter(m -> m.type() == 'L').count() == 3, "finale needs 3 breakers");
    }

    private static void languages() {
        Map<String, Object> en = Json.parseObject(Resources.require("/lang/en.json"));
        Map<String, Object> vi = Json.parseObject(Resources.require("/lang/vi.json"));
        for (String k : en.keySet()) check(vi.containsKey(k), "vi.json is missing " + k);
        for (String k : vi.keySet()) check(en.containsKey(k), "en.json is missing " + k);
        check(Json.list(en.get("tips")).size() == Json.list(vi.get("tips")).size(), "tip counts differ");
        for (Map.Entry<String, Object> e : en.entrySet()) {
            if (!(e.getValue() instanceof String a)) continue;
            String b = (String) vi.get(e.getKey());
            for (int i = 0; i < 4; i++) {
                String ph = "{" + i + "}";
                check(a.contains(ph) == b.contains(ph), e.getKey() + ": placeholder " + ph + " differs");
            }
        }
        // every rule, death cause and action has its text
        for (hollowshift.rules.Rule r : hollowshift.rules.Rule.values())
            for (String part : new String[]{"title", "hint", "short"}) check(en.containsKey("rule." + r + "." + part), "missing rule text " + r);
        for (DeathCause d : DeathCause.values()) check(en.containsKey("death." + d + ".title"), "missing death text " + d);
        for (hollowshift.engine.Action a : hollowshift.engine.Action.values()) check(en.containsKey("action." + a), "missing action " + a);
    }

    // ---- rules in a controlled room ---------------------------------------------------------------

    private static Services services() {
        Audio audio = new Audio();
        audio.setVolumes(0, 0, 0);
        return new Services(audio, new Art(), new Input(), new SaveData());
    }

    /** An open 30x14 room with the player at the left and the Tenant some distance to the right. */
    private static World room(Services sv, double creatureDistance, Rule... rules) {
        TileMap map = new TileMap(30, 14);
        for (int y = 1; y < 13; y++) for (int x = 1; x < 29; x++) map.set(x, y, Tile.FLOOR);
        ShiftDef def = new ShiftDef(1, "test", "", 0, 0, 0, 1, 0, 1, RuleSet.of(rules), false, false);
        World w = new World(sv, def, map, new FuseObjectives(1), 1);
        w.addRoomPoint(TileMap.center(25), TileMap.center(7));
        w.add(new Player(TileMap.center(4), TileMap.center(7)));
        w.add(new Creature(TileMap.center(4) + creatureDistance, TileMap.center(7), 0.0001));
        w.start();
        return w;
    }

    private static void tick(World w, int ticks) {
        for (int i = 0; i < ticks; i++) w.update(1 / 60.0);
    }

    private static void sightRule() {
        Services sv = services();
        World dark = room(sv, 100, Rule.SIGHT);
        sv.input().simulate(KeyEvent.VK_F, true); // lantern off
        sv.input().poll();
        tick(dark, 1);
        sv.input().simulate(KeyEvent.VK_F, false);
        sv.input().poll();
        check(!dark.player.lanternOn(), "lantern should be off");
        tick(dark, 30);
        check(!dark.creatures().get(0).isChasing(), "dark player was seen");

        World lit = room(services(), 100, Rule.SIGHT);
        tick(lit, 5);
        check(lit.creatures().get(0).state() instanceof ChaseState, "lit player was not seen");
    }

    private static void noSightRule() {
        World w = room(services(), 100, Rule.SMELL);
        tick(w, 5);
        check(!w.creatures().get(0).isChasing(), "lit player seen without the SIGHT rule");
    }

    private static void hearingRule() {
        Services sv = services();
        World far = room(sv, 150, Rule.HEARING);
        sv.input().simulate(KeyEvent.VK_S, true); // walk down (quietly)
        sv.input().poll();
        tick(far, 60);
        check(!far.creatures().get(0).isChasing(), "walking 150px away should not be heard as a chase");

        Services sv2 = services();
        World near = room(sv2, 70, Rule.HEARING);
        sv2.input().simulate(KeyEvent.VK_SHIFT, true);
        sv2.input().simulate(KeyEvent.VK_S, true);
        sv2.input().poll();
        boolean chased = false;
        for (int i = 0; i < 60 && !chased; i++) {
            tick(near, 1);
            chased = near.creatures().get(0).isChasing();
        }
        check(chased, "running 70px away was not heard");
    }

    private static void touchKills() {
        World w = room(services(), 6);
        tick(w, 2);
        check(w.death() == DeathCause.TENANT, "expected death by Tenant, got " + w.death());
    }

    private static void coldRule() {
        Services sv = services();
        World w = room(sv, 300, Rule.COLD);
        w.creatures().get(0).x = 1e6; // out of the way
        sv.input().simulate(KeyEvent.VK_F, true);
        sv.input().poll();
        tick(w, 1);
        sv.input().simulate(KeyEvent.VK_F, false);
        sv.input().poll();
        for (int i = 0; i < 60 * 60 && w.death() == null; i++) tick(w, 1);
        check(w.death() == DeathCause.COLD, "expected to freeze, got " + w.death() + " cold=" + w.player.cold);
    }

    private static void throughWalls() {
        World w = room(services(), 300);
        for (int y = 0; y < 14; y++) w.map.set(15, y, Tile.WALL);
        Creature c = w.creatures().get(0);
        c.x = TileMap.center(25);
        double wallX = 15 * TileMap.TILE;
        // speed is near zero in room(); use a normal-speed Tenant instead
        Creature fast = w.add(new Creature(TileMap.center(25), TileMap.center(7), 1.0));
        c.x = 1e6;
        boolean crossed = false;
        for (int i = 0; i < 60 * 12 && !crossed && w.death() == null; i++) {
            fast.perceive(new hollowshift.ai.Stimulus(w.player.x, w.player.y, 1, hollowshift.ai.Stimulus.Kind.SIGHT));
            tick(w, 1);
            crossed = fast.x < wallX;
        }
        check(crossed || w.death() != null, "Tenant did not pass the wall, x=" + fast.x);
    }

    private static void grab() {
        World w = room(services(), 300, Rule.SIGHT);
        w.creatures().get(0).x = 1e6;
        Creature c = w.add(new Creature(w.player.x + 45, w.player.y, 1.0));
        boolean reached = false;
        for (int i = 0; i < 60 * 4 && w.death() == null; i++) {
            tick(w, 1);
            reached |= c.state() instanceof hollowshift.ai.ReachState;
        }
        check(reached, "Tenant never switched to grabbing");
        check(w.death() == DeathCause.TENANT, "player 45px away was not grabbed, death=" + w.death());
    }

    private static void outrunSmell() {
        Services sv = services();
        TileMap map = new TileMap(170, 12);
        for (int y = 1; y < 11; y++) for (int x = 1; x < 169; x++) map.set(x, y, Tile.FLOOR);
        ShiftDef def = new ShiftDef(4, "test", "", 0, 0, 0, 1, 0, 1.05, RuleSet.of(Rule.SMELL), false, false);
        World w = new World(sv, def, map, new FuseObjectives(1), 1);
        w.addRoomPoint(TileMap.center(5), TileMap.center(6));
        w.add(new Player(TileMap.center(14), TileMap.center(6)));
        Creature c = w.add(new Creature(TileMap.center(14) - 80, TileMap.center(6), 1.05));
        w.start();
        c.perceive(new hollowshift.ai.Stimulus(w.player.x, w.player.y, 1, hollowshift.ai.Stimulus.Kind.SMELL));
        check(c.isChasing(), "setup: the Tenant should be on the player's trail");
        sv.input().simulate(KeyEvent.VK_SHIFT, true);
        sv.input().simulate(KeyEvent.VK_D, true);
        sv.input().poll();
        boolean lost = false;
        for (int i = 0; i < 60 * 20 && w.death() == null; i++) {
            tick(w, 1);
            lost |= !c.isChasing() && c.distanceTo(w.player) > 200;
        }
        check(w.death() == null, "caught while running away (distance " + (int) c.distanceTo(w.player) + ")");
        check(lost, "never shook it off, distance " + (int) c.distanceTo(w.player));
    }

    private static void hiddenEnding() {
        for (boolean allNotes : new boolean[]{false, true}) {
            Services sv = services();
            if (allNotes) for (int i = 1; i <= SaveData.STORY_NOTES; i++) sv.save().notesRead.add(i);
            Campaign c = Campaign.load();
            World w = WorldBuilder.build(sv, c.shift(c.shifts().size()), 3);
            w.start();
            hollowshift.entity.Crack crack = w.entitiesOf(hollowshift.entity.Crack.class).get(0);
            check(!crack.canInteract(w.player), "crack open before the lift arrived");
            for (int i = 0; i < 3; i++) w.objectives().onLeverPulled();
            for (int i = 0; i < 60 * 50; i++) w.objectives().update(1 / 60.0);
            check(w.objectives().exitOpen(), "lift should have arrived");
            check(crack.canInteract(w.player) == allNotes, "crack open=" + crack.canInteract(w.player) + " with allNotes=" + allNotes);
            check(w.entitiesOf(hollowshift.entity.Note.class).size() == 2, "final map should hold the last two notes");
            if (allNotes) {
                crack.interact(w.player);
                check(w.objectives().complete() && w.objectives().stayed(), "staying below should end the shift");
            }
        }
    }

    private static void press(Services sv, World w, int key) {
        sv.input().simulate(key, true);
        sv.input().poll();
        tick(w, 1);
        sv.input().simulate(key, false);
        sv.input().poll();
        tick(w, 1);
    }

    private static void pockets() {
        Services sv = services();
        World w = room(sv, 600);
        Player p = w.player;
        for (int i = 0; i < 4; i++) {
            Stone st = w.add(new Stone(p.x, p.y));
            tick(w, 1);
            if (st.canInteract(p)) st.interact(p);
        }
        check(p.pack().count(Supply.STONE) == Supply.STONE.max, "stones should stop at the pocket limit, got " + p.pack().count(Supply.STONE));
        check(w.entitiesOf(Stone.class).size() == 1, "the stone that did not fit should stay on the floor");
        check(!p.isHolding(), "stones go in a pocket, not the hands");

        press(sv, w, KeyEvent.VK_Q);
        check(p.pack().count(Supply.STONE) == Supply.STONE.max - 1, "Q should throw a stone");
        check(w.entitiesOf(Stone.class).stream().anyMatch(Stone::isFlying), "thrown stone should be in the air");

        BatteryCell cell = w.add(new BatteryCell(p.x, p.y));
        tick(w, 1);
        cell.interact(p);
        check(p.pack().selected() == Supply.BATTERY, "a new item is selected when picked up");
        press(sv, w, KeyEvent.VK_Q);
        check(p.pack().count(Supply.BATTERY) == 1, "a full lantern should not use up a cell");
        p.battery = 0.2;
        press(sv, w, KeyEvent.VK_Q);
        check(p.pack().count(Supply.BATTERY) == 0 && p.battery > 0.75, "a cell should recharge the lantern");

        press(sv, w, KeyEvent.VK_1);
        check(p.pack().selected() == Supply.STONE, "key 1 selects the stone pocket");
    }

    private static void notesKept() {
        Services sv = services();
        World w = room(sv, 600);
        Note n = w.add(new Note(w.player.x, w.player.y, 3));
        tick(w, 1);
        n.interact(w.player);
        check(w.reading() != null && w.readingNumber() == 3, "the note should be on screen");
        check(sv.save().notesRead.contains(3), "the note should be in the journal");
        tick(w, 1);
        check(w.entitiesOf(Note.class).isEmpty(), "the note should be taken off the floor");
    }

    private static void lanternDrains() {
        World plain = room(services(), 600);
        World rule = room(services(), 600, Rule.BATTERY);
        tick(plain, 60 * 40);
        tick(rule, 60 * 40);
        check(plain.player.battery < 0.9 && plain.player.battery > 0.7, "40 s of light should cost a little charge, left " + plain.player.battery);
        check(rule.player.battery < plain.player.battery - 0.2, "BATTERY should drain much faster, left " + rule.player.battery);
    }

    // ---- full shifts with a random bot --------------------------------------------------------------

    private static void simulation() {
        Campaign c = Campaign.load();
        List<ShiftDef> defs = new ArrayList<>(c.shifts());
        for (int n = 1; n <= 8; n += 3) defs.add(c.endless(n, 1234));
        int[] keys = {KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D};
        for (ShiftDef def : defs) {
            Services sv = services();
            World w = WorldBuilder.build(sv, def, 5000 + def.number());
            w.start();
            Rng rng = new Rng(def.number());
            int held = keys[0];
            Set<String> states = new HashSet<>();
            for (int i = 0; i < 60 * 90 && w.death() == null && !w.objectives().complete(); i++) {
                if (i % 50 == 0) {
                    sv.input().simulate(held, false);
                    held = keys[rng.range(0, 4)];
                    sv.input().simulate(held, true);
                    sv.input().simulate(KeyEvent.VK_SHIFT, rng.chance(0.3));
                }
                if (i % 23 == 0) sv.input().simulate(KeyEvent.VK_E, true);
                if (i % 23 == 1) sv.input().simulate(KeyEvent.VK_E, false);
                if (i % 211 == 0) sv.input().simulate(KeyEvent.VK_Q, true);
                if (i % 211 == 1) sv.input().simulate(KeyEvent.VK_Q, false);
                sv.input().poll();
                w.update(1 / 60.0);
                for (Creature cr : w.creatures()) states.add(cr.state().name());
                // while grabbed the player is dragged to the Tenant, which may stand inside rock
                if (!w.player.isFrozen()) check(!w.map.solidAt(w.player.x, w.player.y), def.title() + ": player inside a wall");
            }
            System.out.println("        " + def.title() + ": " + (w.death() != null ? "died (" + w.death() + ")" : "alive")
                    + " after " + (int) w.time + "s, Tenant states " + states);
        }
    }
}
