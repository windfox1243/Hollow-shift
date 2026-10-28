package hollowshift.engine;

public final class MathUtil {
    private MathUtil() {
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Frame-rate independent smoothing: fraction of the remaining distance kept after one second. */
    public static double damp(double a, double b, double keepPerSecond, double dt) {
        return lerp(a, b, 1 - Math.pow(keepPerSecond, dt));
    }

    public static double dist(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    public static double approach(double v, double target, double step) {
        return v < target ? Math.min(v + step, target) : Math.max(v - step, target);
    }
}
