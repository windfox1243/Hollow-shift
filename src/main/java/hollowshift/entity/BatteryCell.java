package hollowshift.entity;

import hollowshift.engine.Lang;

/** Recharges the lantern (BATTERY rule). */
public final class BatteryCell extends Consumable {
    public BatteryCell(double x, double y) {
        super(x, y, "battery");
    }

    @Override
    protected String sprite() {
        return "item_battery";
    }

    @Override
    public String prompt(Player p) {
        return Lang.t("prompt.battery");
    }

    @Override
    protected void consume(Player p) {
        p.battery = Math.min(1, p.battery + 0.6);
        world.audio.play("lantern", hollowshift.engine.audio.Bus.SFX, 1, 0.8);
        world.toast(Lang.t("toast.recharged"));
    }
}
