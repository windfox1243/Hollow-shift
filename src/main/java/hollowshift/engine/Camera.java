package hollowshift.engine;

/** World-space camera centred on (x, y) with smoothing and distance-attenuated shake. */
public final class Camera {
    public double x;
    public double y;
    private double shakeMagnitude;
    private double shakeTime;
    private double offX;
    private double offY;
    private final Rng rng = new Rng(7);

    // the followed point; the view is anchored to it at whole pixels (see left())
    private double targetX, targetY;

    public void snapTo(double tx, double ty) {
        x = targetX = tx;
        y = targetY = ty;
    }

    public void follow(double tx, double ty, double dt) {
        targetX = tx;
        targetY = ty;
        x = MathUtil.damp(x, tx, 0.004, dt);
        y = MathUtil.damp(y, ty, 0.004, dt);
    }

    /** Shake whose strength falls off with distance from the camera centre. */
    public void shakeFrom(double sx, double sy, double magnitude, double duration) {
        double d = Math.max(1, MathUtil.dist(x, y, sx, sy));
        double m = Math.min(magnitude / d * 10, 8);
        if (m < 0.5) return;
        shakeMagnitude = Math.max(shakeMagnitude, m);
        shakeTime = Math.max(shakeTime, duration);
    }

    public void update(double dt) {
        if (shakeTime > 0) {
            shakeTime -= dt;
            offX = rng.range(-shakeMagnitude, shakeMagnitude);
            offY = rng.range(-shakeMagnitude, shakeMagnitude);
            shakeMagnitude *= Math.pow(0.02, dt);
        } else {
            offX = offY = 0;
            shakeMagnitude = 0;
        }
    }

    /**
     * Left edge of the view in world pixels. It is the rounded target plus the
     * rounded smoothing lag, so the followed entity (drawn at its rounded position)
     * keeps a fixed screen position frame to frame instead of jittering by a pixel.
     */
    public int left() {
        return (int) Math.round(targetX) + (int) Math.round(x - targetX + offX) - Window.WIDTH / 2;
    }

    public int top() {
        return (int) Math.round(targetY) + (int) Math.round(y - targetY + offY) - Window.HEIGHT / 2;
    }

    public boolean visible(double wx, double wy, double margin) {
        return wx > left() - margin && wx < left() + Window.WIDTH + margin
                && wy > top() - margin && wy < top() + Window.HEIGHT + margin;
    }
}
