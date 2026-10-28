package hollowshift.entity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * One of the Tenant's tentacles: a chain of joints solved with FABRIK toward
 * its tip target every frame. A planted tip stays where it gripped the floor
 * while the body moves; a free tip writhes. Swings ease the tip to a new grip
 * and report the frame it lands.
 */
final class Tentacle {
    private static final int JOINTS = 9;
    private static final Color FLESH = new Color(62, 18, 22);
    private static final Color FLESH_WET = new Color(96, 30, 34);
    private static final Color OUTLINE = new Color(8, 2, 4);
    private static final Color BONE = new Color(214, 204, 186);

    final double segment;
    private final double thickness;
    final double baseAngle;     // where on the body rim it grows from (radians)
    private final double phase; // writhing offset
    private final double[] px = new double[JOINTS], py = new double[JOINTS];
    double tipX, tipY;          // tip target (world space)
    private double fromX, fromY, toX, toY;
    private double t = 1, duration = 1;
    private boolean lifted;
    boolean planted;

    Tentacle(double segment, double thickness, double baseAngle, double phase, double x, double y) {
        this.segment = segment;
        this.thickness = thickness;
        this.baseAngle = baseAngle;
        this.phase = phase;
        for (int i = 0; i < JOINTS; i++) {
            px[i] = x;
            py[i] = y;
        }
        tipX = fromX = toX = x;
        tipY = fromY = toY = y;
    }

    double reach() {
        return segment * (JOINTS - 1);
    }

    boolean moving() {
        return t < 1;
    }

    double endX() {
        return px[JOINTS - 1];
    }

    double endY() {
        return py[JOINTS - 1];
    }

    /** True when the grip is (nearly) out of reach of the base. */
    boolean stretched() {
        return Math.hypot(tipX - px[0], tipY - py[0]) > reach() * 0.95;
    }

    double distanceFromBase(double x, double y) {
        return Math.hypot(x - px[0], y - py[0]);
    }

    /** Swings the tip to (x, y) over the given time; it grips there when it lands. */
    void reachTo(double x, double y, double time, boolean lifted) {
        fromX = tipX;
        fromY = tipY;
        toX = x;
        toY = y;
        t = 0;
        duration = Math.max(0.04, time);
        this.lifted = lifted;
        planted = false;
    }

    void snapTo(double x, double y) {
        tipX = fromX = toX = x;
        tipY = fromY = toY = y;
        t = 1;
        planted = true;
    }

    /** Lets go: the tip will writhe near the body until it grips again. */
    void release() {
        planted = false;
        t = 1;
    }

    /** Advances the swing; returns true on the frame the tip lands. */
    boolean update(double dt, double time, double bodyX, double bodyY, double facing) {
        if (t < 1) {
            t = Math.min(1, t + dt / duration);
            double k = 1 - Math.pow(1 - t, 3); // fast out, soft landing
            tipX = fromX + (toX - fromX) * k;
            tipY = fromY + (toY - fromY) * k - (lifted ? Math.sin(Math.PI * t) * 8 : 0);
            if (t >= 1) {
                planted = true;
                return true;
            }
        } else if (!planted) {
            // writhing: curl around a point beside the body
            double a = baseAngle + facing + Math.sin(time * 2.1 + phase) * 0.8;
            double r = reach() * (0.45 + 0.2 * Math.sin(time * 1.7 + phase * 2));
            tipX = bodyX + Math.cos(a) * r;
            tipY = bodyY + Math.sin(a) * r * 0.7;
        }
        return false;
    }

    /** FABRIK: pull the chain toward the tip target, then re-anchor it at the base. */
    void solve(double bx, double by, double time) {
        double total = reach();
        double dx = tipX - bx, dy = tipY - by;
        double d = Math.hypot(dx, dy);
        if (d >= total) {
            for (int i = 0; i < JOINTS; i++) {
                px[i] = bx + dx / d * segment * i;
                py[i] = by + dy / d * segment * i;
            }
        } else {
            for (int iter = 0; iter < 3; iter++) {
                px[JOINTS - 1] = tipX;
                py[JOINTS - 1] = tipY;
                for (int i = JOINTS - 2; i >= 0; i--) pull(i, i + 1);
                px[0] = bx;
                py[0] = by;
                for (int i = 1; i < JOINTS; i++) pull(i, i - 1);
            }
        }
        // a slow travelling ripple so it never looks rigid
        for (int i = 1; i < JOINTS - 1; i++) {
            double w = Math.sin(time * 6 + phase + i * 0.9) * 0.9 * Math.sin(Math.PI * i / (JOINTS - 1));
            double nx = -(py[i + 1] - py[i - 1]), ny = px[i + 1] - px[i - 1];
            double nl = Math.hypot(nx, ny);
            if (nl > 0.001) {
                px[i] += nx / nl * w;
                py[i] += ny / nl * w;
            }
        }
    }

    private void pull(int i, int toward) {
        double dx = px[i] - px[toward], dy = py[i] - py[toward];
        double d = Math.max(0.0001, Math.hypot(dx, dy));
        px[i] = px[toward] + dx / d * segment;
        py[i] = py[toward] + dy / d * segment;
    }

    void render(Graphics2D g, int ox, int oy) {
        // outline pass, then flesh, tapering from base to tip
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < JOINTS - 1; i++) {
                float w = (float) (thickness - (thickness - 1.5) * i / (JOINTS - 1));
                g.setStroke(new BasicStroke(pass == 0 ? w + 2 : w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(pass == 0 ? OUTLINE : (i % 3 == 1 ? FLESH_WET : FLESH));
                g.drawLine((int) Math.round(px[i]) - ox, (int) Math.round(py[i]) - oy,
                        (int) Math.round(px[i + 1]) - ox, (int) Math.round(py[i + 1]) - oy);
            }
        }
        g.setStroke(new BasicStroke(1));
        // barbs of bone along the length
        g.setColor(BONE);
        for (int i = 2; i < JOINTS - 1; i += 2) {
            double nx = -(py[i + 1] - py[i - 1]), ny = px[i + 1] - px[i - 1];
            double nl = Math.max(0.001, Math.hypot(nx, ny));
            int bx = (int) Math.round(px[i] + nx / nl * thickness * 0.5) - ox;
            int by = (int) Math.round(py[i] + ny / nl * thickness * 0.5) - oy;
            g.drawLine(bx, by, bx + (int) Math.round(nx / nl * 2), by + (int) Math.round(ny / nl * 2));
        }
        // hooked bone claw at the tip
        int tx = (int) Math.round(px[JOINTS - 1]) - ox, ty = (int) Math.round(py[JOINTS - 1]) - oy;
        double dx = px[JOINTS - 1] - px[JOINTS - 2], dy = py[JOINTS - 1] - py[JOINTS - 2];
        double dl = Math.max(0.001, Math.hypot(dx, dy));
        g.drawLine(tx, ty, tx + (int) Math.round(dx / dl * 4 - dy / dl * 2), ty + (int) Math.round(dy / dl * 4 + dx / dl * 2));
    }
}
