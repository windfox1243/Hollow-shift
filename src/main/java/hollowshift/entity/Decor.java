package hollowshift.entity;

/** Something the last crew left behind: a barrel, a cart, bones. Just there to be looked at. */
public final class Decor extends Fixture {
    private final String sprite;

    public Decor(double x, double y, String sprite) {
        super(x, y);
        this.sprite = sprite;
    }

    @Override
    protected String sprite() {
        return sprite;
    }
}
