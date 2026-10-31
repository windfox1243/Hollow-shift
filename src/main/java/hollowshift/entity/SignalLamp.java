package hollowshift.entity;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The little signal lamp on a machine that still needs work. The supply down here is bad, so it
 * does not blink cleanly: it blinks for a while, stutters, browns out, then comes back. It is
 * about as bright as the Tenant's eyes, a pin-prick in the black you can see across the room.
 * Once the job is done it settles to a dim steady green.
 */
public final class SignalLamp {
    private static final Color AMBER = new Color(255, 170, 50);
    private static final Color DONE = new Color(120, 255, 140, 150);

    private SignalLamp() {
    }

    /** 0..1: how lit the lamp is at time t. Each lamp has its own seed so they do not blink together. */
    public static double level(double t, long seed) {
        int span = (int) Math.floor(t / 1.4);
        double mood = hash(span, seed);
        if (mood < 0.25) {
            // browned out: dead, with the odd twitch
            return hash((int) Math.floor(t / 0.07), seed * 31) < 0.06 ? 0.5 : 0;
        }
        if (mood < 0.5) {
            // stuttering as the power comes and goes
            return hash((int) Math.floor(t / 0.08), seed * 17) < 0.55 ? 1 : 0;
        }
        // a plain blink, a little uneven
        double phase = (t + hash(span, seed * 7) * 0.3) % 0.7;
        return phase < 0.42 ? 0.85 + 0.15 * hash((int) Math.floor(t / 0.05), seed) : 0;
    }

    public static void render(Graphics2D g, double sx, double sy, boolean pending, double t, long seed) {
        int x = (int) Math.round(sx), y = (int) Math.round(sy);
        if (!pending) {
            g.setColor(DONE);
            g.fillRect(x, y, 1, 1);
            return;
        }
        double v = level(t, seed);
        if (v <= 0) {
            // an unlit bulb still catches a little light
            g.setColor(new Color(90, 50, 20, 120));
            g.fillRect(x, y, 2, 2);
            return;
        }
        // soft halo, then the hot core
        g.setColor(new Color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(), (int) (36 * v)));
        g.fillRect(x - 3, y - 1, 8, 4);
        g.fillRect(x - 1, y - 3, 4, 8);
        g.setColor(new Color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(), (int) (90 * v)));
        g.fillRect(x - 1, y, 4, 2);
        g.fillRect(x, y - 1, 2, 4);
        g.setColor(new Color(255, 236, 190, (int) (255 * v)));
        g.fillRect(x, y, 2, 2);
    }

    private static double hash(int n, long seed) {
        long h = n * 0x9E3779B97F4A7C15L + seed * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) / (double) (1L << 53);
    }
}
