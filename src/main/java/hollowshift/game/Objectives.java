package hollowshift.game;

import hollowshift.engine.audio.Bus;
import hollowshift.entity.Player;

import java.util.List;

/**
 * What the player must do to finish a shift. Normal shifts and the final shift
 * plug in different subclasses (Template Method: the escape sequence is shared).
 */
public abstract class Objectives {
    private static final double ESCAPE_TIME = 2.2;
    protected World world;
    private boolean escaping;
    private double escapeTimer;
    private boolean complete;
    private boolean stayed;

    void attach(World world) {
        this.world = world;
    }

    public void onFuseInstalled() {
    }

    public void onLeverPulled() {
    }

    /** Whether the lift can be used. */
    public abstract boolean exitOpen();

    /** Prompt shown at the lift while it is locked. */
    public abstract String exitLockedReason();

    /** Lines for the objective panel. */
    public abstract List<String> lines();

    /** Extra Tenant speed (final shift pressure). */
    public double huntMultiplier() {
        return 1;
    }

    public void update(double dt) {
        if (!escaping) return;
        escapeTimer += dt;
        if (escapeTimer >= ESCAPE_TIME) complete = true;
    }

    public void beginEscape() {
        escaping = true;
        Player p = world.player;
        p.setFrozen(true);
        p.x = world.elevator().x;
        p.y = world.elevator().y - 2;
        world.audio.play("elevator", Bus.SFX, 0.8, 1);
        world.audio.play("elevator_bell", Bus.SFX, 0.6, 1);
    }

    public boolean escaping() {
        return escaping;
    }

    /** 0..1 progress of the closing-doors animation. */
    public double escapeProgress() {
        return Math.min(1, escapeTimer / ESCAPE_TIME);
    }

    /** The hidden ending: the player gives themself to the mountain instead of riding up. */
    public void stayBelow() {
        stayed = true;
        complete = true;
    }

    public boolean stayed() {
        return stayed;
    }

    public boolean complete() {
        return complete;
    }
}
