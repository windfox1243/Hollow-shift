package hollowshift.entity;

import java.awt.Color;

/** A coal brazier: warm, dim orange light. */
public final class Heater extends Fixture implements LightSource {
    private double t;

    public Heater(double x, double y) {
        super(x, y);
        hw = 7;
        hh = 4;
    }

    @Override
    protected String sprite() {
        return "heater_" + ((int) (t * 6) % 3);
    }

    @Override
    public void update(double dt) {
        t += dt;
    }

    @Override
    public double lightX() {
        return x;
    }

    @Override
    public double lightY() {
        return y - 4;
    }

    @Override
    public double lightRadius() {
        return 64 + Math.sin(t * 9) * 3 + Math.sin(t * 2.3) * 4;
    }

    @Override
    public double lightIntensity() {
        return 0.55;
    }

    @Override
    public Color lightTint() {
        return new Color(255, 120, 40, 80);
    }
}
