package hollowshift.entity;

/** An item used up the moment it is picked up (battery cell, food). */
public abstract class Consumable extends Item {
    protected Consumable(double x, double y, String displayName) {
        super(x, y, displayName);
    }

    protected abstract void consume(Player p);

    @Override
    public boolean canInteract(Player p) {
        return true;
    }

    @Override
    public void interact(Player p) {
        consume(p);
        remove();
    }
}
