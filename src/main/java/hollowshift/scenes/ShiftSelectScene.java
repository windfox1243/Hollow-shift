package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.rules.Rule;
import hollowshift.story.Run;
import hollowshift.story.ShiftDef;

import java.awt.Color;
import java.awt.Graphics2D;

/** Replay any unlocked story shift. */
public final class ShiftSelectScene extends Scene {
    private final MenuList menu;

    public ShiftSelectScene(Game game) {
        super(game);
        menu = new MenuList(game);
        for (ShiftDef s : game.campaign.shifts()) {
            boolean unlocked = s.number() <= game.save.shiftsUnlocked;
            menu.add(() -> unlocked ? s.title() : Lang.t("select.locked", s.number()),
                    () -> {
                        MenuBackdrop.stopMusic();
                        game.scenes.set(new BriefingScene(game, Run.story(s.number())));
                    }, null, () -> unlocked);
        }
        menu.add(Lang.t("common.back"), () -> game.scenes.set(new MenuScene(game)));
    }

    @Override
    public void update(double dt) {
        MenuBackdrop.update(dt);
        menu.update();
        if (game.input.pressed(Action.BACK)) {
            game.audio.ui("ui_back");
            game.scenes.set(new MenuScene(game));
        }
    }

    @Override
    public void render(Graphics2D g) {
        MenuBackdrop.render(g);
        Text.draw(g, Lang.t("select.title"), Window.WIDTH / 2, 60, 20, new Color(236, 214, 170), Text.CENTER);
        menu.render(g, Window.WIDTH / 2, 110, 20, 12);
        int sel = menu.selected();
        if (sel < game.campaign.shifts().size()) {
            ShiftDef s = game.campaign.shifts().get(sel);
            if (s.number() <= game.save.shiftsUnlocked) {
                StringBuilder sb = new StringBuilder();
                for (Rule r : s.rules().ordered()) sb.append(sb.isEmpty() ? "" : "  ·  ").append(Lang.t("rule." + r.name() + ".short"));
                Text.draw(g, sb.isEmpty() ? Lang.t("select.norules") : sb.toString(), Window.WIDTH / 2, Window.HEIGHT - 30, 10,
                        new Color(160, 150, 140), Text.CENTER);
            }
        }
    }
}
