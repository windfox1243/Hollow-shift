package hollowshift.entity;

/** A jerrycan of diesel for the generator. Heavy: it takes both hands, like a fuse. */
public final class FuelCan extends Carryable {
    public FuelCan(double x, double y) {
        super(x, y, "fuel");
    }

    @Override
    protected String sprite() {
        return "item_fuel";
    }

    @Override
    public boolean throwable() {
        return false;
    }
}
