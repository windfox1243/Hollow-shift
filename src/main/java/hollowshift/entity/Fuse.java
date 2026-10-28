package hollowshift.entity;

/** Carried to an empty fuse box to restore part of the power. */
public final class Fuse extends Carryable {
    public Fuse(double x, double y) {
        super(x, y, "fuse");
    }

    @Override
    protected String sprite() {
        return "item_fuse";
    }

    @Override
    public boolean throwable() {
        return false;
    }
}
