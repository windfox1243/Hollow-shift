package hollowshift.entity;

import hollowshift.engine.gfx.Weathered;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Something someone wrote on the rock in chalk or with a lump of coal: wobbly, broken strokes,
 * faint. Part of the pixel art, so you only make it out with a light on it.
 */
public final class Scrawl extends Entity {
    private final String text;
    private final boolean chalk;
    /** Pixels the line sits above or below the middle of the wall face. */
    private final int lift;
    private BufferedImage image;

    public Scrawl(double x, double y, String text, boolean chalk, int lift) {
        super(x, y);
        this.text = text;
        this.chalk = chalk;
        this.lift = lift;
    }

    public String text() {
        return text;
    }

    @Override
    public double sortY() {
        return y - 16;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        if (image == null) {
            Color stroke = chalk ? new Color(232, 228, 214, 215) : new Color(20, 18, 18, 225);
            image = Weathered.scrawl(text, stroke, (long) (x * 13 + y * 29));
        }
        int sx = (int) Math.round(x) - image.getWidth() / 2 - ox;
        int sy = (int) Math.round(y) - 8 - 16 + (16 - image.getHeight()) / 2 + lift - oy;
        g.drawImage(image, sx, sy, null);
    }
}
