package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;
import hollowshift.game.Job;
import hollowshift.game.WiringPanel;

import java.awt.Graphics2D;

/** A junction box for the lift's signal line, its wires torn loose. Rewire it at the box (a {@link WiringPanel}). */
public final class JunctionBox extends Fixture implements Interactable, Beacon {
    public static final double NOISE = 200;
    private boolean wired;

    public JunctionBox(double x, double y) {
        super(x, y);
    }

    public boolean isWired() {
        return wired;
    }

    @Override
    protected String sprite() {
        return wired ? "junction_1" : "junction_0";
    }

    @Override
    public String prompt(Player p) {
        return wired ? null : Lang.t("prompt.wires");
    }

    @Override
    public boolean canInteract(Player p) {
        return !wired;
    }

    @Override
    public void interact(Player p) {
        world.audio.play("grab", hollowshift.engine.audio.Bus.SFX, 0.6, 0.8);
        world.openPanel(new WiringPanel(world, this));
    }

    /** The last wire is in. */
    public void wire() {
        if (wired) return;
        wired = true;
        world.audio.playAt("power_on", x, y, 600, 0.8, 1.3);
        world.emitNoise(Noise.machine(x, y, NOISE));
        world.objectives().onJobDone(Job.WIRES);
    }

    @Override
    public void renderSignal(Graphics2D g, int ox, int oy) {
        SignalLamp.render(g, x + 4 - ox, y - 10 - oy, !wired, world.time, (long) (x * 5 + y * 3));
    }
}
