package hollowshift.entity;

import hollowshift.ai.*;
import hollowshift.game.World;
import hollowshift.rules.DeathCause;
import hollowshift.rules.Rule;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * The Tenant: a pulsing mass of flesh dragged along by tentacles. It perceives
 * the player only through the senses enabled by the shift's rules and acts
 * through a {@link CreatureState} state machine.
 * <p>
 * Locomotion keeps the original monster's stride mechanic: each stride eases the
 * body a fixed distance straight toward its goal, ignoring walls, so it oozes
 * through the rock between rooms. Two lead tentacles take turns whipping ahead
 * to grip the floor before each stride (the pull); the others keep grips around
 * the body and let go when they are stretched too far. Chasing strides are short
 * and quick and speed up the longer the chase lasts. Up close it stops and
 * lashes a tentacle at the player.
 */
public final class Creature extends Entity implements LightSource {
    /** Distance at which it stops striding and lashes at the player. */
    public static final double REACH_RANGE = 80;
    /** Farther than this from a summoning nest, it jumps to the room's doorway instead of walking. */
    private static final double SUMMON_JUMP = 320;
    private static final double ARRIVE = 8;
    private static final double KILL_RANGE = 16;
    private static final double GRAB_RADIUS = 10;
    private static final double PULL_TIME = 0.25;
    private static final double EYES_RANGE = 190;
    /** Pace while squeezing through solid rock, relative to moving in the open. */
    private static final double ROCK_PACE = 0.55;
    private static final int TENTACLES = 9;
    private static final double BODY_RADIUS = 24;

    private final List<Sense> senses = new ArrayList<>();
    private final double baseSpeedMultiplier;
    /** tentacles[0] and [1] are the lead pair that pull the body; the rest are support grips. */
    private final Tentacle[] tentacles = new Tentacle[TENTACLES];
    private CreatureState state;
    private double heading; // direction of travel (radians); tentacles are arranged around it

    // stride
    private boolean hasGoal;
    private double goalX, goalY;
    private Gait gait = Gait.ROAM;
    private double stepFromX, stepFromY, stepToX, stepToY;
    private double stepT = 1, stepDuration = 1;
    private double stepCooldown;
    private int nextLead;
    private int chaseSteps;
    private double chaseBonus;
    private double pulse;

    // grab
    private boolean grabbing;
    private double pullT;
    private double pullFromX, pullFromY;

    /** Seconds before it arrives; until then it is not in the level at all. */
    private double dormant;
    public double lastKnownX, lastKnownY;
    /** Visual opacity, used by the ECHO fade. */
    public double alpha = 1;

    public Creature(double x, double y, double speedMultiplier) {
        super(x, y);
        hw = 12;
        hh = 7;
        baseSpeedMultiplier = speedMultiplier;
        for (int i = 0; i < TENTACLES; i++) {
            double a = i < 2 ? (i == 0 ? -0.5 : 0.5) : Math.PI * 2 * (i - 2) / (TENTACLES - 2) + 0.3;
            double seg = i < 2 ? 11.0 : 7.5 + (i % 3) * 1.5;
            double thick = i < 2 ? 9 : 6.5;
            tentacles[i] = new Tentacle(seg, thick, a, i * 1.37, x + Math.cos(a) * 50, y + Math.sin(a) * 34);
        }
    }

    @Override
    public void attach(World world) {
        super.attach(world);
        if (world.rules.has(Rule.SIGHT)) senses.add(new SightSense());
        if (world.rules.has(Rule.HEARING)) senses.add(new HearingSense());
        if (world.rules.has(Rule.SMELL)) senses.add(new SmellSense());
        if (world.rules.has(Rule.ECHO)) senses.add(new EchoSense());
        setState(new WanderState());
    }

    public World world() {
        return world;
    }

    /** Keeps it out of the level for a while, so the shift starts in uneasy quiet. */
    public void setDormant(double seconds) {
        dormant = seconds;
    }

    public boolean isDormant() {
        return dormant > 0;
    }

    // ---- state machine -----------------------------------------------------------

