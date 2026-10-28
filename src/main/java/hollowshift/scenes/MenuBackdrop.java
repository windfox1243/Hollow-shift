package hollowshift.scenes;

import hollowshift.engine.Game;
import hollowshift.engine.Rng;
import hollowshift.engine.Window;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.audio.Voice;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;

/** Shared animated background (dust in a lantern beam) and music for all menu screens. */
final class MenuBackdrop {
    private static final int DUST = 70;
    private static final double[][] dust = new double[DUST][3];
    private static Voice music;
    private static double t;

    static {
        Rng r = new Rng(3);
        for (double[] d : dust) {
            d[0] = r.range(0, Window.WIDTH);
            d[1] = r.range(0, Window.HEIGHT);
            d[2] = r.range(4, 14);
        }
    }

    private MenuBackdrop() {
    }

    static void startMusic(Game game) {
        if (music == null || music.isDone()) music = game.audio.loop("menu_drone", Bus.MUSIC, 0.7);
    }

    static void stopMusic() {
        if (music != null) music.stop();
        music = null;
    }

    static void update(double dt) {
        t += dt;
        for (double[] d : dust) {
            d[0] += Math.sin(t * 0.3 + d[2]) * dt * 4;
            d[1] -= d[2] * dt * 0.4;
            if (d[1] < -2) d[1] = Window.HEIGHT + 2;
        }
    }

    static void render(Graphics2D g) {
        g.setColor(new Color(8, 7, 11));
        g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
        float cx = Window.WIDTH / 2f, cy = Window.HEIGHT * 0.38f;
        float flick = (float) (1 + Math.sin(t * 13) * 0.02 + Math.sin(t * 3.1) * 0.03);
        g.setPaint(new RadialGradientPaint(cx, cy, 230 * flick, new float[]{0f, 1f},
                new Color[]{new Color(70, 52, 30, 120), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
        for (double[] d : dust) {
            double dist = Math.hypot(d[0] - cx, d[1] - cy);
            int a = (int) Math.max(0, 170 - dist * 0.7);
            if (a <= 0) continue;
            g.setColor(new Color(255, 220, 170, a));
            g.fillRect((int) d[0], (int) d[1], 1, 1);
        }
    }
}
