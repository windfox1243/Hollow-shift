package hollowshift.game;

import hollowshift.ai.Noise;
import hollowshift.ai.NoiseBus;
import hollowshift.engine.Camera;
import hollowshift.engine.Lang;
import hollowshift.engine.Input;
import hollowshift.engine.MathUtil;
import hollowshift.engine.Rng;
import hollowshift.engine.audio.Audio;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.audio.Voice;
import hollowshift.engine.gfx.Art;
import hollowshift.engine.gfx.Lighting;
import hollowshift.entity.*;
import hollowshift.rules.*;
import hollowshift.story.SaveData;
import hollowshift.story.ShiftDef;
import hollowshift.world.TileMap;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** All state of one running shift: map, entities, rules, objectives, audio mood. */
public final class World {
    /** Radius of the halo that lets the player see themselves in total darkness. */
    private static final double PERSONAL_GLOW = 22;
    private final Services services;
    public final ShiftDef shift;
    public final TileMap map;
    public final RuleSet rules;
    public final Rng rng;
    public final Audio audio;
    public final Art art;
    public final Camera camera = new Camera();
    public final NoiseBus noises = new NoiseBus();
    public Player player;
    public double time;

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> pending = new ArrayList<>();
    private final List<Entity> drawOrder = new ArrayList<>();
    private final List<LightSource> lights = new ArrayList<>();
    private final List<Creature> creatures = new ArrayList<>();
    private final List<StatusEffect> effects = new ArrayList<>();
    private final List<double[]> roomPoints = new ArrayList<>();
    private final Objectives objectives;
    private final Lighting lighting = new Lighting();
    private Elevator elevator;
    private DeathCause death;
    private String toast;
    private String reading;
    private Panel panel;
    /** The phone number's digits read off the walls so far, '_' where not yet seen. */
    private char[] codeSeen;
    private int readingNumber;
    private double toastTimer;
    private Voice ambience;
    private Voice chase;
    private double chaseVolume;
    private boolean hunted;
    private double lostTimer;
    private double heartbeatTimer;
    private static final double FEAR_RANGE = 240;
    private double fear;
    private double heartPulse;

    public World(Services services, ShiftDef shift, TileMap map, Objectives objectives, long seed) {
        this.services = services;
        this.shift = shift;
        this.map = map;
        this.rules = shift.rules();
        this.rng = new Rng(seed ^ 0x5DEECE66DL);
        this.audio = services.audio();
        this.art = services.art();
        this.objectives = objectives;
        objectives.attach(this);
        effects.add(new BatteryEffect(rules.has(Rule.BATTERY) ? BatteryEffect.DRAIN_SECONDS_RULE : BatteryEffect.DRAIN_SECONDS));
        if (rules.has(Rule.COLD)) effects.add(new ColdEffect());
        if (rules.has(Rule.HUNGER)) effects.add(new HungerEffect());
    }

    // ---- population ---------------------------------------------------------------

    public <T extends Entity> T add(T e) {
        e.attach(this);
        pending.add(e);
        if (e instanceof Player p) player = p;
        if (e instanceof Creature c) creatures.add(c);
        if (e instanceof Elevator el) elevator = el;
        return e;
    }

    public void addRoomPoint(double x, double y) {
        roomPoints.add(new double[]{x, y});
    }

    public double[] randomRoomPoint() {
        return roomPoints.get(rng.range(0, roomPoints.size()));
    }

    /** A random room centre at least {@code minDistance} from the player, or any one if none is that far. */
    public double[] randomRoomPointAway(double minDistance) {
        List<double[]> far = new ArrayList<>();
        for (double[] r : roomPoints) if (Math.hypot(r[0] - player.x, r[1] - player.y) >= minDistance) far.add(r);
        return far.isEmpty() ? randomRoomPoint() : far.get(rng.range(0, far.size()));
    }

    /** The room centre farthest from the player right now. */
    public double[] farthestRoomPoint() {
        double[] best = randomRoomPoint();
        double bestD = -1;
        for (double[] r : roomPoints) {
            double d = Math.hypot(r[0] - player.x, r[1] - player.y);
            if (d > bestD) {
                bestD = d;
                best = r;
            }
        }
        return best;
    }

    public List<Entity> entities() {
        return entities;
    }

    /** Entities added but not yet in play (while a level is being built). */
    public List<Entity> pendingEntities() {
        return pending;
    }

    public List<Creature> creatures() {
        return creatures;
    }

