package hollowshift.game;

import hollowshift.engine.Rng;
import hollowshift.engine.Window;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Screen effects while the Tenant is close: a red vignette that throbs with the
 * heartbeat, then (very close) the picture starts to flicker and tear.
 */
public final class FearOverlay {
    private static final int W = Window.WIDTH, H = Window.HEIGHT;
    private final BufferedImage vignette = buildVignette();
    private final Rng rng = new Rng(99);

    public void render(Graphics2D g, World w) {
        double fear = w.fear();
        if (fear < 0.02) return;
        double pulse = w.heartPulse();
        Composite old = g.getComposite();
        float a = (float) Math.min(1, fear * (0.45 + 0.55 * pulse));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        // the vignette tightens on each beat
        int grow = (int) (pulse * 24 * fear);
        g.drawImage(vignette, -grow, -grow, W + grow * 2, H + grow * 2, null);
        g.setComposite(old);

        if (fear > 0.55) {
            double k = (fear - 0.55) / 0.45;
            // brief blackouts, as if your vision drops out
            if (rng.chance(0.05 + 0.12 * k)) {
                g.setColor(new Color(0, 0, 0, (int) (90 + 120 * k * rng.next())));
                g.fillRect(0, 0, W, H);
            }
            // torn scanlines
            int lines = (int) (k * 6);
            for (int i = 0; i < lines; i++) {
                int y = rng.range(0, H);
                g.setColor(rng.chance(0.5) ? new Color(0, 0, 0, 160) : new Color(140, 10, 10, 90));
                g.fillRect(0, y, W, rng.range(1, 3));
            }
        }
    }

    /** Red at the edges fading to clear in the middle. */
    private static BufferedImage buildVignette() {
        BufferedImage img = new BufferedImage(W / 4, H / 4, BufferedImage.TYPE_INT_ARGB);
        double cx = img.getWidth() / 2.0, cy = img.getHeight() / 2.0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                double dx = (x + 0.5 - cx) / cx, dy = (y + 0.5 - cy) / cy;
                double d = Math.sqrt(dx * dx * 0.8 + dy * dy);
                double t = Math.max(0, Math.min(1, (d - 0.45) / 0.7));
                int alpha = (int) (t * t * 235);
                img.setRGB(x, y, alpha << 24 | 110 << 16 | 4 << 8 | 8);
            }
        }
        return img;
    }
}
