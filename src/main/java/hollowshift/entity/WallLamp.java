package hollowshift.entity;

import java.awt.Color;
import java.awt.Graphics2D;

/** An old work light. Some are steady, some flicker, some are dead. */
public final class WallLamp extends Fixture implements LightSource {
    public enum Mode { STEADY, FLICKER, DEAD }

    private final Mode mode;
    private double t;
    private double level = 1;
    private double nextFlicker;

    public WallLamp(double x, double y, Mode mode) {
        super(x, y);
        this.mode = mode;
    }

    @Override
    protected String sprite() {
        return "lamp";
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (mode == Mode.FLICKER) {
            nextFlicker -= dt;
            if (nextFlicker <= 0) {
                level = world.rng.chance(0.3) ? 0.05 : world.rng.range(0.6, 1.0);
                nextFlicker = level < 0.1 ? world.rng.range(0.05, 0.25) : world.rng.range(0.1, 1.6);
            }
        }
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        super.render(g, ox, oy);
        if (mode == Mode.DEAD || level < 0.1) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillOval((int) x - 2 - ox, (int) y - 6 - oy, 6, 6);
        }
    }

    @Override
    public double lightX() {
        return x;
    }

    @Override
    public double lightY() {
        return y - 2;
    }

    @Override
    public double lightRadius() {
        return mode == Mode.DEAD ? 0 : 62 * (0.85 + 0.15 * level);
    }

    @Override
    public double lightIntensity() {
        return mode == Mode.DEAD ? 0 : 0.55 * level;
    }

    @Override
    public Color lightTint() {
        return new Color(255, 220, 150, 35);
    }
}