    public void setState(CreatureState s) {
        if (!(s instanceof ChaseState) && !(s instanceof ReachState)) {
            chaseSteps = 0;
            chaseBonus = 0;
        }
        state = s;
        s.enter(this);
    }

    public CreatureState state() {
        return state;
    }

    public boolean isChasing() {
        return state.isChase();
    }

    public boolean isBusy() {
        return state.isBusy();
    }

    public boolean isVisibleInWorld() {
        return alpha > 0.5;
    }

    /** Entry point for all senses. */
    public void perceive(Stimulus s) {
        if (state.isBusy()) return;
        if (s.chase()) {
            lastKnownX = s.x();
            lastKnownY = s.y();
            if (state instanceof ChaseState cs) cs.refresh(s.kind());
            else if (state instanceof ReachState rs) rs.refresh();
            else {
                ChaseState chase = new ChaseState();
                setState(chase);
                chase.refresh(s.kind());
            }
            return;
        }
        if (state.isChase()) return;
        if (state instanceof InvestigateState inv) {
            if (Math.hypot(inv.targetX() - s.x(), inv.targetY() - s.y()) > 40) inv.retarget(s.x(), s.y());
            return;
        }
        setState(new InvestigateState(s.x(), s.y()));
    }

    /**
     * A wall nest has caught the player (CONSUME): it wakes if asleep, and if it is far off it
     * slips through the rock and comes in at a doorway of that room, as with ECHO.
     */
    public void summon(double px, double py) {
        dormant = 0;
        if (!isChasing() && !isBusy() && distanceTo(px, py) > SUMMON_JUMP) echoTo(px, py);
        else alarm(px, py);
    }

    /** Its growth was touched (CONSUME): it knows exactly where the player is. */
    public void alarm(double px, double py) {
        if (isDormant()) return;
        perceive(new Stimulus(px, py, 1, Stimulus.Kind.TRAP));
    }

    public void stun() {
        halt();
        releaseGrab();
        setState(new StunnedState());
        world.camera.shakeFrom(x, y, 30, 0.3);
    }

    public void echoTo(double nx, double ny) {
        halt();
        setState(new EchoState(nx, ny));
    }

    /** Difficulty, HASTE and final-shift pressure combined. */
    public double speedMultiplier() {
        double m = baseSpeedMultiplier;
        if (world.rules.has(Rule.HASTE)) m *= 1 + Math.min(0.5, world.time / 60 * 0.07);
        return m * world.objectives().huntMultiplier();
    }

    // ---- locomotion ------------------------------------------------------------------

    /** Sets the stride goal. Returns true once the body is there. */
    public boolean walkTo(double tx, double ty, Gait g) {
        hasGoal = true;
        goalX = tx;
        goalY = ty;
        gait = g;
        return distanceTo(tx, ty) < ARRIVE && stepT >= 1;
    }

    /** Stops after the current stride. */
    public void halt() {
        hasGoal = false;
    }

    /** Teleport used by ECHO: cancels the stride and regrips around the new spot. */
    public void placeAt(double nx, double ny) {
        x = nx;
        y = ny;
        stepT = 1;
        hasGoal = false;
        for (Tentacle t : tentacles) {
            double a = heading + t.baseAngle;
            t.snapTo(nx + Math.cos(a) * t.reach() * 0.6, ny + Math.sin(a) * t.reach() * 0.45);
        }
    }

    private double baseX(Tentacle t) {
        return x + Math.cos(heading + t.baseAngle) * BODY_RADIUS * 0.7;
    }

    private double baseY(Tentacle t) {
        return bodyY() + Math.sin(heading + t.baseAngle) * BODY_RADIUS * 0.5;
    }

    /** Centre of the fleshy mass (it sits a little above its ground point). */
    private double bodyY() {
        return y - 14 - lift();
    }

