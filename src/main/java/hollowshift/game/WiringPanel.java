package hollowshift.game;

import hollowshift.ai.Noise;
import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.gfx.Text;
import hollowshift.entity.JunctionBox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Rewiring a junction box: four wires hang loose on the left, and each goes to the terminal of
 * its own colour on the right. Pick a wire, then a terminal. The wrong pair shorts with a bang
 * and a shower of sparks that can be heard rooms away.
 */
public final class WiringPanel extends Panel {
    public static final double SHORT_NOISE = 300;
    public static final Color[] COLORS = {
            new Color(200, 50, 40), new Color(230, 200, 60), new Color(60, 110, 220), new Color(80, 190, 90)};
    private final JunctionBox box;
    /** right[i] = the colour of terminal i. */
    private final int[] right = new int[COLORS.length];
    private final boolean[] joined = new boolean[COLORS.length];
    private int cursor;
    /** The wire in hand, or -1 while picking one. */
    private int wire = -1;
    private double spark;
    private int sparkRow = -1;

    public WiringPanel(World world, JunctionBox box) {
        super(world, 190, 130);
        this.box = box;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < COLORS.length; i++) order.add(i);
        // never already in order
        do world.rng.shuffle(order);
        while (isIdentity(order));
        for (int i = 0; i < right.length; i++) right[i] = order.get(i);
    }

    private static boolean isIdentity(List<Integer> l) {
        for (int i = 0; i < l.size(); i++) if (l.get(i) != i) return false;
        return true;
    }

    @Override
    protected String title() {
        return Lang.t("panel.wires");
    }

    /** Which colour terminal i on the right takes. */
    public int terminal(int i) {
        return right[i];
    }

    /** Picks up wire w (the left column). */
    public void pick(int w) {
        if (joined[w]) return;
        wire = w;
        world.audio.play("grab", Bus.SFX, 0.6, 1.2);
    }

    /** Joins the wire in hand to terminal t (the right column). */
    public void join(int t) {
        if (wire < 0) return;
        if (right[t] == wire && !joined[wire]) {
            joined[wire] = true;
            world.audio.play("fuse_insert", Bus.SFX, 0.4, 1.6);
            wire = -1;
            for (boolean j : joined) if (!j) return;
            close();
            box.wire();
            return;
        }
        // a short: bang, sparks, and everything nearby hears it
        spark = 0.5;
        sparkRow = t;
        wire = -1;
        world.audio.playAt("fuse_insert", box.x, box.y, 700, 1, 0.6);
        world.camera.shakeFrom(box.x, box.y, 15, 0.25);
        world.emitNoise(new Noise(box.x, box.y, SHORT_NOISE, true));
        world.toast(Lang.t("toast.shorted"));
    }

    @Override
    public void update(Input in, double dt) {
        spark = Math.max(0, spark - dt);
        int n = COLORS.length;
        if (in.pressed(Action.UP)) cursor = (cursor + n - 1) % n;
        if (in.pressed(Action.DOWN)) cursor = (cursor + 1) % n;
        if (pressedAny(in, Action.INTERACT, Action.CONFIRM)) {
            if (wire < 0) pick(cursor);
            else join(cursor);
        }
    }

    private int rowY(int y, int i) {
        return y + 30 + i * 20;
    }

    @Override
    protected void draw(Graphics2D g, int x, int y) {
        int lx = x + 36, rx = x + w - 36;
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < COLORS.length; i++) {
            int ly = rowY(y, i);
            // wire stub on the left, terminal block on the right
            g.setColor(COLORS[i]);
            if (joined[i]) {
                int t = 0;
                while (right[t] != i) t++;
                g.drawLine(lx, ly, rx, rowY(y, t));
            } else if (i == wire) {
                // in hand: it follows the cursor across
                g.drawLine(lx, ly, rx - 14, rowY(y, cursor));
            } else {
                g.drawLine(lx - 8, ly, lx + 10, ly + 2);
            }
        }
        g.setStroke(old);
        for (int i = 0; i < COLORS.length; i++) {
            int ly = rowY(y, i);
            g.setColor(STEEL_DARK);
            g.fillRect(lx - 14, ly - 4, 8, 8);
            g.fillRect(rx, ly - 4, 10, 8);
            g.setColor(COLORS[right[i]]);
            g.fillRect(rx + 2, ly - 2, 6, 4);
            // notches, so the colours can be told apart without colour too
            g.setColor(STEEL_DARK);
            for (int k = 0; k <= right[i]; k++) g.fillRect(rx + 12 + k * 3, ly - 2, 2, 4);
            g.setColor(COLORS[i]);
            for (int k = 0; k <= i; k++) g.fillRect(lx - 19 - k * 3, ly - 2, 2, 4);
        }
        // the cursor: on the wires while picking, on the terminals with a wire in hand
        int cy = rowY(y, cursor);
        g.setColor(new Color(255, 214, 120));
        if (wire < 0) g.drawRect(lx - 16, cy - 6, 11, 11);
        else g.drawRect(rx - 2, cy - 6, 13, 11);
        if (spark > 0 && sparkRow >= 0) {
            g.setColor(new Color(255, 240, 160));
            int sy = rowY(y, sparkRow);
            for (int i = 0; i < 8; i++) {
                double a = i * 0.8 + spark * 20;
                g.fillRect(rx - 2 + (int) (Math.cos(a) * 8 * (1 - spark)), sy + (int) (Math.sin(a) * 8 * (1 - spark)), 1, 1);
            }
        }
        String key = Input.keyName(world.input().binding(Action.INTERACT)[0]);
        Text.draw(g, Lang.t(wire < 0 ? "panel.wiresPick" : "panel.wiresJoin", key), x + w / 2, y + h - 6, 8, DIM, Text.CENTER);
    }
}
