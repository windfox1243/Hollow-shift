package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;
import hollowshift.rules.DeathCause;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * CONSUME: a nest of tentacles grown out of the rock. Step too close and they pin you to the
 * wall; mash Interact to tear free before they pull you in, while it hears and comes running.
 */
public final class WallMaw extends Entity {
    private static final double TRIGGER = 14;
    /** Seconds before the wall swallows a pinned player. */
    public static final double SWALLOW_TIME = 5;
    /** Struggle gained per press, and lost per second of not struggling. */
    private static final double PER_PRESS = 0.12, SLIP = 0.15;
    private static final double REGROW = 15;
    private static final int STRANDS = 7;

    /** Unit vector from the floor tile into the wall the nest grows from. */
    private final double wx, wy;
    private Player held;
    private double holdTime;
    private double struggle;
    private double regrow;
    private double t;
    private double alarmTimer;

    public WallMaw(double x, double y, double wallDirX, double wallDirY) {
        super(x, y);
        wx = wallDirX;
        wy = wallDirY;
        t = (x * 3 + y * 7) % 10;
    }

    @Override
    public void update(double dt) {
        t += dt;
        Player p = world.player;
        if (held != null) {
            holdTime += dt;
            struggle = Math.max(0, struggle - SLIP * dt);
            // pinned against the rock
            p.x = rootX() - wx * 7;
            p.y = rootY() - wy * 4 + 3;
            alarmTimer -= dt;
            if (alarmTimer <= 0) {
                alarmTimer = 1;
                for (Creature c : world.creatures()) c.alarm(p.x, p.y);
            }
            if (holdTime >= SWALLOW_TIME) world.kill(DeathCause.CONSUMED);
            return;
        }
        if (regrow > 0) {
            regrow -= dt;
            return;
        }
        if (!p.isFrozen() && p.distanceTo(rootX() - wx * 6, rootY() - wy * 6) < TRIGGER) seize(p);
    }

    private void seize(Player p) {
        held = p;
        holdTime = 0;
        struggle = 0;
        alarmTimer = 0;
        p.setTrappedBy(this);
        p.setFrozen(true);
        world.audio.play("grab", Bus.SFX, 1, 0.7);
        world.camera.shakeFrom(x, y, 4, 0.3);
        world.toast(Lang.t("toast.trapped", hollowshift.engine.Input.keyName(world.input().binding(hollowshift.engine.Action.INTERACT)[0])));
    }

    /** One press of Interact while pinned. */
    public void struggle() {
        if (held == null) return;
        struggle += PER_PRESS;
        world.audio.play("drop", Bus.SFX, 0.35, world.rng.range(0.7, 1.0));
        if (struggle >= 1) release();
    }

    private void release() {
        Player p = held;
        held = null;
        regrow = REGROW;
        p.setTrappedBy(null);
        p.setFrozen(false);
        p.x -= wx * 6;
        p.y -= wy * 6;
        world.audio.play("stun", Bus.SFX, 0.6, 1.3);
        world.toast(Lang.t("toast.tornFree"));
    }

    public double struggleProgress() {
        return struggle;
    }

    /** 0 when just caught, 1 when the wall takes you. */
    public double swallowProgress() {
        return Math.min(1, holdTime / SWALLOW_TIME);
    }

    private double rootX() {
        return x + wx * 8;
    }

    private double rootY() {
        return y + wy * 8;
    }

    @Override
    public double sortY() {
        return wy < 0 ? y - 8 : y;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        double rx = rootX() - ox, ry = rootY() - oy;
        // the growth itself: a dark swelling on the rock
        g.setColor(new Color(46, 12, 22));
        g.fillOval((int) rx - 7, (int) ry - 6, 14, 12);
        g.setColor(new Color(92, 26, 38));
        g.fillOval((int) rx - 4, (int) ry - 4, 8, 7);
        double reach;
        if (held != null) reach = 1;
        else if (regrow > 0) reach = 0.15;
        else reach = 0.35 + Math.sin(t * 2.1) * 0.08; // slowly tasting the air
        Graphics2D s = (Graphics2D) g.create();
        s.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < STRANDS; i++) {
            double spread = (i - (STRANDS - 1) / 2.0) / STRANDS;
            double ang = Math.atan2(-wy, -wx) + spread * 2.2;
            double len = (held != null ? 12 : 16) * reach + 4 + (i % 3) * 2;
            double ex = rx + Math.cos(ang) * len, ey = ry + Math.sin(ang) * len;
            if (held != null) {
                // wrapped round the victim
                ex = held.x - ox + Math.cos(t * 5 + i) * 4;
                ey = held.y - oy - 6 + Math.sin(t * 4 + i * 1.7) * 5;
            }
            double wob = Math.sin(t * 3 + i * 1.3) * 3;
            double mx = (rx + ex) / 2 - Math.sin(ang) * wob, my = (ry + ey) / 2 + Math.cos(ang) * wob;
            Path2D path = new Path2D.Double();
            path.moveTo(rx, ry);
            path.quadTo(mx, my, ex, ey);
            s.setStroke(new BasicStroke(i % 2 == 0 ? 2.2f : 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            s.setColor(new Color(70, 18, 30));
            s.draw(path);
            s.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            s.setColor(new Color(150, 60, 70));
            s.draw(path);
        }
        s.dispose();
    }
}
