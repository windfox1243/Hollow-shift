package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.ai.Noise;

/** Final-shift breaker lever: hold to pull. Very loud. */
public final class Lever extends Fixture implements Interactable {
    private boolean pulled;

    public Lever(double x, double y) {
        super(x, y);
    }

    public boolean isPulled() {
        return pulled;
    }

    @Override
    protected String sprite() {
        return pulled ? "lever_1" : "lever_0";
    }

    @Override
    public String prompt(Player p) {
        return pulled ? null : Lang.t("prompt.lever");
    }

    @Override
    public boolean canInteract(Player p) {
        return !pulled;
    }

    @Override
    public double holdTime() {
        return 2.0;
    }

    @Override
    public void interact(Player p) {
        pulled = true;
        world.audio.playAt("surge", x, y, 2000, 0.8, 1);
        world.audio.playAt("power_on", x, y, 800, 1, 0.8);
        world.camera.shakeFrom(x, y, 40, 0.5);
        world.emitNoise(new Noise(x, y, 520, true));
        world.objectives().onLeverPulled();
    }
}
