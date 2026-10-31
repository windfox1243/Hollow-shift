package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.ai.Noise;

import java.awt.Color;

/** Needs one fuse. Installing it is loud: the power surge carries far. */
public final class FuseBox extends Fixture implements Interactable, LightSource {
    public static final double SURGE_NOISE = 340;
    private boolean powered;
    private double glow;

    public FuseBox(double x, double y) {
        super(x, y);
    }

    public boolean isPowered() {
        return powered;
    }

    @Override
    protected String sprite() {
        return powered ? "fusebox_1" : "fusebox_0";
    }

    @Override
    public String prompt(Player p) {
        if (powered) return null;
        return p.held() instanceof Fuse ? Lang.t("prompt.install") : Lang.t("prompt.needsFuse");
    }

    @Override
    public boolean canInteract(Player p) {
        return !powered && p.held() instanceof Fuse;
    }

    @Override
    public void interact(Player p) {
        p.consumeHeld();
        powered = true;
        glow = 1;
        world.audio.playAt("fuse_insert", x, y, 600, 1, 1);
        world.audio.playAt("power_on", x, y, 600, 0.9, 1);
        world.camera.shakeFrom(x, y, 25, 0.3);
        world.emitNoise(Noise.machine(x, y, SURGE_NOISE));
        world.objectives().onJobDone(hollowshift.game.Job.FUSE);
    }

    @Override
    public void update(double dt) {
        glow = Math.max(0, glow - dt * 0.8);
    }

    @Override
    public double lightX() {
        return x;
    }

    @Override
    public double lightY() {
        return y - 6;
    }

    @Override
    public double lightRadius() {
        return powered ? 14 + glow * 40 : 0;
    }

    @Override
    public double lightIntensity() {
        return 0.3 + glow * 0.4;
    }

    @Override
    public Color lightTint() {
        return new Color(80, 255, 120, 60);
    }

    @Override
    public boolean warms() {
        return false;
    }
}
