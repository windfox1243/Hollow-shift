package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.ai.Noise;
import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.MathUtil;
import hollowshift.rules.Rule;
import hollowshift.world.Tile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** The miner. Moves, runs, carries one item and works the lantern. */
public final class Player extends Entity implements LightSource {
    public static final double WALK_SPEED = 64;
    public static final double RUN_SPEED = 108;
    // noise radii follow the original (footstep 175, pick-up 500 in 320 px rooms), scaled to 240 px rooms
    public static final double STEP_NOISE_WALK = 130;
    public static final double STEP_NOISE_RUN = 230;
    public static final double PICKUP_NOISE = 375;

    // survival stats, 0..1 (used only when the matching rule is active)
    public double battery = 1;
    public double cold;
    public double food = 1;
    public double stamina = 1;
    /** Multiplier applied to speed by status effects (cold, hunger). */
    public double speedFactor = 1;

    private Carryable held;
    private boolean lanternOn = true;
    private boolean running;
    private boolean moving;
    private boolean frozen;
    private double facingX = 1, facingY = 0;
    private double animTime;
    private double idleTime;
    private double stepTimer;
    // velocity with acceleration, like the original's 8-direction movement
    private double vx, vy;
    private static final double ACCEL = 900, DECEL = 1100;
    private boolean exhausted;
    private Interactable focus;
    private double holdProgress;

    public Player(double x, double y) {
        super(x, y);
        hw = 4;
        hh = 3;
    }

    public void handleInput(Input in, double dt) {
        if (frozen) {
            moving = running = false;
            vx = vy = 0;
            return;
        }
        double mx = 0, my = 0;
        if (in.held(Action.LEFT)) mx -= 1;
        if (in.held(Action.RIGHT)) mx += 1;
        if (in.held(Action.UP)) my -= 1;
        if (in.held(Action.DOWN)) my += 1;
        moving = mx != 0 || my != 0;
        if (moving) {
            double len = Math.hypot(mx, my);
            mx /= len;
            my /= len;
            facingX = mx;
            facingY = my;
        }

        boolean wantRun = in.held(Action.RUN) && moving && !exhausted;
        running = wantRun;
        if (running) {
            stamina -= dt / 3.5;
            if (stamina <= 0) {
                stamina = 0;
                exhausted = true;
            }
        } else {
            stamina = Math.min(1, stamina + dt / (moving ? 6 : 3.5));
            if (exhausted && stamina > 0.35) exhausted = false;
        }

        double speed = (running ? RUN_SPEED : WALK_SPEED) * speedFactor;
        // ease toward the wanted velocity: quick to start, a short slide to stop
        double tvx = moving ? mx * speed : 0, tvy = moving ? my * speed : 0;
        double rate = (moving ? ACCEL : DECEL) * dt;
        vx = approach(vx, tvx, rate);
        vy = approach(vy, tvy, rate);
        if (vx != 0 || vy != 0) {
            double ox = x, oy = y;
            moveAndSlide(vx * dt, vy * dt);
            // walls stop the velocity on the blocked axis so it does not stick
            if (Math.abs(x - ox) < Math.abs(vx * dt) * 0.5) vx = 0;
            if (Math.abs(y - oy) < Math.abs(vy * dt) * 0.5) vy = 0;
        }
        double actual = Math.hypot(vx, vy);
        if (moving) {
            animTime += dt * (running ? 13 : 8) * Math.min(1, actual / WALK_SPEED);
            idleTime = 0;
            stepTimer -= dt;
            if (stepTimer <= 0) {
                stepTimer = running ? 0.27 : 0.42;
                footstep();
            }
        } else {
            idleTime += dt;
            stepTimer = 0.1;
        }

        if (in.pressed(Action.LANTERN)) toggleLantern();
        if (in.pressed(Action.THROW) && held != null) throwOrDrop();
        updateInteraction(in, dt);
    }

    private static double approach(double v, double target, double step) {
        double d = target - v;
        return Math.abs(d) <= step ? target : v + Math.signum(d) * step;
    }

    private void footstep() {
        Tile t = world.map.tileAt(x, y);
        boolean quiet = t.quiet;
        world.audio.play(quiet ? "step_soft" : "step", hollowshift.engine.audio.Bus.SFX,
                running ? 0.6 : 0.35, world.rng.range(0.85, 1.15));
        if (!quiet) world.emitNoise(new Noise(x, y, running ? STEP_NOISE_RUN : STEP_NOISE_WALK, true));
        else if (running) world.emitNoise(new Noise(x, y, STEP_NOISE_WALK, true));
    }

    private void toggleLantern() {
        if (!lanternOn && world.rules.has(Rule.BATTERY) && battery <= 0) {
            world.toast(Lang.t("toast.lanternDead"));
            return;
        }
        lanternOn = !lanternOn;
        world.audio.play("lantern");
    }

    private void throwOrDrop() {
        Carryable item = held;
        held = null;
        item.setHeld(false);
        if (item.throwable()) {
            world.audio.play("throw", hollowshift.engine.audio.Bus.SFX, 0.7, 1);
            world.emitNoise(new Noise(x, y, 60, true));
            item.launch(x + facingX * 6, y + facingY * 6, x + facingX * 130, y + facingY * 130);
        } else {
            item.x = x + facingX * 8;
            item.y = y + facingY * 8;
            if (world.map.boxBlocked(item.x - 3, item.y - 2, item.x + 3, item.y + 2)) {
                item.x = x;
                item.y = y;
            }
            world.audio.play("drop", hollowshift.engine.audio.Bus.SFX, 0.6, 1);
        }
    }

