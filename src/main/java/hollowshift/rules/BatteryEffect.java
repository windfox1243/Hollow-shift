package hollowshift.rules;

import hollowshift.engine.Lang;
import hollowshift.entity.Player;
import hollowshift.game.World;

import java.awt.Color;

/** BATTERY: the lantern drains a full charge in about 80 seconds of use. */
public final class BatteryEffect implements StatusEffect {
    private static final double DRAIN_SECONDS = 80;

    @Override
    public void update(Player p, World world, double dt) {
        if (!p.lanternOn()) return;
        p.battery = Math.max(0, p.battery - dt / DRAIN_SECONDS);
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