    private void stride(double dt) {
        if (stepT < 1) {
            stepT = Math.min(1, stepT + dt / stepDuration);
            // a pull: slow start while the tentacles tense, then a surge
            double k = stepT * stepT * (3 - 2 * stepT);
            x = stepFromX + (stepToX - stepFromX) * k;
            y = stepFromY + (stepToY - stepFromY) * k;
        }
        stepCooldown -= dt;
        if (!hasGoal || stepT < 1 || stepCooldown > 0) return;
        double d = distanceTo(goalX, goalY);
        if (d < ARRIVE) return;

        boolean chase = gait == Gait.CHASE;
        double mult = speedMultiplier() + chaseBonus;
        double length = chase ? 30 : world.rng.range(15, 30) + Math.min(1.5 * world.shift.number(), 11);
        length = Math.min(length, d);
        double angle = Math.atan2(goalY - y, goalX - x);
        heading = angle;
        stepFromX = x;
        stepFromY = y;
        stepToX = x + Math.cos(angle) * length;
        stepToY = y + Math.sin(angle) * length;
        stepDuration = (chase ? 0.3 : world.rng.range(0.3, 0.7)) / mult;
        stepCooldown = (chase ? 0.4 : world.rng.range(0.5, 1.6)) / mult;
        // it can squeeze through rock, but slowly: open tunnels are where you outrun it
        if (throughRock(stepFromX, stepFromY, stepToX, stepToY)) {
            stepDuration /= ROCK_PACE;
            stepCooldown /= ROCK_PACE;
        }
        stepT = 0;
        if (chase && ++chaseSteps > 25) chaseBonus = Math.min(0.3, chaseBonus + 0.01);

        // a lead tentacle whips ahead and grips the floor in front of where the body is going
        Tentacle lead = tentacles[nextLead];
        int side = nextLead == 0 ? -1 : 1;
        nextLead ^= 1;
        double ahead = Math.min(lead.reach() * 0.8, length + 40) + world.rng.range(-6, 6);
        double sideX = -Math.sin(angle) * 16 * side, sideY = Math.cos(angle) * 16 * side;
        lead.reachTo(stepToX + Math.cos(angle) * ahead + sideX, stepToY + Math.sin(angle) * ahead * 0.75 + sideY,
                stepDuration * 0.55, true);

        // support tentacles that are left too far behind let go and regrip nearby
        for (int i = 2; i < TENTACLES; i++) {
            Tentacle t = tentacles[i];
            if (t.moving()) continue;
            if (t.stretched() || !t.planted || world.rng.chance(0.15)) {
                double a = angle + t.baseAngle * 0.8 + world.rng.range(-0.4, 0.4);
                double r = t.reach() * world.rng.range(0.45, 0.85);
                t.reachTo(stepToX + Math.cos(a) * r, stepToY + Math.sin(a) * r * 0.7,
                        stepDuration * world.rng.range(0.6, 1.2), true);
            }
        }
    }

    private boolean throughRock(double x0, double y0, double x1, double y1) {
        for (int i = 0; i <= 4; i++) {
            double t = i / 4.0;
            if (world.map.solidAt(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t)) return true;
        }
        return false;
    }

    // ---- grabbing ---------------------------------------------------------------------

    /** Lashes the next lead tentacle at a point (ReachState). */
    public void lunge(double tx, double ty) {
        heading = Math.atan2(ty - y, tx - x);
        Tentacle t = tentacles[nextLead];
        nextLead ^= 1;
        if (!t.moving()) t.reachTo(tx, ty, 0.18, false);
    }

    public boolean handsBusy() {
        return tentacles[0].moving() || tentacles[1].moving();
    }

    private void checkGrab(Player p) {
        if (grabbing || !(state instanceof ReachState) || alpha < 0.8) return;
        for (int i = 0; i < 2; i++) {
            Tentacle t = tentacles[i];
            if (Math.hypot(t.endX() - p.x, t.endY() - (p.y - 6)) < GRAB_RADIUS) {
                grabbing = true;
                pullT = 0;
                pullFromX = p.x;
                pullFromY = p.y;
                p.setFrozen(true);
                world.audio.playAt("grab", x, y, 400, 1, 0.6);
                return;
            }
        }
    }

    private void releaseGrab() {
        if (grabbing) world.player.setFrozen(false);
        grabbing = false;
    }

