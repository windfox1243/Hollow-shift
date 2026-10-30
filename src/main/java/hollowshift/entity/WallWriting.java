package hollowshift.entity;

import java.awt.Graphics2D;

/**
 * Words on the rock: room signs and scrawls. They are drawn on the full-resolution overlay so
 * the letters stay sharp, and only as brightly as the spot is lit (in the dark you read nothing).
 */
public interface WallWriting {
    /** Draws the words at their place on screen; light is 0..1 at that spot. */
    void renderWriting(Graphics2D g, int ox, int oy, double light);
}
