package hollowshift.entity;

/** A spare cell for the lantern. Kept in a pocket until the light starts to fade. */
public final class BatteryCell extends Consumable {
    public BatteryCell(double x, double y) {
        super(x, y, "battery");
    }

    @Override
    protected String sprite() {
        return "item_battery";
    }

    @Override
    public Supply supply() {
        return Supply.BATTERY;
    }
}
