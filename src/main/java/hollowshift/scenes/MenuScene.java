package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.story.Run;

import java.awt.Color;
import java.awt.Graphics2D;

/** Title screen. */
public final class MenuScene extends Scene {
    private final MenuList menu;

    public MenuScene(Game game) {
        super(game);
        int next = Math.min(game.save.shiftsUnlocked, game.campaign.shifts().size());
        menu = new MenuList(game);
        if (next > 1) menu.add(Lang.t("menu.continue", next), () -> startStory(next));
        // a new game replays the story from shift 1; unlocked shifts stay unlocked
        menu.add(Lang.t("menu.start"), () -> startStory(1))
                .add(Lang.t("menu.select"), () -> game.scenes.set(new ShiftSelectScene(game)))
                .add(Lang.t("menu.endless"), () -> game.scenes.set(new EndlessSetupScene(game)))
                .add(Lang.t("menu.settings"), () -> game.scenes.set(new SettingsScene(game, () -> new MenuScene(game))))
                .add(Lang.t("menu.credits"), () -> game.scenes.set(new CreditsScene(game, false)))
                .add(Lang.t("menu.quit"), game::quit);
    }

    private void startStory(int shift) {
        MenuBackdrop.stopMusic();
        game.scenes.set(new BriefingScene(game, Run.story(shift)));
    }

    @Override
    public void enter() {
        MenuBackdrop.startMusic(game);
    }

    @Override
    public void update(double dt) {
        MenuBackdrop.update(dt);
        menu.update();
    }

    @Override
    public void render(Graphics2D g) {
        MenuBackdrop.render(g);
        Text.draw(g, Game.TITLE, Window.WIDTH / 2, 92, 36, new Color(236, 214, 170), Text.CENTER);
        Text.draw(g, Lang.t("menu.subtitle"), Window.WIDTH / 2, 114, 11, new Color(150, 140, 128), Text.CENTER);
        menu.render(g, Window.WIDTH / 2, 156, 20, 13);
        if (game.save.bestEndless > 0) {
            Text.draw(g, Lang.t("menu.best", game.save.bestEndless), Window.WIDTH / 2, Window.HEIGHT - 14, 9,
                    new Color(120, 116, 110), Text.CENTER);
        }
    }
}
