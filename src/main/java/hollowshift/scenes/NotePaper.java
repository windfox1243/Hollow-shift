package hollowshift.scenes;

import hollowshift.engine.Rng;
import hollowshift.engine.Window;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a found note full size. The text uses an ordinary, anti-aliased system
 * font (the pixel font is too small to read a paragraph). Each note gets its
 * own wear matching what it is: oil, flood water, coal fingerprints, blood,
 * dust; note 7 is chalk on rock rather than paper.
 */
final class NotePaper {
    private static final int W = 440;
    private static final int PAD = 24;
    private static final int LINE = 19;
    /** Empty band under the text where the heavy stains go, so they never cover a word. */
    private static final int STAIN_BAND = 40;
    private static final Font FONT = pickFont(Font.PLAIN, 14);
    private static final Font FONT_BOLD = pickFont(Font.BOLD, 14);

    /** What happened to this piece of paper. */
    private record Wear(boolean coal, boolean oil, boolean water, boolean ring, boolean blood, boolean creases,
                        boolean tornTop, boolean chalkOnRock) {
    }

    private static Wear wearFor(int note) {
        return switch (note) {
            case 1 -> new Wear(true, false, false, false, false, true, false, false);  // foreman's notebook
            case 2 -> new Wear(false, false, false, true, false, true, true, false);   // torn calendar page
            case 3 -> new Wear(true, true, false, false, false, false, false, false);  // maintenance log
            case 4 -> new Wear(true, true, false, false, true, true, false, false);    // oily scrap
            case 5 -> new Wear(false, false, false, true, false, true, false, false);  // unsent letter
            case 6 -> new Wear(false, false, true, false, false, true, false, false);  // flood report
            case 7 -> new Wear(false, false, false, false, false, false, false, true); // chalk on the wall
            case 8 -> new Wear(true, false, false, false, true, false, false, false);  // tape label
            case 9 -> new Wear(false, false, true, false, true, true, false, false);   // roster
            default -> new Wear(true, false, false, false, true, true, true, false);   // your own handwriting
        };
    }

