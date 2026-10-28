package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.story.Run;

import java.awt.Color;
import java.awt.Graphics2D;

/** Between shifts: a short breather, then the next briefing. */
public final class ShiftCompleteScene extends Scene {
    private final Run run;
    private final double seconds;
    private double t;

    public ShiftCompleteScene(Game game, Run run, double seconds) {
        super(game);
        this.run = run;
        this.seconds = seconds;
    }

    @Override
    public void enter() {
        game.audio.play("elevator_bell");
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (t > 1.0 && game.input.pressed(Action.CONFIRM)) {
            run.advance();
            game.scenes.set(new BriefingScene(game, run));
        } else if (t > 1.0 && game.input.pressed(Action.BACK)) {
            game.scenes.set(new MenuScene(game));
        }
    }

    @Override
    public void render(Graphics2D g) {
        int cx = Window.WIDTH / 2;
        Text.draw(g, Lang.t("complete.title", run.shift()), cx, 130, 24, new Color(236, 214, 170), Text.CENTER);
        int m = (int) seconds / 60, s = (int) seconds % 60;
        Text.draw(g, Lang.t("complete.time", String.format("%d:%02d", m, s)), cx, 160, 11, new Color(180, 172, 160), Text.CENTER);
        if (t > 1.0) {
            Text.draw(g, Lang.t("complete.keys"), cx, 230, 11, new Color(255, 214, 120), Text.CENTER);
        }
    }
}
