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
import hollowshift.game.Job;
import hollowshift.rules.*;
import hollowshift.story.Campaign;
import hollowshift.story.SaveData;
import hollowshift.story.ShiftDef;
import hollowshift.world.*;

import java.awt.event.KeyEvent;
import java.util.*;
import java.util.EnumSet;

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
        test("no room is a dead end: every door lies on a loop", RunTests::noDeadEnds);
        test("ECHO: the Tenant arrives through a doorway, not mid-room", RunTests::echoDoorway);
        test("ECHO: after a loud job it takes 3 s and comes out 150 px away", RunTests::echoHeadStart);
        test("CONSUME: a wall nest pins you; hammering E frees you, waiting kills", RunTests::wallMaw);
        test("CONSUME: stepping on its flesh calls the Tenant", RunTests::fleshNode);
        test("every lift job (fuse, valve, key, fuel, phone) can be done", RunTests::liftJobs);
        test("each run draws its own mix of jobs, placed in the map", RunTests::jobVariety);
        test("CONSUME: a wall nest wakes the Tenant and brings it", RunTests::nestSummons);
        test("CONSUME: flesh knots lie in wait beside useful things", RunTests::knotsAsBait);
        test("props: heavy ones block their tile but never a path, clutter lies flat", RunTests::props);
        test("every room has its own sign; scrawls match the room and never repeat", RunTests::writing);
        test("COLD: the dark freezes faster than lantern light", RunTests::coldDark);
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
        check(lib.rooms().size() >= 20, "expected at least 20 room templates, got " + lib.rooms().size());
        check(!lib.starts().isEmpty(), "missing start template");
        List<ChunkTemplate> all = new ArrayList<>(lib.rooms());
        all.addAll(lib.starts());
        for (ChunkTemplate t : all) {
            for (int flip = 0; flip < 8; flip++) {
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

    /** Stamps a template with all four doorways open; flip bit 0 mirrors X, bit 1 mirrors Y, bit 2 turns it. */
    private static void stamp(TileMap map, List<Marker> markers, ChunkTemplate t, int flip) {
        int all = WorldGenerator.N | WorldGenerator.E | WorldGenerator.S | WorldGenerator.W;
        WorldGenerator.stamp(map, markers, t, 0, 0, all, (flip & 1) != 0, (flip & 2) != 0, (flip & 4) != 0, 0, 0);
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
            // the last ear may be one room longer than needed
            check(count == rooms || count == rooms + 1, "seed " + seed + ": expected " + rooms + " rooms, got " + count);
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
                if (shift < 4) check(!rs.has(Rule.HASTE) && !rs.has(Rule.COLD), "HASTE/COLD too early");
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
        ShiftDef last = c.shifts().get(c.shifts().size() - 1);
        for (long seed = 1; seed <= 20; seed++) {
            World w = WorldBuilder.build(services(), last, seed);
            int levers = w.pendingEntities().stream().filter(e -> e instanceof Lever).toList().size();
            check(levers == Math.max(3, last.fuses()), "seed " + seed + ": final map has " + levers + " breakers");
            check(w.pendingEntities().stream().anyMatch(e -> e instanceof Crack), "seed " + seed + ": final map has no crack");
        }
        // rules stack: every shift keeps all the rules of the one before
        for (int n = 2; n <= c.shifts().size(); n++) {
            List<Rule> before = c.shift(n - 1).rules().ordered(), now = c.shift(n).rules().ordered();
            check(now.containsAll(before) && now.size() > before.size(), "shift " + n + " rules " + now + " should extend " + before);
        }
        // later shifts ask for more before the lift runs
        for (int n = 2; n < c.shifts().size(); n++) {
            ShiftDef prev = c.shift(n - 1), cur = c.shift(n);
            int jobsA = prev.fuses() + prev.jobs(), jobsB = cur.fuses() + cur.jobs();
            check(jobsB > jobsA, "shift " + n + " should have more jobs than shift " + (n - 1));
        }
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
        World w = new World(sv, def, map, new ShiftObjectives(1), 1);
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
        World w = new World(sv, def, map, new ShiftObjectives(1), 1);
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
            for (int i = 0; i < w.entitiesOf(hollowshift.entity.Lever.class).size(); i++) w.objectives().onLeverPulled();
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

    private static void noDeadEnds() {
        WorldGenerator gen = new WorldGenerator(ChunkLibrary.load());
        Set<WorldGenerator.Layout> seen = EnumSet.noneOf(WorldGenerator.Layout.class);
        int[] dx = {0, 1, 0, -1}, dy = {-1, 0, 1, 0};
        for (int seed = 0; seed < 150; seed++) {
            int w = 3 + seed % 4, h = 3 + seed % 3;
            WorldGenerator.Level l = gen.generate(seed, w, h, Math.max(5, w * h * 2 / 3), 0.15);
            seen.add(l.layout());
            for (int cell = 0; cell < l.doors().length; cell++) {
                if (l.depth()[cell] < 0) continue;
                check(Integer.bitCount(l.doors()[cell]) >= 2, "seed " + seed + ": room " + cell + " is a dead end");
                for (int d = 1; d <= 2; d++) {
                    if ((l.doors()[cell] & (1 << d)) == 0) continue;
                    int other = (cell / w + dy[d]) * w + cell % w + dx[d];
                    check(reachesWithout(l, cell, other), "seed " + seed + ": door " + cell + "-" + other + " is the only way through");
                }
            }
        }
        check(seen.size() == WorldGenerator.Layout.values().length, "not every layout appeared: " + seen);
    }

    /** Whether b can still be reached from a with the door between them shut. */
    private static boolean reachesWithout(WorldGenerator.Level l, int a, int b) {
        int w = l.cellsW();
        int[] dx = {0, 1, 0, -1}, dy = {-1, 0, 1, 0};
        boolean[] seen = new boolean[l.doors().length];
        ArrayDeque<Integer> q = new ArrayDeque<>(List.of(a));
        seen[a] = true;
        while (!q.isEmpty()) {
            int c = q.poll();
            if (c == b) return true;
            for (int d = 0; d < 4; d++) {
                if ((l.doors()[c] & (1 << d)) == 0) continue;
                int o = (c / w + dy[d]) * w + c % w + dx[d];
                if (c == a && o == b || c == b && o == a || seen[o]) continue;
                seen[o] = true;
                q.add(o);
            }
        }
        return false;
    }

    private static void echoDoorway() {
        Services sv = services();
        Campaign camp = Campaign.load();
        World w = WorldBuilder.build(sv, camp.shift(4), 11);
        w.start();
        tick(w, 1);
        Creature c = w.creatures().get(0);
        c.setDormant(0);
        // a noise in a room far from both of them
        double[] far = null;
        for (int i = 0; i < 200 && far == null; i++) {
            double[] p = w.randomRoomPoint();
            if (w.player.distanceTo(p[0], p[1]) > 300 && c.distanceTo(p[0], p[1]) > 300) far = p;
        }
        check(far != null, "setup: no far room");
        List<double[]> doors = WorldGenerator.doorways(w.map, far[0], far[1]);
        check(doors.size() >= 2, "a room should have at least two doorways, got " + doors.size());
        c.echoTo(far[0], far[1]);
        // look at it the moment it has come out, before it starts searching
        for (int i = 0; i < 60 * 5 && c.state() instanceof hollowshift.ai.EchoState; i++) tick(w, 1);
        double best = Double.MAX_VALUE;
        for (double[] d : doors) best = Math.min(best, c.distanceTo(d[0], d[1]));
        check(best < 2, "the Tenant should be standing in a doorway, nearest is " + (int) best + " px away");
    }

    /** A breaker or fuse box makes the noise right where the player stands, on every story map size. */
    private static void echoHeadStart() {
        Campaign camp = Campaign.load();
        for (long seed = 1; seed <= 12; seed++) {
            World w = WorldBuilder.build(services(), camp.shift(4 + (int) (seed % 2)), seed);
            w.start();
            tick(w, 1);
            Creature c = w.creatures().get(0);
            c.setDormant(0);
            Player p = w.player;
            double[] job = w.randomRoomPoint();
            p.x = job[0];
            p.y = job[1];
            c.echoTo(p.x, p.y);
            int i = 0;
            for (; i < 60 * 5 && c.state() instanceof hollowshift.ai.EchoState; i++) {
                tick(w, 1);
                // visible only while fading out (first 0.6 s) or once it comes back after the travel
                check(!c.isVisibleInWorld() || i < 40 || i >= 60 * 2.8, "seed " + seed + ": it is back after only " + i / 60.0 + " s");
            }
            check(i >= 60 * 3, "seed " + seed + ": it was back in " + i / 60.0 + " s");
            check(c.isVisibleInWorld(), "seed " + seed + ": it should be back by now");
            check(c.distanceTo(p) >= hollowshift.ai.EchoState.MIN_DISTANCE - 1 || w.death() != null,
                    "seed " + seed + ": it came out " + (int) c.distanceTo(p) + " px from the player");
            check(w.death() == null, "seed " + seed + ": standing still until it comes back should not be fatal");
        }
    }

    private static void wallMaw() {
        for (boolean fight : new boolean[]{true, false}) {
            Services sv = services();
            World w = room(sv, 600, Rule.CONSUME);
            // a nest in the north wall right above the player
            WallMaw maw = w.add(new WallMaw(w.player.x, TileMap.center(1), 0, -1));
            w.player.y = TileMap.center(1) + 6; // walking along the wall, past the nest
            tick(w, 2);
            check(w.player.trappedBy() == maw && w.player.isFrozen(), "standing by the nest should pin the player");
            for (int i = 0; i < 60 * 6 && w.death() == null && w.player.trappedBy() != null; i++) {
                if (fight && i % 12 == 0) press(sv, w, KeyEvent.VK_E);
                else tick(w, 1);
            }
            if (fight) check(w.death() == null && w.player.trappedBy() == null && !w.player.isFrozen(), "hammering E should tear free");
            else check(w.death() == DeathCause.CONSUMED, "waiting should be fatal, got " + w.death());
        }
    }

    private static void fleshNode() {
        World w = room(services(), 300, Rule.CONSUME);
        Creature c = w.creatures().get(0);
        tick(w, 5);
        check(!c.isChasing(), "setup: it should not know where the player is");
        w.add(new FleshNode(w.player.x, w.player.y + 2));
        tick(w, 3);
        check(c.isChasing(), "stepping on its flesh should send it after the player");
    }

    private static void liftJobs() {
        Services sv = services();
        TileMap map = new TileMap(30, 14);
        for (int y = 1; y < 13; y++) for (int x = 1; x < 29; x++) map.set(x, y, Tile.FLOOR);
        ShiftDef def = new ShiftDef(2, "test", "", 0, 0, 0, 1, 0, 1, RuleSet.of(), false, false, 4);
        Map<Job, Integer> jobs = new EnumMap<>(Job.class);
        for (Job j : Job.values()) jobs.put(j, 1);
        ShiftObjectives obj = new ShiftObjectives(jobs);
        World w = new World(sv, def, map, obj, 1);
        Player p = w.add(new Player(TileMap.center(4), TileMap.center(7)));
        w.start();
        check(obj.remaining() == 5, "five jobs expected, got " + obj.remaining());
        obj.onJobDone(Job.FUSE);
        Valve v = w.add(new Valve(p.x, p.y));
        LiftKey k = w.add(new LiftKey(p.x + 40, p.y));
        Generator gen = w.add(new Generator(p.x, p.y));
        FuelCan can = w.add(new FuelCan(p.x, p.y));
        Phone phone = w.add(new Phone(p.x, p.y));
        tick(w, 1);
        v.interact(p);
        k.interact(p);
        check(!gen.canInteract(p), "the generator needs fuel in hand");
        can.interact(p);
        check(gen.canInteract(p), "with a can in hand the generator can be fuelled");
        gen.interact(p);
        check(gen.isRunning() && !p.isHolding(), "the can should be poured in");
        check(!obj.exitOpen(), "the phone call is still missing");
        phone.interact(p);
        check(obj.exitOpen(), "all jobs done, the lift should run");
    }

    private static void jobVariety() {
        Campaign c = Campaign.load();
        for (int n = 1; n < c.shifts().size(); n++) {
            ShiftDef def = c.shift(n);
            Set<Set<Job>> mixes = new HashSet<>();
            for (long seed = 1; seed <= 40; seed++) {
                Map<Job, Integer> jobs = WorldBuilder.drawJobs(def, seed);
                check(jobs.get(Job.FUSE) >= def.fuses(), "shift " + n + ": too few fuses");
                check(jobs.size() == 1 + def.jobs(), "shift " + n + ": expected " + def.jobs() + " extra jobs, got " + jobs.keySet());
                mixes.add(jobs.keySet());
                World w = WorldBuilder.build(services(), def, seed);
                int gens = (int) w.pendingEntities().stream().filter(e -> e instanceof Generator).count();
                int cans = (int) w.pendingEntities().stream().filter(e -> e instanceof FuelCan).count();
                check(gens == jobs.getOrDefault(Job.FUEL, 0), "shift " + n + " seed " + seed + ": generators " + gens + " for " + jobs);
                check(cans >= gens, "shift " + n + " seed " + seed + ": not enough fuel cans");
                check(w.pendingEntities().stream().filter(e -> e instanceof Phone).count() == jobs.getOrDefault(Job.PHONE, 0), "phones");
                check(w.pendingEntities().stream().filter(e -> e instanceof LiftKey).count() == jobs.getOrDefault(Job.KEY, 0), "keys");
            }
            if (def.jobs() > 0 && def.jobs() < Job.EXTRAS.length) check(mixes.size() > 1, "shift " + n + " always asks for the same jobs");
        }
    }

    private static void nestSummons() {
        Services sv = services();
        TileMap map = new TileMap(60, 14);
        for (int y = 1; y < 13; y++) for (int x = 1; x < 59; x++) map.set(x, y, Tile.FLOOR);
        ShiftDef def = new ShiftDef(3, "test", "", 0, 0, 0, 1, 0, 1, RuleSet.of(Rule.CONSUME), false, false);
        World w = new World(sv, def, map, new ShiftObjectives(1), 1);
        w.addRoomPoint(TileMap.center(50), TileMap.center(7));
        Player p = w.add(new Player(TileMap.center(4), TileMap.center(1) + 6));
        Creature c = w.add(new Creature(TileMap.center(55), TileMap.center(7), 1));
        c.setDormant(30);
        w.add(new WallMaw(p.x, TileMap.center(1), 0, -1));
        w.start();
        tick(w, 2);
        check(p.trappedBy() != null, "setup: the nest should catch the player");
        check(!c.isDormant(), "a nest should wake the Tenant");
        check(c.state() instanceof hollowshift.ai.EchoState, "far away, it should come through the rock, state " + c.state().name());
    }

    private static void props() {
        Campaign camp = Campaign.load();
        for (int n = 1; n <= camp.shifts().size(); n++) {
            for (long seed = 1; seed <= 6; seed++) {
                World w = WorldBuilder.build(services(), camp.shift(n), seed * 37 + n);
                Player p = (Player) w.pendingEntities().stream().filter(e -> e instanceof Player).findFirst().orElseThrow();
                boolean[] seen = flood(w.map, TileMap.toTile(p.x), TileMap.toTile(p.y));
                for (int y = 0; y < w.map.height; y++)
                    for (int x = 0; x < w.map.width; x++)
                        if (!w.map.solid(x, y)) check(seen[y * w.map.width + x], "shift " + n + " seed " + seed + ": a prop cut off tile " + x + "," + y);
                for (hollowshift.entity.Entity e : w.pendingEntities()) {
                    if (!(e instanceof Decor d)) continue;
                    if (d.isFlat()) check(d.sortY() < d.y - 16, "clutter should be drawn under anyone standing on it");
                    else check(w.map.get(TileMap.toTile(d.x), TileMap.toTile(d.y)) == Tile.PROP, "a heavy prop should block its tile");
                }
                // nothing usable ends up stuck inside a prop
                for (hollowshift.entity.Entity e : w.pendingEntities()) {
                    if (e instanceof hollowshift.entity.Interactable && !(e instanceof Elevator)) {
                        check(!w.map.solid(TileMap.toTile(e.x), TileMap.toTile(e.y)), e.getClass().getSimpleName() + " is inside a prop");
                    }
                }
            }
        }
    }

    private static void writing() {
        Campaign camp = Campaign.load();
        for (int n = 1; n <= camp.shifts().size(); n++) {
            for (long seed = 1; seed <= 6; seed++) {
                World w = WorldBuilder.build(services(), camp.shift(n), seed * 53 + n);
                List<String> signs = w.pendingEntities().stream().filter(e -> e instanceof RoomSign).map(e -> ((RoomSign) e).text()).toList();
                List<String> scrawls = w.pendingEntities().stream().filter(e -> e instanceof Scrawl).map(e -> ((Scrawl) e).text()).toList();
                int rooms = (int) w.pendingEntities().stream().filter(e -> e instanceof RoomSign).count();
                check(rooms >= 4, "shift " + n + " seed " + seed + ": only " + rooms + " signs");
                check(new HashSet<>(signs).size() == signs.size(), "shift " + n + " seed " + seed + ": two rooms share a sign " + signs);
                check(scrawls.size() >= 2, "shift " + n + " seed " + seed + ": too few scrawls");
                check(new HashSet<>(scrawls).size() == scrawls.size(), "a scrawl repeats: " + scrawls);
                for (hollowshift.entity.Entity e : w.pendingEntities()) {
                    if (e instanceof RoomSign || e instanceof Scrawl) {
                        check(w.map.solid(TileMap.toTile(e.x), TileMap.toTile(e.y) - 1), "writing should be on a wall");
                    }
                }
            }
        }
    }

    private static void knotsAsBait() {
        Campaign camp = Campaign.load();
        ShiftDef def = camp.shift(camp.shifts().size()); // CONSUME is in the final shift
        int nearBait = 0, total = 0;
        for (long seed = 1; seed <= 10; seed++) {
            World w = WorldBuilder.build(services(), def, seed);
            List<hollowshift.entity.Entity> all = w.pendingEntities();
            for (hollowshift.entity.Entity e : all) {
                if (!(e instanceof FleshNode)) continue;
                total++;
                for (hollowshift.entity.Entity o : all) {
                    if ((o instanceof hollowshift.entity.Interactable || o instanceof Heater) && o.distanceTo(e) < 18) {
                        nearBait++;
                        break;
                    }
                }
            }
        }
        check(total >= 10 * 10, "expected plenty of flesh knots, got " + total + " in 10 maps");
        check(nearBait >= total / 4, "only " + nearBait + " of " + total + " knots sit beside something useful");
    }

    private static void coldDark() {
        World lit = room(services(), 600, Rule.COLD);
        World dark = room(services(), 600, Rule.COLD);
        dark.player.forceLanternOff();
        tick(lit, 60 * 20);
        tick(dark, 60 * 20);
        check(dark.player.cold > lit.player.cold * 1.5, "dark " + dark.player.cold + " vs lit " + lit.player.cold);
        check(dark.death() == null, "20 s in the dark should not kill yet");
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
