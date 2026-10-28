package hollowshift.entity;

import hollowshift.engine.Lang;
import java.awt.Color;

/** The way out. Usable once the shift's objectives report that power is restored. */
public final class Elevator extends Fixture implements Interactable, LightSource {
    public Elevator(double x, double y) {
        super(x, y);
    }

    private boolean ready() {
        return world.objectives().exitOpen();
    }

    @Override
    protected String sprite() {
        return ready() ? "elevator_1" : "elevator_0";
    }

    @Override
    public String prompt(Player p) {
        if (world.objectives().escaping()) return null;
        return ready() ? Lang.t("prompt.lift") : world.objectives().exitLockedReason();
    }

    @Override
    public boolean canInteract(Player p) {
        return ready() && !world.objectives().escaping();
    }

    @Override
    public void interact(Player p) {
        world.objectives().beginEscape();
    }

    @Override
    public double interactRange() {
        return 30;
    }

    @Override
    public double lightX() {
        return x;
    }

    @Override
    public double lightY() {
        return y - 10;
    }

    @Override
    public double lightRadius() {
        return ready() ? 40 : 16;
    }

    @Override
    public double lightIntensity() {
        return ready() ? 0.55 : 0.3;
    }

    @Override
    public Color lightTint() {
        return ready() ? new Color(255, 236, 170, 50) : new Color(255, 40, 30, 50);
    }

    @Override
    public boolean warms() {
        return false;
    }
}
