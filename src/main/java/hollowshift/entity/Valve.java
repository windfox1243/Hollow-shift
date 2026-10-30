package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;

/** A pressure valve on the lift's hydraulics. Hold to bleed it: a long, loud hiss. */
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
    public double holdTime() {
        return 2.5;
    }

    @Override
    public void interact(Player p) {
        open = true;
        world.audio.playAt("surge", x, y, 900, 0.7, 1.6);
        world.emitNoise(new Noise(x, y, NOISE, true));
        world.objectives().onValveOpened();
    }
}
