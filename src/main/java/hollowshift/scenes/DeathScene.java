package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Rng;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.rules.DeathCause;
import hollowshift.story.Run;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/** Shown after dying: cause, a tip, retry or quit. */
public final class DeathScene extends Scene {
    private final Run run;
    private final DeathCause cause;
    private final String tip;
    private final MenuList menu;
    private double t;

    public DeathScene(Game game, Run run, DeathCause cause) {
        super(game);
        this.run = run;
        this.cause = cause;
        List<String> tips = game.campaign.tips();
        this.tip = tips.isEmpty() ? "" : tips.get(new Rng(System.nanoTime()).range(0, tips.size()));
        menu = new MenuList(game)
                .add(Lang.t("death.retry"), this::retry)
                .add(Lang.t("death.menu"), () -> game.scenes.set(new MenuScene(game)));
    }

    private void retry() {
        run.newAttempt();
        game.scenes.set(new BriefingScene(game, run));
    }

    @Override
    public void update(double dt) {
        t += dt;
        if (t < 0.6) return;
        menu.update();
        if (game.input.pressed(Action.BACK)) game.scenes.set(new MenuScene(game));
    }

    @Override
    public void render(Graphics2D g) {
        int cx = Window.WIDTH / 2;
        double a = Math.min(1, t / 1.2);
        Text.draw(g, cause.title(), cx, 110, 32, new Color(200, 50, 40, (int) (255 * a)), Text.CENTER);
        Text.draw(g, cause.text(), cx, 140, 11, new Color(190, 180, 170, (int) (255 * a)), Text.CENTER);
        String where = run.mode == Run.Mode.ENDLESS ? Lang.t("death.endless", run.shift() - 1)
                : run.def(game.campaign).title();
        Text.draw(g, where, cx, 160, 9, new Color(130, 124, 116), Text.CENTER);
        if (t >= 0.6) menu.render(g, cx, 210, 20, 12);
        Text.drawWrapped(g, Lang.t("death.tip", tip), cx, Window.HEIGHT - 40, 460, 9, new Color(150, 144, 134), Text.CENTER);
    }
}
