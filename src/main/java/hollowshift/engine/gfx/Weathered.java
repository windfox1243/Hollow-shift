package hollowshift.engine.gfx;

import hollowshift.engine.Rng;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Lettering for the world's pixel layer: painted or scratched onto old rock. Text is rasterised
 * at 1:1 with no smoothing, in the game's own font, then aged: paint flakes off, runs down in
 * drips, and handwriting wobbles. The result is lit and darkened like any other sprite.
 */
public final class Weathered {
    private Weathered() {
    }

    /** Width in pixels of the lettering paint() or scrawl() would make for text. */
    public static int width(String text) {
        Graphics2D m = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        int w = m.getFontMetrics(Text.font(8, text)).stringWidth(text) + 4;
        m.dispose();
        return w;
    }

    /** Stencil paint: faded, flaking, with a few drips. */
    public static BufferedImage paint(String text, Color paint, long seed) {
        Font font = Text.font(8, text);
        BufferedImage im = raster(text, font, paint, 2, 1, 0, seed);
        Rng rng = new Rng(seed);
        flake(im, rng, 0.1);
        drips(im, rng, new Color(paint.getRed(), paint.getGreen(), paint.getBlue(), paint.getAlpha() / 2));
        return im;
    }

    /** Chalk or coal handwriting: each letter a little off the line, strokes broken. */
    public static BufferedImage scrawl(String text, Color stroke, long seed) {
        Font font = Text.font(8, text);
        // the pixel font (English) has one-pixel strokes: shifting its letters makes them unreadable
        int wobble = font.getSize() <= 8 ? 0 : 1;
        BufferedImage im = raster(text, font, stroke, 2, 1, wobble, seed);
        flake(im, new Rng(seed * 31 + 7), 0.1);
        // pale chalk on pale brick: a one-pixel dark shadow (as the HUD text has) keeps it legible
        boolean pale = stroke.getRed() + stroke.getGreen() + stroke.getBlue() > 384;
        return pale ? shadowed(im, new Color(10, 8, 8, 170)) : im;
    }

    /** A copy one pixel larger with a dark shadow under and to the right of every stroke. */
    private static BufferedImage shadowed(BufferedImage im, Color shadow) {
        BufferedImage out = new BufferedImage(im.getWidth() + 1, im.getHeight() + 1, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++)
                if ((im.getRGB(x, y) >>> 24) != 0) out.setRGB(x + 1, y + 1, shadow.getRGB());
        Graphics2D g = out.createGraphics();
        g.drawImage(im, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Draws text letter by letter with no anti-aliasing; wobble moves letters up or down a pixel. */
    private static BufferedImage raster(String text, Font font, Color c, int padX, int padY, int wobble, long seed) {
        Graphics2D m = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        FontMetrics fm = m.getFontMetrics(font);
        int w = fm.stringWidth(text) + padX * 2, h = fm.getAscent() + fm.getDescent() + padY * 2;
        m.dispose();
        BufferedImage im = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setFont(font);
        g.setColor(c);
        Rng rng = new Rng(seed * 7 + 1);
        int x = padX, base = padY + fm.getAscent();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            g.drawString(ch, x, base + (wobble > 0 ? rng.range(-wobble, wobble + 1) : 0));
            x += fm.stringWidth(ch);
            i += Character.charCount(cp);
        }
        g.dispose();
        return im;
    }

    /** Knocks out a share of the painted pixels, and fades some of the rest. */
    private static void flake(BufferedImage im, Rng rng, double share) {
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++) {
                int argb = im.getRGB(x, y);
                if ((argb >>> 24) == 0) continue;
                if (rng.chance(share)) im.setRGB(x, y, 0);
                else if (rng.chance(0.3)) im.setRGB(x, y, (argb & 0xFFFFFF) | ((argb >>> 24) * 3 / 5) << 24);
            }
    }

    /** A few runs of paint dribbling down from the bottom of the letters. */
    private static void drips(BufferedImage im, Rng rng, Color c) {
        int n = Math.max(1, im.getWidth() / 10);
        for (int i = 0; i < n; i++) {
            int x = rng.range(0, im.getWidth());
            int bottom = -1;
            for (int y = im.getHeight() - 1; y >= 0 && bottom < 0; y--) if ((im.getRGB(x, y) >>> 24) != 0) bottom = y;
            if (bottom < 0) continue;
            int len = rng.range(1, 4);
            for (int y = bottom + 1; y <= Math.min(im.getHeight() - 1, bottom + len); y++) im.setRGB(x, y, c.getRGB());
        }
    }
}