    private void pull(Player p, double dt) {
        pullT += dt / PULL_TIME;
        double k = Math.min(1, pullT);
        p.x = pullFromX + (x - pullFromX) * k;
        p.y = pullFromY + (y - 2 - pullFromY) * k;
        // every tentacle wraps around the victim
        for (Tentacle t : tentacles) {
            t.reachTo(p.x + world.rng.range(-3, 3), p.y - 6 + world.rng.range(-3, 3), 0.05, false);
        }
        if (k >= 1) world.kill(DeathCause.TENANT);
    }

    // ---- update / draw ----------------------------------------------------------------

    @Override
    public void update(double dt) {
        if (dormant > 0) {
            dormant -= dt;
            if (dormant <= 0) {
                // the player has moved since the shift began: it wakes in the room farthest from them now
                double[] far = world.farthestRoomPoint();
                placeAt(far[0], far[1]);
                // it announces itself only with a distant sound from somewhere in the dark
                world.audio.playAt("creature_step_heavy", world.player.x + 300, world.player.y, 900, 0.6, 0.6);
            }
            return;
        }
        pulse += dt * (isChasing() ? 9 : 4);
        Player p = world.player;
        if (grabbing) {
            pull(p, dt);
            updateTentacles(dt);
            return;
        }
        for (Sense s : senses) s.update(this, world, dt);

        double d = distanceTo(p);
        // touch: anything this close is noticed regardless of rules
        if (d < 34 && alpha > 0.8 && !state.isBusy()) perceive(new Stimulus(p.x, p.y, 1, Stimulus.Kind.TOUCH));
        state.update(this, dt);
        if (!state.isBusy()) stride(dt);
        updateTentacles(dt);
        checkGrab(p);

        if (d < KILL_RANGE && alpha > 0.8 && !state.isBusy()) world.kill(DeathCause.TENANT);
    }

    private void updateTentacles(double dt) {
        for (int i = 0; i < TENTACLES; i++) {
            Tentacle t = tentacles[i];
            double bx = baseX(t), by = baseY(t);
            boolean landed = t.update(dt, world.time, bx, by, heading);
            // a support grip that the body has dragged out of reach lets go at once and slaps down nearby
            if (i >= 2 && !t.moving() && t.planted && t.stretched() && !grabbing) {
                double a = heading + t.baseAngle * 0.8 + world.rng.range(-0.3, 0.3);
                double r = t.reach() * world.rng.range(0.4, 0.7);
                t.reachTo(x + Math.cos(a) * r, y + Math.sin(a) * r * 0.7, world.rng.range(0.12, 0.22), true);
            }
            t.solve(bx, by, world.time);
            // only the lead pair thuds the ground; support grips are quiet
            if (landed && i < 2 && !grabbing && !(state instanceof ReachState) && alpha > 0.5) {
                world.audio.playAt(isChasing() ? "creature_step_heavy" : "creature_step", t.endX(), t.endY(), 560, 0.9,
                        world.rng.range(0.9, 1.1));
                if (world.settings().screenShake) world.camera.shakeFrom(t.endX(), t.endY(), isChasing() ? 30 : 15, 0.3);
            }
        }
    }

    /** The mass heaves up during a pull. */
    private double lift() {
        return stepT < 1 ? Math.sin(Math.PI * stepT) * 4 : 0;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        if (dormant > 0 || alpha <= 0.02) return;
        Composite old = g.getComposite();
        if (alpha < 1) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        int sx = (int) Math.round(x) - ox, sy = (int) Math.round(y) - oy;
        g.setColor(new Color(0, 0, 0, 140));
        g.fillOval(sx - 30, sy - 6, 60, 13);
        for (Tentacle t : tentacles) t.render(g, ox, oy);
        renderBody(g, ox, oy);
        g.setComposite(old);
    }

    private static final Color SKIN_EDGE = new Color(10, 3, 5);
    private static final Color SKIN_DEEP = new Color(44, 10, 14);
    private static final Color SKIN_WET = new Color(82, 22, 26);
    private static final Color BONE = new Color(218, 208, 190);
    private static final Color GUM = new Color(110, 14, 22);

