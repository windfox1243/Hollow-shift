package hollowshift.entity;

import java.awt.Graphics2D;

/** A machine with a signal lamp that shows through the dark, so the player can find the work. */
public interface Beacon {
    /** Drawn over the darkness, like the Tenant's eyes. */
    void renderSignal(Graphics2D g, int ox, int oy);
}
