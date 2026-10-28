package hollowshift.engine.gfx;

import hollowshift.engine.Window;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Darkness overlay, computed directly on int arrays (Java2D's software alpha
 * compositing and bilinear scaling were the most expensive part of a frame).
 * <ol>
 *   <li>A half-resolution darkness buffer starts fully dark; every light scales
 *       down the darkness inside its radius by a falloff curve.</li>
 *   <li>Tinted lights add a coloured glow in a second half-resolution buffer.</li>
 *   <li>Both are upsampled 2x (bilinear, fixed weights) into one ARGB overlay
 *       that is blitted over the scene with a single unscaled drawImage.</li>
 * </ol>
 */
public final class Lighting {
    private static final int W = Window.WIDTH, H = Window.HEIGHT;
    private static final int HW = W / 2, HH = H / 2;
    private static final int TABLE = 1024;
    /** Hole falloff indexed by squared normalised distance: flat-ish core, soft rim. */
    private static final int[] HOLE = new int[TABLE];
    /** Glow falloff (linear in distance), 0..256. */
    private static final int[] GLOW = new int[TABLE];

    static {
        for (int i = 0; i < TABLE; i++) {
            double d = Math.sqrt(i / (double) TABLE);
            double hole = d < 0.45 ? 1 - d : 0.55 * (1 - (d - 0.45) / 0.55);
            HOLE[i] = (int) Math.round(Math.max(0, hole) * 256);
            GLOW[i] = (int) Math.round(Math.max(0, 1 - d) * 256);
        }
    }

    private final int[] dark = new int[HW * HH];      // 0..255 darkness alpha
    private final float[] glowA = new float[HW * HH];  // 0..1
    private final float[] glowR = new float[HW * HH];
    private final float[] glowG = new float[HW * HH];
    private final float[] glowB = new float[HW * HH];
    private final BufferedImage overlay = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    private final int[] out = ((DataBufferInt) overlay.getRaster().getDataBuffer()).getData();
    // upsampling: output pixel x reads half-res columns x0[x], x0[x]+1 with weight wx[x] (0..256) on the second
    private final int[] x0 = new int[W], wx = new int[W], y0 = new int[H], wy = new int[H];
    private boolean anyGlow;

    public Lighting() {
        for (int x = 0; x < W; x++) {
            double s = (x + 0.5) / 2 - 0.5;
            int i = (int) Math.floor(s);
            double t = s - i;
            if (i < 0) {
                i = 0;
                t = 0;
            }
            if (i >= HW - 1) {
                i = HW - 2;
                t = 1;
            }
            x0[x] = i;
            wx[x] = (int) Math.round(t * 256);
        }
        for (int y = 0; y < H; y++) {
            double s = (y + 0.5) / 2 - 0.5;
            int i = (int) Math.floor(s);
            double t = s - i;
            if (i < 0) {
                i = 0;
                t = 0;
            }
            if (i >= HH - 1) {
                i = HH - 2;
                t = 1;
            }
            y0[y] = i;
            wy[y] = (int) Math.round(t * 256);
        }
    }

    /** Starts a frame with the given ambient darkness (0 = fully lit, 1 = pitch black). */
    public void begin(double darkness) {
        java.util.Arrays.fill(dark, (int) Math.round(255 * darkness));
        if (anyGlow) {
            java.util.Arrays.fill(glowA, 0);
            java.util.Arrays.fill(glowR, 0);
            java.util.Arrays.fill(glowG, 0);
            java.util.Arrays.fill(glowB, 0);
        }
        anyGlow = false;
    }

