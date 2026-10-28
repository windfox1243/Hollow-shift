package hollowshift.entity;

import hollowshift.engine.Lang;
import java.awt.Color;

/**
 * A road flare. Throwing it lights it: it burns for a while as a bright light
 * that warms and reveals, and under the SIGHT rule it lures the Tenant.
 */
public final class Flare extends Carryable implements LightSource {
    private static final double BURN_TIME = 30;
    private boolean lit;
    private double burn;
    private double flicker;

    public Flare(double x, double y) {
        super(x, y, "flare");
    }

    public boolean isLit() {
        return lit && burn > 0;
    }

    @Override
    protected String sprite() {
        return isLit() ? "item_flare_lit" : "item_flare";
    }

    @Override
    public String prompt(Player p) {
        return isLit() ? Lang.t("prompt.flareLit") : burn < 0 ? null : Lang.t("prompt.pickup", displayName());
    }

    @Override
    public boolean canInteract(Player p) {
        return burn >= 0 && super.canInteract(p);
    }

    @Override
    public boolean throwable() {
        return true;
    }

    @Override
    public void launch(double sx, double sy, double tx, double ty) {
        if (!lit) {
            lit = true;
            burn = BURN_TIME;
            world.audio.playAt("flare", sx, sy, 400, 0.8, 1);
        }
        super.launch(sx, sy, tx, ty);
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        flicker += dt;
        if (lit && burn > 0) {
            burn -= dt;
            if (burn <= 0) burn = -1; // spent: stays as a dead stick
        }
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
        if (!isLit()) return 0;
        double fade = Math.min(1, burn / 4);
        return (105 + Math.sin(flicker * 23) * 4 + Math.sin(flicker * 7) * 3) * (0.4 + 0.6 * fade);
    }

    @Override
    public double lightIntensity() {
        return isLit() ? 0.95 : 0;
    }

    @Override
    public Color lightTint() {
        return new Color(255, 70, 40, 70);
    }
}
