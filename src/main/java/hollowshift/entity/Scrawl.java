package hollowshift.entity;

import hollowshift.engine.gfx.Fonts;

import java.awt.*;

/**
 * Something someone wrote on the rock in chalk or with a lump of coal. It only shows when your
 * light is on it and you are close enough to make it out.
 */
public final class Scrawl extends Entity implements WallWriting {
    private static final double READ_RANGE = 90;
    private final String text;
    private final boolean chalk;
    private final float tilt;

    public Scrawl(double x, double y, String text, boolean chalk, float tilt) {
        super(x, y);
        this.text = text;
        this.chalk = chalk;
        this.tilt = tilt;
    }

    public String text() {
        return text;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        // nothing in the pixel layer: the letters are drawn on the overlay
    }

    @Override
    public void renderWriting(Graphics2D g, int ox, int oy, double light) {
        double near = 1 - Math.max(0, world.player.distanceTo(x, y) - 40) / (READ_RANGE - 40);
        int a = (int) (230 * Math.min(1, light * 1.8) * Math.max(0, Math.min(1, near)));
        if (a < 8) return;
        Graphics2D t = (Graphics2D) g.create();
        t.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        t.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        t.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        t.setFont(Fonts.scrawl(7.5f, text));
        float w = (float) t.getFontMetrics().getStringBounds(text, t).getWidth();
        t.translate(x - ox, y - oy - 12);
        t.rotate(tilt);
        if (!chalk) {
            // coal on dark rock needs a faint pale edge to read at all
            t.setColor(new Color(150, 140, 120, a / 3));
            t.drawString(text, -w / 2 + 0.4f, 0.4f);
        }
        t.setColor(chalk ? new Color(222, 218, 206, a) : new Color(20, 18, 18, a));
        t.drawString(text, -w / 2, 0);
        t.dispose();
    }
}
