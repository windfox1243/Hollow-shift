package hollowshift.entity;

/** An item that is saved in a pocket and used up later (battery cell, food); see {@link Player#useSelected}. */
public abstract class Consumable extends Item implements Stashable {
    protected Consumable(double x, double y, String displayName) {
        super(x, y, displayName);
    }

    @Override
    public String prompt(Player p) {
        return Stashable.prompt(p, supply());
    }

    @Override
    public boolean canInteract(Player p) {
        return p.pack().hasRoom(supply());
    }

    @Override
    public void interact(Player p) {
        p.stash(this);
    }
}
