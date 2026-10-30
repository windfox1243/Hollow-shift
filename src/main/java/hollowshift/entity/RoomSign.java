package hollowshift.entity;

import hollowshift.engine.Rng;
import hollowshift.engine.gfx.Weathered;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A room name stencilled straight onto the rock by the way in ("STORE 3", "CREW ROOM B"), decades
 * ago: faded, flaking, run with drips. The shrine one has been clawed at, the sump one is caked
 * in dried mud. Part of the pixel art, so it is only legible where light falls.
 */
public final class RoomSign extends Entity {
    public enum Damage { NONE, CLAWED, MUDDY }

    private final String text;
    private final Damage damage;
    /** The lift shaft is marked black on a band of old safety yellow, everything else in oxide red. */
    private final boolean safetyYellow;
    private BufferedImage image;

    public RoomSign(double x, double y, String text, Damage damage, boolean safetyYellow) {
        super(x, y);
        this.text = text;
        this.damage = damage;
        this.safetyYellow = safetyYellow;
    }

    public String text() {
        return text;
    }

    @Override
    public double sortY() {
        return y - 16; // on the wall, behind anyone standing in front of it
    }

    private BufferedImage image() {
        if (image != null) return image;
        long seed = (long) (x * 31 + y * 17);
        Color paint = safetyYellow ? new Color(26, 22, 20, 235) : new Color(118, 34, 26, 235);
        image = Weathered.paint(text, paint, seed);
        Graphics2D g = image.createGraphics();
        Rng rng = new Rng(seed + 3);
        if (safetyYellow) {
            // the yellow band goes on first, under the black letters, and has flaked too
            BufferedImage band = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int yy = 0; yy < band.getHeight(); yy++)
                for (int xx = 0; xx < band.getWidth(); xx++)
                    if (!rng.chance(0.18)) band.setRGB(xx, yy, new Color(190, 156, 60, rng.chance(0.3) ? 150 : 215).getRGB());
            Graphics2D b = band.createGraphics();
            b.drawImage(image, 0, 0, null);
            b.dispose();
            image = band;
            g.dispose();
            g = image.createGraphics();
        }
        if (damage == Damage.CLAWED) {
            // four parallel gouges through the letters
            g.setColor(new Color(30, 22, 20, 230));
            int x0 = rng.range(0, Math.max(1, image.getWidth() - 10));
            for (int i = 0; i < 4; i++) g.drawLine(x0 + i * 2, 0, x0 + i * 2 + 5, image.getHeight() - 1);
        } else if (damage == Damage.MUDDY) {
            // splashes of dried silt over the right half, thick in places, patchy in others
            int w = image.getWidth(), h = image.getHeight();
            for (int yy = 0; yy < h; yy++)
                for (int xx = w / 2; xx < w; xx++) {
                    double cover = (xx - w / 2.0) / (w / 2.0) * 0.9 + (yy > h / 2 ? 0.25 : 0);
                    if (rng.chance(cover)) image.setRGB(xx, yy, new Color(70, 54, 36, rng.chance(0.5) ? 200 : 240).getRGB());
                }
        }
        g.dispose();
        return image;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        BufferedImage im = image();
        // centred on the wall face above the tile (the face spans the 16 px above the floor tile)
        int sx = (int) Math.round(x) - im.getWidth() / 2 - ox;
        int sy = (int) Math.round(y) - 8 - 16 + (16 - im.getHeight()) / 2 - oy;
        g.drawImage(im, sx, sy, null);
    }
}