    /** Adds a light at screen coordinates. intensity 0..1; tint may be null for neutral light. */
    public void light(double sx, double sy, double radius, double intensity, Color tint) {
        if (radius < 2 || intensity <= 0.01) return;
        double cx = sx / 2, cy = sy / 2, r = radius / 2;
        int xa = Math.max(0, (int) Math.floor(cx - r)), xb = Math.min(HW - 1, (int) Math.ceil(cx + r));
        int ya = Math.max(0, (int) Math.floor(cy - r)), yb = Math.min(HH - 1, (int) Math.ceil(cy + r));
        if (xa > xb || ya > yb) return;
        int k = (int) Math.round(Math.min(1, intensity) * 256);
        double inv = TABLE / (r * r);
        float ta = 0, tr = 0, tg = 0, tb = 0;
        if (tint != null) {
            ta = (float) (tint.getAlpha() / 255.0 * Math.min(1, intensity));
            tr = tint.getRed();
            tg = tint.getGreen();
            tb = tint.getBlue();
            anyGlow = true;
        }
        for (int y = ya; y <= yb; y++) {
            double dy = y + 0.5 - cy;
            double dy2 = dy * dy;
            int row = y * HW;
            for (int x = xa; x <= xb; x++) {
                double dx = x + 0.5 - cx;
                int idx = (int) ((dx * dx + dy2) * inv);
                if (idx >= TABLE) continue;
                int i = row + x;
                // darkness *= 1 - intensity * falloff
                dark[i] -= (dark[i] * ((k * HOLE[idx]) >> 8)) >> 8;
                if (tint != null) {
                    float ga = ta * GLOW[idx] / 256f;
                    if (ga <= 0) continue;
                    float a0 = glowA[i];
                    float na = a0 + ga * (1 - a0);
                    float w = ga * (1 - a0) / na;
                    glowR[i] += (tr - glowR[i]) * w;
                    glowG[i] += (tg - glowG[i]) * w;
                    glowB[i] += (tb - glowB[i]) * w;
                    glowA[i] = na;
                }
            }
        }
    }

    /** Composites glow then darkness into one overlay and draws it. */
    public void end(Graphics2D g) {
        for (int y = 0; y < H; y++) {
            int r0 = y0[y] * HW, r1 = r0 + HW, fy = wy[y], iy = 256 - fy;
            int o = y * W;
            for (int x = 0; x < W; x++) {
                int c0 = x0[x], fx = wx[x], ix = 256 - fx;
                int a = (((dark[r0 + c0] * ix + dark[r0 + c0 + 1] * fx) >> 8) * iy
                        + ((dark[r1 + c0] * ix + dark[r1 + c0 + 1] * fx) >> 8) * fy) >> 8;
                if (!anyGlow) {
                    out[o + x] = a << 24;
                    continue;
                }
                int p00 = r0 + c0, p01 = p00 + 1, p10 = r1 + c0, p11 = p10 + 1;
                float bx = fx / 256f, by = fy / 256f;
                float w00 = (1 - bx) * (1 - by), w01 = bx * (1 - by), w10 = (1 - bx) * by, w11 = bx * by;
                float ga = glowA[p00] * w00 + glowA[p01] * w01 + glowA[p10] * w10 + glowA[p11] * w11;
                if (ga < 0.004f) {
                    out[o + x] = a << 24;
                    continue;
                }
                // glow under the darkness: out = S*(1-A) + K*A
                float ad = a / 255f;
                float gw = ga * (1 - ad);
                float total = gw + ad;
                float gr = (glowR[p00] * glowA[p00] * w00 + glowR[p01] * glowA[p01] * w01
                        + glowR[p10] * glowA[p10] * w10 + glowR[p11] * glowA[p11] * w11) / ga;
                float gg = (glowG[p00] * glowA[p00] * w00 + glowG[p01] * glowA[p01] * w01
                        + glowG[p10] * glowA[p10] * w10 + glowG[p11] * glowA[p11] * w11) / ga;
                float gb = (glowB[p00] * glowA[p00] * w00 + glowB[p01] * glowA[p01] * w01
                        + glowB[p10] * glowA[p10] * w10 + glowB[p11] * glowA[p11] * w11) / ga;
                float m = gw / total; // share of the colour coming from the glow (rest is black)
                int A = Math.min(255, (int) (total * 255));
                out[o + x] = A << 24 | (int) (gr * m) << 16 | (int) (gg * m) << 8 | (int) (gb * m);
            }
        }
        g.drawImage(overlay, 0, 0, null);
    }
}
