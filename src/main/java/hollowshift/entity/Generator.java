package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;
import hollowshift.game.Job;

import java.awt.Color;

/** A diesel generator for the lift motor. Bring a can of fuel and pour it in; it coughs into life, loudly. */
public final class Generator extends Fixture implements Interactable, LightSource {
    public static final double START_NOISE = 420;
    private boolean running;
    private double t;
    private double chug;

    public Generator(double x, double y) {
        super(x, y);
        hw = 7;
        hh = 4;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    protected String sprite() {
        return running ? "generator_" + (1 + (int) (t * 10) % 2) : "generator_0";
    }

    @Override
    public String prompt(Player p) {
        if (running) return null;
        return p.held() instanceof FuelCan ? Lang.t("prompt.fuel") : Lang.t("prompt.needsFuel");
    }

    @Override
    public boolean canInteract(Player p) {
        return !running && p.held() instanceof FuelCan;
    }

    @Override
    public double holdTime() {
        return 3;
    }

    @Override
    public void interact(Player p) {
        p.consumeHeld();
        running = true;
        world.audio.playAt("surge", x, y, 900, 1, 0.6);
        world.audio.playAt("power_on", x, y, 700, 1, 0.7);
        world.camera.shakeFrom(x, y, 40, 0.4);
        world.emitNoise(new Noise(x, y, START_NOISE, true));
        world.objectives().onJobDone(Job.FUEL);
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (!running) return;
        chug -= dt;
        if (chug <= 0) {
            chug = 1.6;
            world.audio.playAt("creature_step", x, y, 300, 0.35, 0.5);
        }
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
        return running ? 22 + Math.sin(t * 30) * 2 : 0;
    }

    @Override
    public double lightIntensity() {
        return 0.35;
    }

    @Override
    public Color lightTint() {
        return new Color(255, 200, 120, 50);
    }

    @Override
    public boolean warms() {
        return running;
    }
}
