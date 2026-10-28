package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.audio.Voice;
import hollowshift.engine.gfx.Text;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Story endings. Riding up: the lift climbs while the epilogue types out.
 * Staying below (hidden): a dark tunnel where a new worker's lantern comes down,
 * and from inside the rock you knock three times.
 */
public final class EndingScene extends Scene {
    private final String text;
    private final boolean stayed;
    private Voice hum;
    private double t;
    private double knockTimer = 2;

    public EndingScene(Game game, boolean stayed) {
        super(game);
        this.stayed = stayed;
        text = stayed ? Lang.t("ending.stay") : game.campaign.ending();
    }

    @Override
    public void enter() {
        hum = game.audio.loop(stayed ? "drone" : "elevator", Bus.MUSIC, 0.5);
    }

    @Override
    public void exit() {
        if (hum != null) hum.stop();
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (stayed && (knockTimer -= dt) <= 0) {
            game.audio.play("knock", Bus.SFX, 0.8, 1);
            knockTimer = 7;
        }
        boolean done = t * 40 > text.length() + 40;
        if (game.input.pressed(Action.CONFIRM) && !done) t = (text.length() + 40) / 40.0;
        else if (done && (game.input.pressed(Action.CONFIRM) || game.input.pressed(Action.BACK))) {
            game.scenes.set(new CreditsScene(game, true));
        }
    }

    @Override
    public void render(Graphics2D g) {
        if (stayed) renderTunnel(g);
        else renderShaft(g);
        int shown = (int) Math.min(text.length(), t * 40);
        Text.drawWrapped(g, text.substring(0, shown), Window.WIDTH / 2, stayed ? 36 : 90, 440, 11, new Color(210, 202, 188), Text.CENTER);
        if (t * 40 > text.length() + 40) {
            Text.draw(g, Lang.t("ending.theend"), Window.WIDTH / 2, Window.HEIGHT - 40, 18, new Color(236, 214, 170), Text.CENTER);
        }
    }

    /** Shaft walls sliding past as the cage climbs. */
    private void renderShaft(Graphics2D g) {
        int offset = (int) (t * 90) % 40;
        g.setColor(new Color(22, 20, 24));
        for (int y = -40 + offset; y < Window.HEIGHT; y += 40) {
            g.fillRect(0, y, 70, 18);
            g.fillRect(Window.WIDTH - 70, y + 20, 70, 18);
        }
    }

    /** Seen from inside the rock: a small lantern swaying down a black tunnel. */
    private void renderTunnel(Graphics2D g) {
        double depth = Math.min(1, t / 40);
        int lx = (int) (Window.WIDTH / 2 + Math.sin(t * 1.7) * 6);
        int ly = (int) (40 + depth * 220);
        int r = (int) (20 + depth * 30);
        g.setPaint(new java.awt.RadialGradientPaint(lx, ly, r * 2.5f, new float[]{0f, 1f},
                new Color[]{new Color(255, 200, 120, 60), new Color(0, 0, 0, 0)}));
        g.fillRect(0, 0, Window.WIDTH, Window.HEIGHT);
        g.setColor(new Color(255, 220, 150));
        g.fillOval(lx - 2, ly - 2, 4, 4);
        // the rock face you are part of now, in the foreground
        g.setColor(new Color(12, 6, 8));
        g.fillRect(0, 0, 60, Window.HEIGHT);
        g.fillRect(Window.WIDTH - 60, 0, 60, Window.HEIGHT);
        g.setColor(new Color(40, 12, 16));
        for (int y = 0; y < Window.HEIGHT; y += 23) g.drawLine(60, y, 50, y + 12);
    }
}
