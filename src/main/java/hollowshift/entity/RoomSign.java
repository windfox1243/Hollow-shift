package hollowshift.entity;

import hollowshift.engine.gfx.Fonts;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

/**
 * A painted board by the way into a room: "STORE 3", "CREW ROOM B". The shrine sign has been
 * clawed at and the sump sign is half under dried mud.
 */
public final class RoomSign extends Entity implements WallWriting {
    public enum Damage { NONE, CLAWED, MUDDY }

    private final Font font;
    private final String text;
    private final Damage damage;
    private final int width;

    public RoomSign(double x, double y, String text, Damage damage) {
        super(x, y);
        this.text = text;
        this.damage = damage;
        font = Fonts.sign(7f, text);
        // a board a little wider than its lettering, measured in the logical 640x360 space
        Graphics2D m = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        width = (int) Math.ceil(m.getFontMetrics(font).getStringBounds(text, m).getWidth()) + 8;
        m.dispose();
    }

    public String text() {
        return text;
    }

    private int top() {
        return (int) Math.round(y) - 21; // on the wall face above the tile
    }

    @Override
    public double sortY() {
        return y - 16;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        int sx = (int) Math.round(x) - width / 2 - ox, sy = top() - oy;
        g.setColor(new Color(16, 12, 10));
        g.fillRect(sx - 1, sy - 1, width + 2, 12);
        g.setColor(new Color(92, 70, 46));
        g.fillRect(sx, sy, width, 10);
        g.setColor(new Color(112, 86, 58));
        g.drawLine(sx, sy, sx + width - 1, sy);
        g.setColor(new Color(70, 52, 34));
        g.drawLine(sx, sy + 5, sx + width - 1, sy + 5);
        g.setColor(new Color(150, 150, 160));
        g.fillRect(sx + 1, sy + 1, 1, 1);
        g.fillRect(sx + width - 2, sy + 1, 1, 1);
    }

    @Override
    public void renderWriting(Graphics2D g, int ox, int oy, double light) {
        int a = (int) (255 * Math.min(1, light * 1.6));
        if (a < 8) return;
        float cx = (float) (x - ox), base = top() - oy + 8f;
        Graphics2D t = (Graphics2D) g.create();
        t.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        t.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        t.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        t.setFont(font);
        float w = (float) t.getFontMetrics().getStringBounds(text, t).getWidth();
        // the board is lit by the lighting pass; the paint is dimmed to match
        t.setColor(new Color(226, 214, 180, a));
        t.drawString(text, cx - w / 2, base);
        if (damage == Damage.CLAWED) {
            t.setStroke(new BasicStroke(0.7f));
            t.setColor(new Color(40, 26, 20, a));
            for (int i = 0; i < 3; i++) t.draw(new Line2D.Float(cx - w / 2 + 2 + i * 2.2f, base - 7, cx - w / 2 + 6 + i * 2.6f, base + 1));
        } else if (damage == Damage.MUDDY) {
            t.setColor(new Color(64, 50, 34, a * 15 / 16));
            t.fillOval((int) cx - 1, (int) base - 9, (int) (w / 2) + 6, 12);
        }
        t.dispose();
    }
}
