package hollowshift.scenes;

import hollowshift.engine.Action;
import hollowshift.engine.Game;
import hollowshift.engine.gfx.Text;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Vertical keyboard-driven menu. Items may react to Confirm and to Left/Right adjustment. */
public final class MenuList {
    private record Item(Supplier<String> label, Runnable onConfirm, IntConsumer onAdjust, Supplier<Boolean> enabled) {
    }

    private final List<Item> items = new ArrayList<>();
    private int selected;
    private final Game game;

    public MenuList(Game game) {
        this.game = game;
    }

    public MenuList add(String label, Runnable onConfirm) {
        return add(() -> label, onConfirm, null, () -> true);
    }

    public MenuList add(Supplier<String> label, Runnable onConfirm, IntConsumer onAdjust, Supplier<Boolean> enabled) {
        items.add(new Item(label, onConfirm, onAdjust, enabled));
        return this;
    }

    public int selected() {
        return selected;
    }

    public void select(int i) {
        selected = Math.max(0, Math.min(items.size() - 1, i));
    }

    public void update() {
        var in = game.input;
        if (in.pressed(Action.DOWN)) move(1);
        if (in.pressed(Action.UP)) move(-1);
        Item it = items.get(selected);
        if (it.onAdjust != null) {
            if (in.pressed(Action.LEFT)) {
                it.onAdjust.accept(-1);
                game.audio.ui("ui_move");
            }
            if (in.pressed(Action.RIGHT)) {
                it.onAdjust.accept(1);
                game.audio.ui("ui_move");
            }
        }
        if (in.pressed(Action.CONFIRM) && it.onConfirm != null && it.enabled.get()) {
            game.audio.ui("ui_select");
            it.onConfirm.run();
        }
    }

    private void move(int d) {
        if (items.isEmpty()) return;
        selected = (selected + d + items.size()) % items.size();
        game.audio.ui("ui_move");
    }

    public void render(Graphics2D g, int cx, int y, int lineHeight, int size) {
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            boolean sel = i == selected;
            boolean on = it.enabled.get();
            Color c = !on ? new Color(90, 88, 86) : sel ? new Color(255, 214, 120) : new Color(200, 196, 188);
            String label = it.label.get();
            Text.draw(g, sel ? "> " + label + " <" : label, cx, y + i * lineHeight, size, c, Text.CENTER);
        }
    }
}
