package hollowshift.game;

import hollowshift.ai.Noise;
import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.gfx.Text;
import hollowshift.entity.Creature;
import hollowshift.entity.Valve;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Bleeding a valve: the pressure needle swings, and the wheel has to be turned while it is in
 * the green. Three turns let the pressure off. Turn it at the wrong moment and the relief
 * whistle howls through the tunnels, which brings the Tenant.
 */
public final class GaugePanel extends Panel {
    public static final int TURNS = 3;
    /** Width of the green band, as a fraction of the dial. */
    public static final double BAND = 0.2;
    private static final double[] SPEED = {0.75, 1.0, 1.3};
    private final Valve valve;
    private double needle, dir = 1;
    private double band;
    private int turns;
    private double flash, good;

    public GaugePanel(World world, Valve valve) {
        super(world, 210, 130);
        this.valve = valve;
        newBand();
    }

    @Override
    protected String title() {
        return Lang.t("panel.valve");
    }

    private void newBand() {
        // never right where the needle is now, so each turn needs a fresh wait
        do band = world.rng.range(0.08, 0.92 - BAND);
        while (Math.abs(band + BAND / 2 - needle) < 0.25);
    }

    public double needle() {
        return needle;
    }

    public double band() {
        return band;
    }

    public int turns() {
        return turns;
    }

    /** Turns the wheel now. */
    public void turn() {
        if (turns >= TURNS) return;
        if (needle >= band && needle <= band + BAND) {
            turns++;
            good = 0.3;
            world.audio.play("drop", Bus.SFX, 0.7, 1.4);
            world.audio.playAt("flare", valve.x, valve.y, 300, 0.4, 1.6);
            world.emitNoise(new Noise(valve.x, valve.y, 120, true));
            if (turns >= TURNS) {
                close();
                valve.bleed();
            } else {
                newBand();
            }
            return;
        }
        // the relief whistle: a howl through every tunnel, and it comes to look
        flash = 0.6;
        world.audio.playAt("surge", valve.x, valve.y, 1400, 1, 2.0);
        world.audio.playAt("flare", valve.x, valve.y, 600, 0.8, 0.7);
        world.camera.shakeFrom(valve.x, valve.y, 25, 0.35);
        for (Creature c : world.creatures()) c.summon(valve.x, valve.y);
        world.toast(Lang.t("toast.valveHowl"));
    }

    @Override
    public void update(Input in, double dt) {
        flash = Math.max(0, flash - dt);
        good = Math.max(0, good - dt);
        needle += dir * SPEED[Math.min(turns, SPEED.length - 1)] * dt;
        if (needle > 1) {
            needle = 2 - needle;
            dir = -1;
        } else if (needle < 0) {
            needle = -needle;
            dir = 1;
        }
        if (pressedAny(in, Action.INTERACT, Action.CONFIRM)) turn();
    }

    @Override
    protected void draw(Graphics2D g, int x, int y) {
        int cx = x + w / 2, cy = y + 90, r = 56;
        // the dial face: a half circle with its green band
        g.setColor(new Color(200, 190, 160));
        g.fillArc(cx - r, cy - r, r * 2, r * 2, 0, 180);
        g.setColor(good > 0 ? new Color(160, 255, 170) : new Color(70, 160, 80));
        int from = (int) Math.round(180 - (band + BAND) * 180);
        g.fillArc(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8, from, (int) Math.round(BAND * 180));
        g.setColor(new Color(200, 190, 160));
        g.fillArc(cx - r + 14, cy - r + 14, r * 2 - 28, r * 2 - 28, 0, 180);
        g.setColor(STEEL_DARK);
        for (int i = 0; i <= 10; i++) {
            double a = Math.PI - i * Math.PI / 10;
            g.drawLine(cx + (int) (Math.cos(a) * (r - 14)), cy - (int) (Math.sin(a) * (r - 14)),
                    cx + (int) (Math.cos(a) * (r - 19)), cy - (int) (Math.sin(a) * (r - 19)));
        }
        // the needle
        double a = Math.PI - needle * Math.PI;
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2));
        g.setColor(flash > 0 ? new Color(230, 40, 30) : STEEL_DARK);
        g.drawLine(cx, cy, cx + (int) (Math.cos(a) * (r - 8)), cy - (int) (Math.sin(a) * (r - 8)));
        g.setStroke(old);
        g.setColor(STEEL_DARK);
        g.fillRect(cx - 3, cy - 3, 6, 6);
        // how many turns done
        for (int i = 0; i < TURNS; i++) {
            g.setColor(i < turns ? new Color(120, 255, 140) : STEEL_DARK);
            g.fillRect(cx - 16 + i * 12, cy + 10, 8, 6);
        }
        String key = Input.keyName(world.input().binding(Action.INTERACT)[0]);
        Text.draw(g, Lang.t("panel.valveHint", key), cx, y + h - 6, 8, DIM, Text.CENTER);
    }
}
