package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * CONSUME: a knot of its flesh on the floor, part of the Tenant. Treading on it does not
 * hurt; it just tells the Tenant exactly where you are, and it comes.
 */
public final class FleshNode extends Entity {
    private static final double TRIGGER = 9;
    private static final double REST = 12;
    private double rest;
    private double t;
    private double flinch;

    public FleshNode(double x, double y) {
        super(x, y);
        t = (x * 5 + y * 11) % 7;
    }

    @Override
    public void update(double dt) {
        t += dt;
        flinch = Math.max(0, flinch - dt * 2);
        if (rest > 0) {
            rest -= dt;
            return;
        }
        Player p = world.player;
        if (p.distanceTo(x, y) < TRIGGER) {
            rest = REST;
            flinch = 1;
            world.audio.play("grab", Bus.SFX, 0.9, 0.5);
            world.audio.playAt("creature_step_heavy", x, y, 400, 0.7, 1.4);
            for (Creature c : world.creatures()) c.alarm(p.x, p.y);
            world.toast(Lang.t("toast.nodeTouched"));
        }
    }

    @Override
    public double sortY() {
        return y - 6; // lies flat, drawn under anyone standing on it
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        int sx = (int) Math.round(x) - ox, sy = (int) Math.round(y) - oy;
        double beat = 1 + Math.max(0, Math.sin(t * 2.4)) * 0.12 + flinch * 0.4;
        int w = (int) (12 * beat), h = (int) (7 * beat);
        g.setColor(new Color(30, 8, 14));
        g.fillOval(sx - w / 2 - 1, sy - h / 2, w + 2, h + 1);
        g.setColor(new Color(84, 22, 34));
        g.fillOval(sx - w / 2, sy - h / 2, w, h);
        g.setColor(new Color(140, 52, 62));
        g.fillOval(sx - 2, sy - 2, 3, 2);
        g.fillOval(sx + 2, sy, 2, 2);
        // a few roots spreading into the floor
        g.setColor(new Color(60, 16, 26));
        for (int i = 0; i < 4; i++) {
            double a = i * 1.7 + 0.4;
            g.drawLine(sx, sy, sx + (int) (Math.cos(a) * 9), sy + (int) (Math.sin(a) * 5));
        }
    }
}
