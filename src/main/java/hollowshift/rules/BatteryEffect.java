package hollowshift.rules;

import hollowshift.engine.Lang;
import hollowshift.entity.Player;
import hollowshift.game.World;

import java.awt.Color;

/**
 * The lantern always runs on a battery: a full charge lasts about three and a half minutes
 * of light, or 80 seconds under the BATTERY rule. Spare cells are kept in a pocket.
 */
public final class BatteryEffect implements StatusEffect {
    public static final double DRAIN_SECONDS = 210;
    public static final double DRAIN_SECONDS_RULE = 80;
    private final double drainSeconds;

    public BatteryEffect(double drainSeconds) {
        this.drainSeconds = drainSeconds;
    }

    @Override
    public void update(Player p, World world, double dt) {
        if (!p.lanternOn()) return;
        p.battery = Math.max(0, p.battery - dt / drainSeconds);
        if (p.battery <= 0) {
            p.forceLanternOff();
            world.toast(Lang.t("toast.lanternDied"));
        }
    }

    @Override
    public String label() {
        return Lang.t("status.LAMP");
    }

    @Override
    public double value(Player p) {
        return p.battery;
    }

    @Override
    public Color color() {
        return new Color(240, 210, 90);
    }

    @Override
    public boolean critical(Player p) {
        return p.battery < 0.2;
    }
}
