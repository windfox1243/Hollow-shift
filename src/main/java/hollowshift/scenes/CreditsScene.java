package hollowshift.scenes;

import hollowshift.engine.Action;
import hollowshift.engine.Lang;
import hollowshift.engine.Game;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.engine.io.Resources;

import java.awt.Color;
import java.awt.Graphics2D;

/** Scrolling credits read from /credits.txt. Lines starting with '#' are headings. */
public final class CreditsScene extends Scene {
    private final String[] lines;
    private final boolean afterEnding;
    private double scroll;

    public CreditsScene(Game game, boolean afterEnding) {
        super(game);
        this.afterEnding = afterEnding;
        String text = Resources.text("/credits_" + Lang.current().code + ".txt");
        if (text == null) text = Resources.text("/credits.txt");
        lines = (text == null ? Game.TITLE : text).split("\r?\n");
    }

    @Override
    public void enter() {
        if (!afterEnding) MenuBackdrop.startMusic(game);
    }

    @Override
    public void update(double dt) {
        boolean fast = game.input.held(Action.CONFIRM) || game.input.held(Action.DOWN);
        scroll += dt * (fast ? 90 : 22);
        if (game.input.pressed(Action.BACK) || scroll > lines.length * 16 + Window.HEIGHT + 20) {
            game.scenes.set(new MenuScene(game));
        }
    }

    @Override
    public void render(Graphics2D g) {
        int y = (int) (Window.HEIGHT - scroll);
        for (String line : lines) {
            if (y > -20 && y < Window.HEIGHT + 20) {
                boolean head = line.startsWith("#");
                Text.draw(g, head ? line.substring(1).trim() : line, Window.WIDTH / 2, y, head ? 13 : 10,
                        head ? new Color(236, 214, 170) : new Color(190, 184, 174), Text.CENTER);
            }
            y += 16;
        }
    }
}