    /**
     * A lumpy, near-black mass: an irregular rim (several noise frequencies),
     * swelling sacks along its back, bone spurs through the skin and a round
     * lamprey maw ringed with teeth on the side it is heading toward.
     */
    private void renderBody(Graphics2D g, int ox, int oy) {
        double cx = x - ox, cy = bodyY() - oy;
        if (state instanceof StunnedState) cx += Math.sin(world.time * 40) * 2;
        double stretch = stepT < 1 ? Math.sin(Math.PI * stepT) * 0.16 : 0;
        double ca = Math.cos(heading), sa = Math.sin(heading);
        int n = 28;
        int[] ex = new int[n], ey = new int[n], mx = new int[n], my = new int[n];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            double r = BODY_RADIUS * (1 + 0.09 * Math.sin(pulse + i * 1.7) + 0.07 * Math.sin(pulse * 1.6 + i * 2.9)
                    + 0.10 * Math.sin(i * 4.3 + 1.1));
            double lx = Math.cos(a) * r * (1 + stretch), ly = Math.sin(a) * r * 0.78 * (1 - stretch * 0.5);
            double wx = lx * ca - ly * sa, wy = (lx * sa + ly * ca) * 0.8;
            ex[i] = (int) Math.round(cx + wx);
            ey[i] = (int) Math.round(cy + wy);
            mx[i] = (int) Math.round(cx + wx * 0.82);
            my[i] = (int) Math.round(cy + wy * 0.82 - 2);
        }
        // bone spurs pierce the rim, pointing away from the way it moves
        g.setColor(BONE);
        for (int k = 0; k < 6; k++) {
            double a = heading + Math.PI + (k - 2.5) * 0.45;
            double r0 = BODY_RADIUS * 0.75, r1 = BODY_RADIUS * (1.25 + 0.12 * (k % 2));
            int bx = (int) Math.round(cx + Math.cos(a) * r0), by = (int) Math.round(cy + Math.sin(a) * r0 * 0.7);
            int tx = (int) Math.round(cx + Math.cos(a) * r1), ty = (int) Math.round(cy + Math.sin(a) * r1 * 0.7 - 6);
            double px = -Math.sin(a) * 2.2, py = Math.cos(a) * 2.2;
            g.fillPolygon(new int[]{(int) (bx + px), (int) (bx - px), tx}, new int[]{(int) (by + py), (int) (by - py), ty}, 3);
        }
        g.setColor(SKIN_EDGE);
        g.fillPolygon(ex, ey, n);
        g.setPaint(new java.awt.RadialGradientPaint((float) cx, (float) (cy - 4), (float) BODY_RADIUS,
                new float[]{0f, 0.7f, 1f}, new Color[]{SKIN_WET, SKIN_DEEP, SKIN_EDGE}));
        g.fillPolygon(mx, my, n);
        // swelling sacks on its back, breathing out of step
        for (int k = 0; k < 4; k++) {
            double a = heading + Math.PI + (k - 1.5) * 0.7;
            double swell = 0.75 + 0.25 * Math.sin(pulse * 0.9 + k * 1.9);
            double r = BODY_RADIUS * 0.42;
            int sx = (int) Math.round(cx + Math.cos(a) * r), sy = (int) Math.round(cy + Math.sin(a) * r * 0.6 - 5);
            int sw = (int) Math.round(9 * swell), sh = (int) Math.round(7 * swell);
            g.setColor(SKIN_DEEP);
            g.fillOval(sx - sw / 2, sy - sh / 2, sw, sh);
            g.setColor(new Color(120, 40, 40, 120));
            g.fillOval(sx - sw / 4, sy - sh / 2 + 1, Math.max(1, sw / 3), Math.max(1, sh / 3));
        }
        // faintly glowing veins
        g.setColor(new Color(150, 30, 30, 110 + (int) (60 * Math.sin(pulse))));
        for (int k = 0; k < 5; k++) {
            double a = k * 1.25 + 0.4;
            int x0 = (int) Math.round(cx + Math.cos(a) * 4), y0 = (int) Math.round(cy + Math.sin(a) * 3);
            int x1 = (int) Math.round(cx + Math.cos(a + 0.3) * BODY_RADIUS * 0.75);
            int y1 = (int) Math.round(cy + Math.sin(a + 0.3) * BODY_RADIUS * 0.55);
            g.drawLine(x0, y0, x1, y1);
        }
        // lamprey maw on the front: gums, a black throat, a ring of teeth pointing in
        double open = isChasing() ? 0.75 + 0.25 * Math.abs(Math.sin(pulse * 0.8)) : 0.35;
        double mr = 9 * open + 3;
        double mcx = cx + ca * BODY_RADIUS * 0.45, mcy = cy + sa * BODY_RADIUS * 0.3 + 1;
        g.setColor(GUM);
        g.fill(new java.awt.geom.Ellipse2D.Double(mcx - mr - 2, mcy - mr * 0.8 - 2, (mr + 2) * 2, (mr * 0.8 + 2) * 2));
        g.setColor(Color.BLACK);
        g.fill(new java.awt.geom.Ellipse2D.Double(mcx - mr * 0.55, mcy - mr * 0.45, mr * 1.1, mr * 0.9));
        g.setColor(BONE);
        int teeth = 12;
        for (int k = 0; k < teeth; k++) {
            double a = Math.PI * 2 * k / teeth + pulse * 0.05;
            double ox0 = mcx + Math.cos(a) * mr, oy0 = mcy + Math.sin(a) * mr * 0.8;
            double ix = mcx + Math.cos(a) * mr * 0.5, iy = mcy + Math.sin(a) * mr * 0.4;
            double px = -Math.sin(a) * 1.2, py = Math.cos(a) * 1.2;
            g.fillPolygon(new int[]{(int) Math.round(ox0 + px), (int) Math.round(ox0 - px), (int) Math.round(ix)},
                    new int[]{(int) Math.round(oy0 + py), (int) Math.round(oy0 - py), (int) Math.round(iy)}, 3);
        }
        // drool
        if (isChasing()) {
            g.setColor(new Color(170, 150, 140, 150));
            int dy = (int) ((world.time * 20) % 8);
            g.fillRect((int) Math.round(mcx + 2), (int) Math.round(mcy + mr * 0.8) + dy, 1, 2);
        }
    }

    // a faint glow of its own (as in the original), so its outline shows in total darkness

    @Override
    public double lightX() {
        return x;
    }

    @Override
    public double lightY() {
        return y - 10;
    }

    @Override
    public double lightRadius() {
        return dormant > 0 ? 0 : 26 * alpha;
    }

    @Override
    public double lightIntensity() {
        return 0.08;
    }

    @Override
    public Color lightTint() {
        return new Color(200, 40, 40, 40);
    }

    @Override
    public boolean warms() {
        return false;
    }

    /** A cluster of glowing eyes drawn on top of the darkness, so the player gets a warning. */
    public void renderEyes(Graphics2D g, int ox, int oy) {
        double d = distanceTo(world.player);
        if (dormant > 0 || d > EYES_RANGE || alpha < 0.3) return;
        // while it is not hunting you the eyes only catch the light now and then
        if (!isChasing() && Math.sin(pulse * 0.37) < 0.55) return;
        int a = (int) (255 * Math.min(1, (EYES_RANGE - d) / 90) * alpha);
        double cx = x - ox, cy = bodyY() - oy;
        double ca = Math.cos(heading), sa = Math.sin(heading);
        g.setColor(isChasing() ? new Color(255, 60, 40, a) : new Color(240, 220, 160, a / 2));
        // a tight cluster of pin-prick eyes above the maw
        double mcx = cx + ca * BODY_RADIUS * 0.45, mcy = cy + sa * BODY_RADIUS * 0.3 + 1;
        for (int i = 0; i < 9; i++) {
            // they blink out of step with each other
            if (Math.sin(pulse * 0.7 + i * 2.3) > 0.9) continue;
            double ang = -Math.PI / 2 + (i - 4) * 0.42;
            double r = 15 + (i % 3) * 3;
            int size = i % 4 == 0 ? 2 : 1;
            g.fillRect((int) Math.round(mcx + Math.cos(ang) * r), (int) Math.round(mcy + Math.sin(ang) * r * 0.6), size, size);
        }
    }
}
