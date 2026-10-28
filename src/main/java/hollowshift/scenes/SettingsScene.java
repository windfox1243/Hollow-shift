package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.story.SaveData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/** Volume, display and control settings. Returns to whichever scene opened it. */
public final class SettingsScene extends Scene {
    private final Supplier<Scene> back;
    private final MenuList menu;

    public SettingsScene(Game game, Supplier<Scene> back) {
        super(game);
        this.back = back;
        SaveData s = game.save;
        menu = new MenuList(game)
                .add(() -> Lang.t("settings.master") + "  " + pct(s.master), null, d -> {
                    s.master = step(s.master, d);
                    apply();
                }, () -> true)
                .add(() -> Lang.t("settings.music") + "  " + pct(s.music), null, d -> {
                    s.music = step(s.music, d);
                    apply();
                }, () -> true)
                .add(() -> Lang.t("settings.sfx") + "  " + pct(s.sfx), null, d -> {
                    s.sfx = step(s.sfx, d);
                    apply();
                }, () -> true)
                .add(() -> Lang.t("settings.fullscreen") + "  " + onOff(s.fullscreen), () -> {
                    s.fullscreen = !s.fullscreen;
                    apply();
                }, d -> {
                    s.fullscreen = !s.fullscreen;
                    apply();
                }, () -> true)
                .add(() -> Lang.t("settings.shake") + "  " + onOff(s.screenShake), () -> s.screenShake = !s.screenShake,
                        d -> s.screenShake = !s.screenShake, () -> true)
                .add(() -> Lang.t("settings.jumpscare") + "  " + onOff(s.jumpscare), () -> s.jumpscare = !s.jumpscare,
                        d -> s.jumpscare = !s.jumpscare, () -> true)
                .add(() -> Lang.t("settings.language") + "  < " + Lang.current().nativeName + " >", this::nextLanguage,
                        d -> nextLanguage(), () -> true)
                .add(Lang.t("settings.controls"), () -> game.scenes.set(new ControlsScene(game, this)))
                .add(Lang.t("common.back"), this::leave);
    }

    private static String onOff(boolean v) {
        return Lang.t(v ? "common.on" : "common.off");
    }

    /** Switches language and rebuilds this screen so its labels are re-translated. */
    private void nextLanguage() {
        Lang.Language next = Lang.current().next();
        Lang.set(next);
        game.save.language = next.code;
        game.save.save();
        SettingsScene again = new SettingsScene(game, back);
        again.menu.select(menu.selected());
        game.scenes.set(again);
    }

    private static String pct(double v) {
        return "< " + Math.round(v * 100) + "% >";
    }

    private static double step(double v, int d) {
        return Math.max(0, Math.min(1, Math.round((v + d * 0.1) * 10) / 10.0));
    }

    private void apply() {
        game.applySettings();
    }

    private void leave() {
        game.save.save();
        game.scenes.set(back.get());
    }

    @Override
    public void update(double dt) {
        MenuBackdrop.update(dt);
        menu.update();
        if (game.input.pressed(Action.BACK)) {
            game.audio.ui("ui_back");
            leave();
        }
    }

    @Override
    public void render(Graphics2D g) {
        MenuBackdrop.render(g);
        Text.draw(g, Lang.t("settings.title"), Window.WIDTH / 2, 60, 20, new Color(236, 214, 170), Text.CENTER);
        menu.render(g, Window.WIDTH / 2, 110, 20, 12);
        Text.draw(g, Lang.t("settings.hint"), Window.WIDTH / 2, Window.HEIGHT - 20, 9, new Color(120, 116, 110), Text.CENTER);
    }
}