    private void updateInteraction(Input in, double dt) {
        Interactable best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : world.entities()) {
            if (!(e instanceof Interactable it) || e == held || e.isRemoved()) continue;
            if (it.prompt(this) == null) continue;
            double d = distanceTo(e.x, e.y);
            if (d < it.interactRange() && d < bestD) {
                best = it;
                bestD = d;
            }
        }
        if (best != focus) holdProgress = 0;
        focus = best;
        if (focus == null || !focus.canInteract(this)) {
            holdProgress = 0;
            return;
        }
        if (focus.holdTime() <= 0) {
            if (in.pressed(Action.INTERACT)) {
                focus.interact(this);
                focus = null;
            }
        } else if (in.held(Action.INTERACT)) {
            holdProgress += dt / focus.holdTime();
            if (holdProgress >= 1) {
                holdProgress = 0;
                focus.interact(this);
                focus = null;
            }
        } else {
            holdProgress = 0;
        }
    }

    public void pickUp(Carryable item) {
        held = item;
        item.setHeld(true);
        world.audio.play("grab", hollowshift.engine.audio.Bus.SFX, 0.8, 1);
        world.emitNoise(new Noise(x, y, PICKUP_NOISE, true));
        world.toast(Lang.t(item.throwable() ? "toast.pickedThrow" : "toast.pickedDrop", item.displayName(), keyHint(Action.THROW)));
    }

    private String keyHint(Action a) {
        return Input.keyName(world.input().binding(a)[0]);
    }

    /** Removes the held item from the world (used when it is installed somewhere). */
    public void consumeHeld() {
        if (held != null) {
            held.setHeld(false);
            held.remove();
            held = null;
        }
    }

    public Carryable held() {
        return held;
    }

    public boolean isHolding() {
        return held != null;
    }

    public boolean isRunning() {
        return running && moving;
    }

    public boolean lanternOn() {
        return lanternOn;
    }

    public void forceLanternOff() {
        if (lanternOn) {
            lanternOn = false;
            world.audio.play("lantern", hollowshift.engine.audio.Bus.SFX, 1, 0.6);
        }
    }

    public Interactable focus() {
        return focus;
    }

    public double holdProgress() {
        return holdProgress;
    }

    public void setFrozen(boolean f) {
        frozen = f;
    }

    /** True while held by the Tenant or riding the lift. */
    public boolean isFrozen() {
        return frozen;
    }

    public double facingX() {
        return facingX;
    }

    // ---- light ---------------------------------------------------------------

    @Override
    public double lightX() {
        return x + facingX * 10;
    }

    @Override
    public double lightY() {
        return y - 8 + facingY * 10;
    }

    @Override
    public double lightRadius() {
        if (!lanternOn) return 0;
        double flick = world.rules.has(Rule.BATTERY) && battery < 0.15 ? (Math.sin(animTime * 37 + world.time * 31) > 0.6 ? 0.6 : 1) : 1;
        return 88 * flick * (world.rules.has(Rule.BATTERY) ? 0.75 + 0.25 * MathUtil.clamp(battery * 3, 0, 1) : 1);
    }

    @Override
    public double lightIntensity() {
        return lanternOn ? 1 : 0;
    }

    @Override
    public Color lightTint() {
        return new Color(255, 214, 140, 40);
    }

    @Override
    public boolean warms() {
        return false; // the lantern only slows the cold, see ColdEffect
    }

    // ---- drawing ---------------------------------------------------------------

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        int frame = moving ? 1 + ((int) animTime % 3) : 0;
        BufferedImage img = world.art.get("player_" + frame);
        int sx = (int) Math.round(x) - ox, sy = (int) Math.round(y) - oy;
        g.setColor(new Color(0, 0, 0, 100));
        g.fillOval(sx - 5, sy - 1, 10, 4);
        int w = img.getWidth(), h = img.getHeight();
        // walk cycle as in the original: rock side to side with each step, hop slightly,
        // and breathe (a slow squash) while standing still
        double phase = animTime * Math.PI / 2;
        double tilt = moving ? Math.sin(phase) * (running ? 0.12 : 0.07) : 0;
        double hop = moving ? Math.abs(Math.sin(phase)) * (running ? 2 : 1.2) : 0;
        double breathe = moving ? 0 : Math.sin(idleTime * 3) * 0.04;
        AffineTransform old = g.getTransform();
        g.translate(sx, sy + 2 - hop);
        g.rotate(tilt);
        g.scale((facingX < 0 ? -1 : 1) * (1 - breathe), 1 + breathe);
        g.drawImage(img, -w / 2, -h, null);
        g.setTransform(old);
        if (held != null) {
            BufferedImage it = world.art.get(held.sprite());
            g.drawImage(it, sx - it.getWidth() / 2 + (int) (facingX * 5), (int) (sy - h - 2 - hop), null);
        }
    }
}