    private static Font pickFont(int style, int size) {
        String[] wanted = {"Segoe UI", "Tahoma", "Arial"};
        String[] have = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String w : wanted) for (String h : have) if (h.equals(w)) return new Font(w, style, size);
        return new Font(Font.DIALOG, style, size);
    }

    void render(Graphics2D g, int note, String text, String closeHint) {
        Wear wear = wearFor(note);
        Rng rng = new Rng(note * 7919L);
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        p.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        p.setColor(new Color(0, 0, 0, 175));
        p.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);

        // the first line ("Sổ tay tổ trưởng...:") says what the thing is; it is set apart in bold
        String[] parts = text.split("\n", 2);
        String heading = parts.length > 1 ? parts[0] : null;
        String bodyText = parts.length > 1 ? parts[1] : parts[0];
        List<String> lines = wrap(p, bodyText, W - PAD * 2);
        int textH = lines.size() * LINE + (heading != null ? LINE + 6 : 0);
        int h = Math.max(130, textH + PAD * 2 + 4 + STAIN_BAND);
        int x = (Window.WIDTH - W) / 2, top = Math.max(16, (Window.HEIGHT - h) / 2 - 10);
        int band = top + PAD + 10 + textH - LINE + 12; // first free y below the text

        Shape sheet = sheet(x, top, W, h, wear, rng);
        if (wear.chalkOnRock) drawRock(p, sheet, x, top, W, h, rng);
        else drawPaper(p, sheet, x, top, W, h, band, wear, rng);

        // text
        Color ink = wear.chalkOnRock ? new Color(226, 222, 212) : new Color(38, 30, 26);
        int y = top + PAD + 10;
        if (heading != null) {
            p.setFont(FONT_BOLD);
            p.setColor(wear.chalkOnRock ? new Color(200, 196, 186) : new Color(90, 70, 56));
            p.drawString(heading, x + PAD, y);
            y += LINE + 6;
        }
        p.setFont(FONT);
        for (String line : lines) {
            if (wear.chalkOnRock) chalkLine(p, line, x + PAD, y, rng);
            else {
                p.setColor(ink);
                p.drawString(line, x + PAD, y);
            }
            y += LINE;
        }

        // a few specks of grime over the ink, light enough not to hide a letter
        p.setClip(sheet);
        for (int i = 0; i < 40; i++) {
            p.setColor(new Color(40, 30, 24, rng.range(20, 60)));
            int s = rng.range(1, 3);
            p.fillOval(x + rng.range(0, W), top + rng.range(0, h), s, s);
        }
        if (wear.blood) bloodSpeckles(p, x, band, top + h - band, rng);
        p.setClip(null);

        p.setFont(FONT);
        p.setColor(new Color(200, 190, 170));
        FontMetrics fm = p.getFontMetrics();
        p.drawString(closeHint, (Window.WIDTH - fm.stringWidth(closeHint)) / 2, Math.min(Window.HEIGHT - 8, top + h + 22));
        p.dispose();
    }

    // ---- the sheet -----------------------------------------------------------------------------

    private static Shape sheet(int x, int y, int w, int h, Wear wear, Rng rng) {
        Path2D s = new Path2D.Double();
        if (wear.tornTop) {
            s.moveTo(x, y + 6);
            for (int i = 1; i <= 24; i++) s.lineTo(x + w * i / 24.0, y + rng.range(0, 9));
        } else {
            s.moveTo(x, y);
            s.lineTo(x + w, y);
        }
        s.lineTo(x + w, y + h - 14);
        s.lineTo(x + w - 14, y + h); // a folded-off corner
        s.lineTo(x, y + h);
        s.closePath();
        return s;
    }

    private void drawPaper(Graphics2D p, Shape sheet, int x, int y, int w, int h, int band, Wear wear, Rng rng) {
        // aged paper, a little different every time
        int warm = rng.range(-8, 8);
        Color base = new Color(214 + warm / 2, 202 + warm / 2, 172 + warm);
        p.setPaint(new RadialGradientPaint(x + w * 0.45f, y + h * 0.4f, w * 0.75f, new float[]{0f, 1f},
                new Color[]{base, new Color(178 + warm / 2, 160, 124)}));
        p.fill(sheet);
        p.setClip(sheet);

        // dust everywhere
        for (int i = 0; i < 260; i++) {
            p.setColor(new Color(110, 92, 70, rng.range(10, 40)));
            p.fillRect(x + rng.range(0, w), y + rng.range(0, h), 1, 1);
        }
        // smudges of dirt
        for (int i = 0; i < 4; i++) blob(p, x + rng.range(0, w), y + rng.range(0, h), rng.range(18, 40), new Color(120, 96, 66), 28, rng);
        if (wear.creases) {
            p.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 2; i++) {
                p.setColor(new Color(255, 250, 235, 60));
                int cy = y + rng.range(h / 4, h * 3 / 4);
                p.drawLine(x, cy, x + w, cy + rng.range(-10, 10));
                p.setColor(new Color(90, 70, 50, 40));
                p.drawLine(x, cy + 1, x + w, cy + 1 + rng.range(-10, 10));
            }
        }
        if (wear.water) {
            // flood damage: a tide line along the bottom and a blotch
            p.setPaint(new GradientPaint(x, y + h * 0.55f, new Color(120, 104, 74, 0), x, y + h, new Color(110, 94, 66, 110)));
            p.fillRect(x, y + h / 2, w, h / 2);
            p.setColor(new Color(96, 78, 54, 90));
            p.setStroke(new BasicStroke(1.4f));
            Path2D tide = new Path2D.Double();
            tide.moveTo(x, y + h * 0.58);
            for (int i = 1; i <= 16; i++) tide.lineTo(x + w * i / 16.0, y + h * 0.58 + rng.range(-6, 6));
            p.draw(tide);
        }
        if (wear.ring) {
            // a mug ring (or tears) where something wet was set down
            int rx = x + rng.range(w / 2, w - 70), ry = y + rng.range(10, h - 70);
            p.setStroke(new BasicStroke(2.2f));
            p.setColor(new Color(130, 96, 60, 80));
            p.draw(new Ellipse2D.Double(rx, ry, 56, 52));
            p.setStroke(new BasicStroke(1f));
            p.setColor(new Color(130, 96, 60, 40));
            p.draw(new Ellipse2D.Double(rx + 3, ry + 4, 50, 46));
        }
        if (wear.oil) {
            // machine oil soaked into the corner, dark and translucent
            int ox = x + (rng.chance(0.5) ? 10 : w - 120), oy = y + h - 80;
            blob(p, ox + 50, oy + 40, 70, new Color(70, 58, 24), 70, rng);
            blob(p, ox + 70, oy + 30, 36, new Color(50, 40, 16), 60, rng);
        }
        if (wear.coal) {
            // sooty fingerprints along the edges where it was held
            for (int i = 0; i < 2; i++) {
                int fx = i == 0 ? x + rng.range(10, 60) : x + w - rng.range(40, 80);
                int fy = band + rng.range(0, Math.max(1, y + h - band - 30));
                fingerprint(p, fx, fy, rng);
            }
        }
        if (wear.blood) {
            // dried blood: a drop with satellites, near a corner
            int bx = x + rng.range(w / 3, w - 70);
            int by = band + rng.range(2, Math.max(3, y + h - band - 22));
            p.setColor(new Color(92, 14, 14, 200));
            p.fill(new Ellipse2D.Double(bx, by, 16, 14));
            p.setColor(new Color(120, 20, 18, 170));
            for (int i = 0; i < 9; i++) {
                double a = rng.range(0, Math.PI * 2), r = rng.range(10, 26), s = rng.range(1.5, 4);
                p.fill(new Ellipse2D.Double(bx + 8 + Math.cos(a) * r, by + 7 + Math.sin(a) * r, s, s));
            }
            // a smear where a finger dragged through it
            p.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            p.setColor(new Color(100, 16, 14, 90));
            p.drawLine(bx + 8, by + 7, bx + 8 + rng.range(-50, 50), by + 7 + rng.range(4, 10));
        }
        p.setClip(null);
        p.setStroke(new BasicStroke(1f));
        p.setColor(new Color(120, 100, 76));
        p.draw(sheet);
    }

    private void drawRock(Graphics2D p, Shape sheet, int x, int y, int w, int h, Rng rng) {
        p.setColor(new Color(58, 54, 52));
        p.fill(sheet);
        p.setClip(sheet);
        for (int i = 0; i < 12; i++) blob(p, x + rng.range(0, w), y + rng.range(0, h), rng.range(30, 80), new Color(36, 32, 30), 70, rng);
        for (int i = 0; i < 6; i++) blob(p, x + rng.range(0, w), y + rng.range(0, h), rng.range(20, 50), new Color(86, 80, 74), 50, rng);
        p.setColor(new Color(24, 20, 18, 160));
        p.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 5; i++) {
            int cx = x + rng.range(0, w), cy = y + rng.range(0, h);
            p.drawLine(cx, cy, cx + rng.range(-60, 60), cy + rng.range(-30, 30));
        }
        p.setClip(null);
    }

    /** Chalk: the letters drawn twice, slightly offset and broken up, as if scraped on stone. */
    private void chalkLine(Graphics2D p, String line, int x, int y, Rng rng) {
        p.setColor(new Color(230, 226, 214, 110));
        p.drawString(line, x + 1, y + 1);
        p.setColor(new Color(236, 232, 222, 230));
        p.drawString(line, x, y);
        p.setColor(new Color(58, 54, 52, 120));
        FontMetrics fm = p.getFontMetrics();
        int len = fm.stringWidth(line);
        for (int i = 0; i < len / 6; i++) p.fillRect(x + rng.range(0, Math.max(1, len)), y - rng.range(0, 11), rng.range(1, 3), 1);
    }

    private static void blob(Graphics2D p, int cx, int cy, int r, Color c, int alpha, Rng rng) {
        p.setPaint(new RadialGradientPaint(cx, cy, r, new float[]{0f, 1f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
        Path2D s = new Path2D.Double();
        for (int i = 0; i <= 12; i++) {
            double a = Math.PI * 2 * i / 12, rr = r * rng.range(0.7, 1.0);
            if (i == 0) s.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            else s.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
        }
        s.closePath();
        p.fill(s);
    }

    private static void fingerprint(Graphics2D p, int x, int y, Rng rng) {
        double tilt = rng.range(-0.6, 0.6);
        Graphics2D f = (Graphics2D) p.create();
        f.rotate(tilt, x + 10, y + 13);
        f.setStroke(new BasicStroke(1.1f));
        for (int i = 0; i < 6; i++) {
            f.setColor(new Color(30, 26, 24, 90 - i * 10));
            f.draw(new Ellipse2D.Double(x + i * 1.5, y + i * 2, 20 - i * 3, 26 - i * 4));
        }
        f.setColor(new Color(30, 26, 24, 40));
        f.fill(new Ellipse2D.Double(x - 2, y - 2, 24, 30));
        f.dispose();
    }

    private static void bloodSpeckles(Graphics2D p, int x, int y, int h, Rng rng) {
        for (int i = 0; i < 6; i++) {
            p.setColor(new Color(110, 16, 16, rng.range(110, 180)));
            double s = rng.range(1.2, 2.6);
            p.fill(new Ellipse2D.Double(x + rng.range(0, W), y + rng.range(0, h), s, s));
        }
    }

    private static List<String> wrap(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics(FONT);
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                String test = cur.isEmpty() ? word : cur + " " + word;
                if (fm.stringWidth(test) > maxWidth && !cur.isEmpty()) {
                    out.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            out.add(cur.toString());
        }
        return out;
    }
}
