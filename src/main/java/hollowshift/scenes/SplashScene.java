package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;

import java.awt.Color;
import java.awt.Graphics2D;

/** Short opening card; also warms up the audio synthesiser. */
public final class SplashScene extends Scene {
    private double t;

    public SplashScene(Game game) {
        super(game);
    }

    @Override
    public void enter() {
        game.audio.preload("step", "step_soft", "creature_step", "creature_step_heavy", "sniff", "grab", "drop",
                "throw", "stone_land", "fuse_insert", "power_on", "surge", "eat", "flare", "lantern", "elevator",
                "elevator_bell", "echo", "stun", "death", "scare_main", "chase_found", "heartbeat", "knock", "drone", "menu_drone", "ui_move",
                "ui_select", "ui_back", "type", "rule");
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (t > 3.2 || (t > 0.5 && (game.input.pressed(Action.CONFIRM) || game.input.pressed(Action.BACK)))) {
            game.scenes.set(new MenuScene(game));
        }
    }

    @Override
    public void render(Graphics2D g) {
        double a = Math.min(1, t / 0.8) * Math.min(1, Math.max(0, (3.2 - t) / 0.6));
        int alpha = (int) (255 * a);
        Text.draw(g, Lang.t("splash.line1"), Window.WIDTH / 2, Window.HEIGHT / 2 - 6, 10,
                new Color(170, 165, 160, alpha), Text.CENTER);
        Text.draw(g, Lang.t("splash.line2"), Window.WIDTH / 2, Window.HEIGHT / 2 + 12, 10,
                new Color(120, 116, 112, alpha), Text.CENTER);
    }
}
