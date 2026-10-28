package hollowshift.scenes;

import hollowshift.engine.Rng;
import hollowshift.engine.Window;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Full-screen scare when the Tenant catches the player.
 * <pre>
 * 0.00-0.14  near silence: its face hangs far away in the dark (anticipation)
 * 0.14       SNAP: the face fills the screen, red/white flash frames
 * 0.14-1.25  jaw unhinges, eyes flare, fingers grip the screen edges,
 *            image shakes with RGB split, grain and scanline tears
 * 1.25-1.60  the picture glitches apart into black
 * </pre>
 * Drawn at 1/4 resolution and post-processed on the pixel array, then scaled
 * up with nearest neighbour so it matches the pixel-art look.
 */
final class Jumpscare {
    // long enough for the whole jumpscare recording (2.6 s, loud for the first ~1.5 s)
    static final double DURATION = 2.0;
    static final double SNAP = 0.14;
    private static final int W = Window.WIDTH / 4, H = Window.HEIGHT / 4;
    private static final Color SKIN = new Color(214, 206, 194);
    private static final Color SKIN_MID = new Color(170, 158, 148);
    private static final Color SKIN_DARK = new Color(112, 100, 96);
    private static final Color PIT = new Color(10, 4, 6);
    private static final Color GUM = new Color(120, 18, 26);

    private final BufferedImage face = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage post = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
    private final int[] src = ((DataBufferInt) face.getRaster().getDataBuffer()).getData();
    private final int[] dst = ((DataBufferInt) post.getRaster().getDataBuffer()).getData();
    private final Rng rng = new Rng(13);

    void render(Graphics2D out, double t) {
        Graphics2D g = face.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        if (t < SNAP) {
            // far away, barely lit, perfectly still
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            head(g, W / 2.0, H / 2.0, 0.22, 0, 0.6);
            g.dispose();
            blit(out, src);
            return;
        }
        double k = t - SNAP;
        double scale = 0.95 + Math.min(0.3, k * 0.6) + Math.sin(k * 47) * 0.015;
        double shake = k < 0.5 ? 4 : 1.5;
        double cx = W / 2.0 + rng.range(-shake, shake);
        double cy = H / 2.0 + 4 + rng.range(-shake, shake);
        double jaw = Math.min(1, k / 0.35);
        head(g, cx, cy, scale, jaw, 1);
        fingers(g, t, -1);
        fingers(g, t, 1);
        // flash frames right after the snap
        if (k < 0.035) {
            g.setColor(new Color(255, 255, 255, 200));
            g.fillRect(0, 0, W, H);
        } else if (k < 0.09) {
            g.setColor(new Color(170, 0, 0, 150));
            g.fillRect(0, 0, W, H);
        }
        g.dispose();
        postProcess(t, k);
        blit(out, dst);
    }

    private void blit(Graphics2D out, int[] pixels) {
        BufferedImage img = pixels == src ? face : post;
        out.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        out.drawImage(img, 0, 0, Window.WIDTH, Window.HEIGHT, null);
    }

    /** RGB split, grain, scanline tears and the final glitch-out, all on the pixel array. */
    private void postProcess(double t, double k) {
        int split = k < 0.3 ? 3 : 1 + (int) (Math.abs(Math.sin(t * 31)) * 2);
        double fade = t > DURATION - 0.35 ? (t - (DURATION - 0.35)) / 0.35 : 0;
        for (int y = 0; y < H; y++) {
            // occasional torn rows that slide sideways
            int tear = rng.chance(0.06 + fade * 0.4) ? rng.range(-12, 12) : 0;
            boolean kill = fade > 0 && rng.chance(fade);
            for (int x = 0; x < W; x++) {
                if (kill) {
                    dst[y * W + x] = 0;
                    continue;
                }
                int sx = x + tear;
                int r = (pixel(sx + split, y) >> 16) & 0xff;
                int gch = (pixel(sx, y) >> 8) & 0xff;
                int b = pixel(sx - split, y) & 0xff;
                int n = rng.range(-22, 22);
                // scanline darkening
                int dim = (y & 1) == 0 ? 0 : 18;
                r = clamp(r + n - dim);
                gch = clamp(gch + n - dim);
                b = clamp(b + n - dim);
                dst[y * W + x] = r << 16 | gch << 8 | b;
            }
        }
    }