    public <T> List<T> entitiesOf(Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Entity e : entities) if (type.isInstance(e) && !e.isRemoved()) out.add(type.cast(e));
        return out;
    }

    public Elevator elevator() {
        return elevator;
    }

    public Objectives objectives() {
        return objectives;
    }

    public List<StatusEffect> effects() {
        return effects;
    }

    public Input input() {
        return services.input();
    }

    public SaveData settings() {
        return services.save();
    }

    // ---- events ---------------------------------------------------------------------

    public void emitNoise(Noise n) {
        noises.emit(n);
    }

    public void toast(String msg) {
        toast = msg;
        toastTimer = 3.5;
    }

    /** Opens a note on screen; the shift is paused until it is put away. */
    public void readNote(int number, String text) {
        reading = text;
        readingNumber = number;
    }

    /** Which story note (1..10) is open, for its look. */
    public int readingNumber() {
        return readingNumber;
    }

    public String reading() {
        return reading;
    }

    /** Puts the note away; it is kept in the journal from now on. */
    public void closeNote() {
        reading = null;
        toast(Lang.t("toast.noteKept", hollowshift.engine.Input.keyName(input().binding(hollowshift.engine.Action.JOURNAL)[0])));
    }

    /** Opens a machine's close-up; the shift runs on while it is open. */
    public void openPanel(Panel p) {
        panel = p;
    }

    public Panel panel() {
        return panel;
    }

    public void closePanel() {
        if (panel != null) panel.close();
        panel = null;
    }

    /** How long the phone number is; call before any digit is learned. */
    public void setCodeLength(int n) {
        codeSeen = "_".repeat(n).toCharArray();
    }

    /** Reads digit d at place i off a wall; true if it was new. */
    public boolean learnDigit(int i, char d) {
        if (codeSeen == null || codeSeen[i] == d) return false;
        codeSeen[i] = d;
        return true;
    }

    /** The number as far as it is known, e.g. "4 _ 7". */
    public String codeMemo() {
        if (codeSeen == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : codeSeen) b.append(b.isEmpty() ? "" : " ").append(c);
        return b.toString();
    }

    public String toast() {
        return toastTimer > 0 ? toast : null;
    }

    public double toastAlpha() {
        return MathUtil.clamp(toastTimer, 0, 1);
    }

    public void kill(DeathCause cause) {
        if (death == null && !objectives.escaping()) death = cause;
    }

    public DeathCause death() {
        return death;
    }

    // ---- light queries --------------------------------------------------------------------

    /** How exposed a point is (0 dark .. 1 fully lit), optionally counting the player's lantern. */
    public double visibilityAt(double x, double y, boolean includeLantern) {
        double best = 0;
        // like the original: brightest light at the point, linear falloff to its rim
        for (LightSource l : lights) {
            if (!includeLantern && l == player) continue;
            double r = l.lightRadius();
            if (r <= 0) continue;
            double d = Math.hypot(l.lightX() - x, l.lightY() - y);
            if (d >= r) continue;
            best = Math.max(best, l.lightIntensity() * (1 - d / r));
        }
        return Math.min(1, best);
    }

    /** Warmth from heat-giving lights (not the lantern), 0..1. */
    public double warmthAt(double x, double y) {
        double best = 0;
        for (LightSource l : lights) {
            if (!l.warms()) continue;
            double r = l.lightRadius();
            if (r <= 0) continue;
            double d = Math.hypot(l.lightX() - x, l.lightY() - y);
            if (d < r) best = Math.max(best, l.lightIntensity() * (1 - d / r) * 1.6);
        }
        return Math.min(1, best);
    }

    // ---- loop ---------------------------------------------------------------------------------

    public void start() {
        flushPending();
        camera.snapTo(player.x, player.y - 8);
        startAudio();
    }

    public void startAudio() {
        stopAudio();
        ambience = audio.loop("drone", Bus.MUSIC, 0.55);
        chase = null; // the chase track starts when the Tenant finds the player
    }

    public void stopAudio() {
        if (ambience != null) ambience.stop();
        if (chase != null) chase.stop();
    }

    public void update(double dt) {
        time += dt;
        flushPending();
        if (death == null) {
            player.speedFactor = 1;
            for (StatusEffect e : effects) e.update(player, this, dt);
            // the close-up shuts if something takes the miner's hands
            if (panel != null && (panel.isClosed() || player.isFrozen())) closePanel();
            if (panel != null) panel.update(services.input(), dt);
            if (panel != null && panel.isClosed()) panel = null;
            player.handleInput(services.input(), dt);
        } else {
            panel = null;
        }
        for (Entity e : entities) if (!e.isRemoved() && e != player) e.update(dt);
        if (entities.removeIf(Entity::isRemoved)) lights.removeIf(l -> ((Entity) l).isRemoved());
        objectives.update(dt);

        noises.clear();
        noises.decay(dt);
        toastTimer -= dt;

        camera.follow(player.x, player.y - 8, dt);
        camera.update(dt);
        audio.setListener(player.x, player.y);
        updateMood(dt);
    }

    private void flushPending() {
        if (pending.isEmpty()) return;
        entities.addAll(pending);
        for (Entity e : pending) if (e instanceof LightSource l) lights.add(l);
        pending.clear();
    }

    /** Chase music and heartbeat scale with the nearest Tenant. */
    private void updateMood(double dt) {
        double nearest = Double.MAX_VALUE;
        boolean chasing = false;
        for (Creature c : creatures) {
            if (c.isDormant()) continue;
            double d = c.distanceTo(player);
            if (d < nearest) nearest = d;
            chasing |= c.isChasing() && d < 500;
        }
        // the chase track kicks in from the beginning the moment it has found you,
        // loops while the hunt lasts and fades out a little after it loses you
        if (chasing && !hunted && death == null) {
            hunted = true;
            if (chase != null) chase.stop();
            chase = audio.loop("chase_found", Bus.MUSIC, 0.9);
            chaseVolume = 1;
        }
        if (chasing) lostTimer = 0;
        else if (hunted && (lostTimer += dt) > 1.5) hunted = false;
        chaseVolume = MathUtil.damp(chaseVolume, hunted ? 1 : 0, hunted ? 0.05 : 0.25, dt);
        if (chase != null) {
            chase.setGain(chaseVolume * 0.9);
            if (!hunted && chaseVolume < 0.02) {
                chase.stop();
                chase = null;
            }
        }
        if (ambience != null) ambience.setGain(0.55 * (1 - chaseVolume * 0.8));

        // fear rises as it closes in (and faster while it is hunting)
        double raw = MathUtil.clamp((FEAR_RANGE - nearest) / (FEAR_RANGE - 40), 0, 1) * (chasing ? 1 : 0.7);
        fear = MathUtil.damp(fear, raw, raw > fear ? 0.02 : 0.3, dt);
        heartPulse = Math.max(0, heartPulse - dt * 3.5);

        heartbeatTimer -= dt;
        if (fear > 0.05 && heartbeatTimer <= 0 && death == null) {
            audio.play("heartbeat", Bus.SFX, 0.35 + 0.6 * fear, 1 + 0.15 * fear);
            heartbeatTimer = 1.1 - 0.65 * fear;
            heartPulse = 1;
            // each beat jolts the view a little, harder the closer it is
            if (settings().screenShake && fear > 0.25) camera.shakeFrom(camera.x, camera.y, 0.3 * fear, 0.12);
        }
    }

    /** 0..1: how close the Tenant is, for the heartbeat screen effects. */
    public double fear() {
        return fear;
    }

    /** 1 right on a heartbeat, decaying to 0 before the next. */
    public double heartPulse() {
        return heartPulse;
    }

    public void render(Graphics2D g) {
        int ox = camera.left(), oy = camera.top();
        map.render(g, camera, art);
        drawOrder.clear();
        for (Entity e : entities) if (camera.visible(e.x, e.y, 64)) drawOrder.add(e);
        drawOrder.sort(Comparator.comparingDouble(Entity::sortY));
        for (Entity e : drawOrder) e.render(g, ox, oy);

        // as in the original: outside the lights it is completely black
        lighting.begin(1.0);
        // the player's own faint halo (never counts toward being seen)
        lighting.light(player.x - ox, player.y - 8 - oy, PERSONAL_GLOW, 0.5, null);
        for (LightSource l : lights) {
            double r = l.lightRadius();
            if (r > 0 && camera.visible(l.lightX(), l.lightY(), r)) {
                lighting.light(l.lightX() - ox, l.lightY() - oy, r, l.lightIntensity(), l.lightTint());
            }
        }
        lighting.end(g);
        for (Creature c : creatures) c.renderEyes(g, ox, oy);
        // signal lamps on unfinished machines show through the dark too
        for (Entity e : drawOrder) if (e instanceof Beacon b) b.renderSignal(g, ox, oy);
    }
}
