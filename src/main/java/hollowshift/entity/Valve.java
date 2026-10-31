package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;
import hollowshift.game.GaugePanel;

/**
 * A pressure valve on the lift's hydraulics. Bleeding it is done by hand at the gauge (a
 * {@link GaugePanel}) and ends in a long, loud hiss.
 */
public final class Valve extends Fixture implements Interactable {
    public static final double NOISE = 380;
    private boolean open;

    public Valve(double x, double y) {
        super(x, y);
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    protected String sprite() {
        return open ? "valve_1" : "valve_0";
    }

    @Override
    public String prompt(Player p) {
        return open ? null : Lang.t("prompt.valve");
    }

    @Override
    public boolean canInteract(Player p) {
        return !open;
    }

    @Override
    public void interact(Player p) {
        world.audio.play("grab", hollowshift.engine.audio.Bus.SFX, 0.6, 0.8);
        world.openPanel(new GaugePanel(world, this));
    }

    /** The last turn of the wheel: the pressure goes. */
    public void bleed() {
        if (open) return;
        open = true;
        world.audio.playAt("surge", x, y, 900, 0.7, 1.6);
        world.emitNoise(new Noise(x, y, NOISE, true));
        world.objectives().onJobDone(hollowshift.game.Job.VALVE);
    }
}
