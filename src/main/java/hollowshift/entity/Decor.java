package hollowshift.entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Something the last crew left behind. Heavy props (carts, barrels, shelves) stand on their tile
 * and block it; clutter (bones, rubble, a dropped helmet) lies flat and is always walked over.
 */
public final class Decor extends Fixture {
    private final String sprite;
    private final boolean flat;
    private final boolean mirrored;

    public Decor(double x, double y, String sprite, boolean flat, boolean mirrored) {
        super(x, y);
        this.sprite = sprite;
        this.flat = flat;
        this.mirrored = mirrored;
    }

    public boolean isFlat() {
        return flat;
    }

    @Override
    protected String sprite() {
        return sprite;
    }

    /** Flat clutter is part of the floor: anyone standing on it is drawn on top. */
    @Override
    public double sortY() {
        return flat ? y - 24 : y + 4;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        if (!mirrored) {
            super.render(g, ox, oy);
            return;
        }
        BufferedImage img = world.art.get(sprite);
        int w = img.getWidth(), h = img.getHeight();
        int sx = (int) Math.round(x) - w / 2 - ox, sy = (int) Math.round(y) + 5 - h - oy;
        g.drawImage(img, sx + w, sy, -w, h, null);
    }
}
