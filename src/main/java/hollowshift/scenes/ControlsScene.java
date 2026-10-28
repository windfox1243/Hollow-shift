package hollowshift.scenes;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.Input;
import hollowshift.engine.Scene;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** Key rebinding. Enter on a row waits for the next key press; Delete clears the slot. */
public final class ControlsScene extends Scene {
    private final Scene back;
    private final List<Action> actions = new ArrayList<>();
    private int row;
    private int slot;
    private boolean waiting;

    public ControlsScene(Game game, Scene back) {
        super(game);
        this.back = back;
        for (Action a : Action.values()) if (a.rebindable()) actions.add(a);
    }

    private int rows() {
        return actions.size() + 2; // + "Reset to defaults" + "Back"
    }

    @Override
    public void update(double dt) {
        MenuBackdrop.update(dt);
        Input in = game.input;
        if (waiting) {
            int key = in.lastPressedKey();
            if (key == KeyEvent.VK_ESCAPE) {
                waiting = false;
            } else if (key != 0) {
                in.bind(actions.get(row), slot, key);
                waiting = false;
                game.audio.ui("ui_select");
                game.save.storeBindings(in);
            }
            return;
        }
        if (in.keyPressed(KeyEvent.VK_DOWN) || in.pressed(Action.DOWN)) move(1);
        if (in.keyPressed(KeyEvent.VK_UP) || in.pressed(Action.UP)) move(-1);
        if (row < actions.size() && (in.keyPressed(KeyEvent.VK_LEFT) || in.keyPressed(KeyEvent.VK_RIGHT))) {
            slot = 1 - slot;
            game.audio.ui("ui_move");
        }
        if (in.keyPressed(KeyEvent.VK_DELETE) && row < actions.size()) {
            in.bind(actions.get(row), slot, 0);
            game.save.storeBindings(in);
        }
        if (in.keyPressed(KeyEvent.VK_ENTER)) {
            game.audio.ui("ui_select");
            if (row < actions.size()) waiting = true;
            else if (row == actions.size()) {
                in.resetBindings();
                game.save.storeBindings(in);
            } else leave();
        }
        if (in.keyPressed(KeyEvent.VK_ESCAPE)) leave();
    }

    private void move(int d) {
        row = (row + d + rows()) % rows();
        game.audio.ui("ui_move");
    }

    private void leave() {
        game.save.save();
        game.scenes.set(back);
    }

    @Override
    public void render(Graphics2D g) {
        MenuBackdrop.render(g);
        int cx = Window.WIDTH / 2;
        Text.draw(g, Lang.t("controls.title"), cx, 40, 18, new Color(236, 214, 170), Text.CENTER);
        int y = 72;
        Color sel = new Color(255, 214, 120), norm = new Color(200, 196, 188);
        for (int i = 0; i < actions.size(); i++) {
            Action a = actions.get(i);
            int[] keys = game.input.binding(a);
            Text.draw(g, a.label(), cx - 40, y, 11, i == row ? sel : norm, Text.RIGHT);
            for (int s = 0; s < 2; s++) {
                boolean active = i == row && s == slot;
                String k = active && waiting ? Lang.t("controls.press") : Input.keyName(keys[s]);
                Text.draw(g, active ? "[" + k + "]" : k, cx + 20 + s * 100, y, 11, active ? sel : norm, Text.LEFT);
            }
            y += 18;
        }
        Text.draw(g, Lang.t("controls.reset"), cx, y + 10, 11, row == actions.size() ? sel : norm, Text.CENTER);
        Text.draw(g, Lang.t("common.back"), cx, y + 28, 11, row == actions.size() + 1 ? sel : norm, Text.CENTER);
        Text.draw(g, Lang.t("controls.help"), cx, Window.HEIGHT - 14, 9,
                new Color(120, 116, 110), Text.CENTER);
    }
}