    private int pixel(int x, int y) {
        if (x < 0 || x >= W) return 0;
        return src[y * W + x];
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    /**
     * The Tenant up close: a lamprey maw that fills the screen. Wet black flesh,
     * concentric rings of hooked teeth spiralling down a red throat, and a cluster
     * of pin-prick eyes above it. jaw 0..1 is how far the maw has dilated.
     */
    private void head(Graphics2D g, double cx, double cy, double s, double jaw, double eyeGlow) {
        double r = 52 * s;
        // the flesh around the maw: lumpy, glistening
        g.setPaint(new RadialGradientPaint((float) cx, (float) (cy - r * 0.2), (float) (r * 1.25),
                new float[]{0f, 0.6f, 1f}, new Color[]{new Color(150, 46, 46), new Color(80, 18, 22), new Color(22, 4, 8)}));
        Path2D lump = new Path2D.Double();
        for (int i = 0; i <= 40; i++) {
            double a = Math.PI * 2 * i / 40;
            double rr = r * 1.2 * (1 + 0.08 * Math.sin(a * 5 + 1.3) + 0.05 * Math.sin(a * 9));
            double px = cx + Math.cos(a) * rr, py = cy + Math.sin(a) * rr * 0.85;
            if (i == 0) lump.moveTo(px, py);
            else lump.lineTo(px, py);
        }
        g.fill(lump);
        // glistening highlights on the wet skin
        g.setColor(new Color(170, 90, 90, 110));
        for (int i = 0; i < 6; i++) {
            double a = i * 1.1 + 0.4;
            double hx = cx + Math.cos(a) * r * 1.0, hy = cy + Math.sin(a) * r * 0.8;
            g.fillRect((int) hx, (int) hy, (int) Math.max(1, 2 * s), 1);
        }
        // the maw dilates: gum ring, then rings of teeth down into the throat
        double open = 0.55 + 0.45 * jaw;
        double mr = r * 0.78 * open;
        g.setColor(new Color(120, 16, 24));
        g.fill(new Ellipse2D.Double(cx - mr * 1.08, cy - mr * 0.95, mr * 2.16, mr * 1.9));
        int rings = 4;
        for (int ring = 0; ring < rings; ring++) {
            double rr = mr * (1 - ring * 0.22);
            // each ring a little darker, then the black of the throat
            // raw gum between the rings, darker the deeper it goes
            int shade = Math.max(0, 150 - ring * 34);
            g.setColor(new Color(shade, shade / 6, shade / 6 + 6));
            g.fill(new Ellipse2D.Double(cx - rr, cy - rr * 0.88, rr * 2, rr * 1.76));
            int teeth = 13 - ring * 2;
            double spin = ring * 0.35 + jaw * 0.3;
            for (int i = 0; i < teeth; i++) {
                // irregular: uneven spacing, length and colour, some broken short
                int h = (i * 37 + ring * 11) % 7;
                double a = Math.PI * 2 * (i + (h - 3) * 0.12) / teeth + spin;
                double len = rr * (0.14 + 0.05 * h);
                int c = 230 - ring * 26 - h * 6;
                g.setColor(new Color(c, Math.max(0, c - 14), Math.max(0, c - 40)));
                double bx = cx + Math.cos(a) * rr, by = cy + Math.sin(a) * rr * 0.88;
                // hooked: the tip bends sideways as it points inward
                double tx = cx + Math.cos(a + 0.18) * (rr - len), ty = cy + Math.sin(a + 0.18) * (rr - len) * 0.88;
                double w = Math.max(1, rr * (0.025 + 0.008 * h));
                double nx = -Math.sin(a) * w, ny = Math.cos(a) * w;
                g.fill(triangle(bx + nx, by + ny, bx - nx, by - ny, tx, ty));
            }
        }
        // the throat
        double tr = mr * 0.22;
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (tr * 2),
                new float[]{0f, 1f}, new Color[]{Color.BLACK, new Color(30, 2, 6)}));
        g.fill(new Ellipse2D.Double(cx - tr * 2, cy - tr * 1.8, tr * 4, tr * 3.6));
        // a crown of pin-prick eyes above the maw
        int a8 = (int) (255 * Math.min(1, eyeGlow));
        for (int i = 0; i < 11; i++) {
            double a = -Math.PI / 2 + (i - 5) * 0.27;
            double er = r * (0.95 + (i % 3) * 0.06);
            double ex = cx + Math.cos(a) * er * 1.05, ey = cy + Math.sin(a) * er * 0.62;
            double size = Math.max(1, (i % 4 == 0 ? 2.4 : 1.4) * s);
            g.setColor(new Color(255, 40, 30, a8 / 3));
            g.fill(new Ellipse2D.Double(ex - size * 1.5, ey - size * 1.5, size * 3, size * 3));
            g.setColor(new Color(255, 220, 160, a8));
            g.fill(new Ellipse2D.Double(ex - size / 2, ey - size / 2, size, size));
        }
    }

    /** Tentacles lashing in from the screen corners toward the camera. */
    private void fingers(Graphics2D g, double t, int side) {
        double k = Math.min(1, (t - SNAP) / 0.12);
        for (int f = 0; f < 2; f++) {
            double edgeX = side < 0 ? -6 : W + 6;
            double edgeY = f == 0 ? -6 : H + 6;
            double len = (48 + f * 8) * k;
            int segs = 12;
            double[] xs = new double[segs + 1], ys = new double[segs + 1];
            double dirX = -side, dirY = f == 0 ? 0.7 : -0.7;
            double dl = Math.hypot(dirX, dirY);
            dirX /= dl;
            dirY /= dl;
            for (int i = 0; i <= segs; i++) {
                double u = i / (double) segs;
                double wave = Math.sin(u * 6 + t * 16 + f * 2 + side) * 7 * u;
                xs[i] = edgeX + dirX * len * u - dirY * wave;
                ys[i] = edgeY + dirY * len * u + dirX * wave;
            }
            // one continuous outline, then the flesh on top, tapering to the tip
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < segs; i++) {
                    float w = (float) (7 * (1 - i / (double) segs) + 1.2);
                    g.setStroke(new BasicStroke(pass == 0 ? w + 2 : w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(pass == 0 ? new Color(8, 2, 4) : new Color(118, 36, 40));
                    g.drawLine((int) xs[i], (int) ys[i], (int) xs[i + 1], (int) ys[i + 1]);
                }
            }
            g.setColor(new Color(220, 210, 192));
            g.fillRect((int) xs[segs] - 1, (int) ys[segs] - 1, 3, 2); // bone hook
        }
        g.setStroke(new BasicStroke(1));
    }

    private static Shape triangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.closePath();
        return p;
    }
}
