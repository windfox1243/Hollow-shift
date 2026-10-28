package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Game;
import hollowshift.engine.Rng;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.story.Run;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/** Endless mode setup: type a seed (or leave it empty for a random one) and press Enter. */
public final class EndlessSetupScene extends Scene {
    private final StringBuilder seed = new StringBuilder();
    private double t;

    public EndlessSetupScene(Game game) {
        super(game);
    }

    @Override
    public void update(double dt) {
        t += dt;
        MenuBackdrop.update(dt);
        String typed = game.input.typed();
        for (char c : typed.toCharArray()) {
            if (Character.isLetterOrDigit(c) && seed.length() < 16) {
                seed.append(Character.toUpperCase(c));
                game.audio.ui("type");
            }
        }
        if (game.input.keyPressed(KeyEvent.VK_BACK_SPACE) && !seed.isEmpty()) {
            seed.setLength(seed.length() - 1);
            game.audio.ui("type");
        } else if (game.input.keyPressed(KeyEvent.VK_ESCAPE)) {
            game.audio.ui("ui_back");
            game.scenes.set(new MenuScene(game));
        } else if (game.input.keyPressed(KeyEvent.VK_ENTER)) {
            String text = seed.isEmpty() ? Long.toString(Math.abs(new Rng(System.nanoTime()).nextLong()) % 1_000_000) : seed.toString();
            game.audio.ui("ui_select");
            MenuBackdrop.stopMusic();
            game.scenes.set(new BriefingScene(game, Run.endless(Rng.seedFromText(text), text)));
        }
    }

    @Override
    public void render(Graphics2D g) {
        MenuBackdrop.render(g);
        int cx = Window.WIDTH / 2;
        Text.draw(g, Lang.t("endless.title"), cx, 70, 20, new Color(236, 214, 170), Text.CENTER);
        Text.drawWrapped(g, Lang.t("endless.desc"),
                cx, 104, 380, 10, new Color(170, 162, 150), Text.CENTER);
        Text.draw(g, Lang.t("endless.seed"), cx, 168, 12, new Color(200, 196, 188), Text.CENTER);
        String shown = seed + ((int) (t * 2) % 2 == 0 ? "_" : " ");
        g.setColor(new Color(20, 18, 24));
        g.fillRect(cx - 90, 176, 180, 22);
        g.setColor(new Color(90, 80, 70));
        g.drawRect(cx - 90, 176, 180, 22);
        Text.draw(g, seed.isEmpty() && (int) (t * 2) % 2 == 1 ? Lang.t("endless.random") : shown, cx, 192, 13,
                new Color(255, 214, 120), Text.CENTER);
        Text.draw(g, Lang.t("endless.keys"), cx, 240, 10, new Color(140, 134, 126), Text.CENTER);
    }
}
