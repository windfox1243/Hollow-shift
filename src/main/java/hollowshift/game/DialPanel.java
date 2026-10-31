package hollowshift.game;

import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;
import hollowshift.engine.gfx.Text;
import hollowshift.entity.Creature;
import hollowshift.entity.Phone;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The phone's keypad. The surface only answers the shaft's own number, three digits someone
 * chalked on the walls in different rooms. A wrong number sets the old bell howling, and the
 * Tenant comes to see who is ringing.
 */
public final class DialPanel extends Panel {
    private static final String[] KEYS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "<"};
    private static final int CELL = 22;
    private final Phone phone;
    private final StringBuilder entered = new StringBuilder();
    private int cursor = 4; // the 5
    private double flash; // > 0 after a wrong number
    private double connecting = -1;

    public DialPanel(World world, Phone phone) {
        super(world, 150, 162);
        this.phone = phone;
    }

    @Override
    protected String title() {
        return Lang.t("panel.dial");
    }

    /** Presses one key of the pad: a digit, C (clear) or &lt; (rub out). */
    public void press(String key) {
        if (connecting >= 0) return;
        world.audio.play("type", Bus.SFX, 0.7, 0.6);
        switch (key) {
            case "C" -> entered.setLength(0);
            case "<" -> {
                if (!entered.isEmpty()) entered.setLength(entered.length() - 1);
            }
            default -> {
                if (entered.length() < phone.code().length()) entered.append(key);
                if (entered.length() == phone.code().length()) check();
            }
        }
    }

    private void check() {
        if (entered.toString().equals(phone.code())) {
            connecting = 0;
            world.audio.play("ui_select", Bus.SFX, 0.8, 0.7);
            return;
        }
        // the wrong line: a dead tone, then the bell howls in its box and calls it here
        flash = 0.6;
        entered.setLength(0);
        world.audio.play("ui_back", Bus.SFX, 0.9, 0.5);
        world.audio.playAt("surge", phone.x, phone.y, 1200, 0.9, 1.3);
        world.audio.playAt("elevator_bell", phone.x, phone.y, 1200, 0.8, 2.2);
        world.camera.shakeFrom(phone.x, phone.y, 20, 0.3);
        for (Creature c : world.creatures()) c.summon(phone.x, phone.y);
        world.toast(Lang.t("toast.wrongNumber"));
    }

    @Override
    public void update(Input in, double dt) {
        flash = Math.max(0, flash - dt);
        if (connecting >= 0) {
            connecting += dt;
            if (connecting > 0.8) {
                close();
                phone.connect();
            }
            return;
        }
        int col = cursor % 3, row = cursor / 3;
        if (in.pressed(Action.LEFT)) col = (col + 2) % 3;
        if (in.pressed(Action.RIGHT)) col = (col + 1) % 3;
        if (in.pressed(Action.UP)) row = (row + 3) % 4;
        if (in.pressed(Action.DOWN)) row = (row + 1) % 4;
        cursor = row * 3 + col;
        int d = digitPressed(in);
        if (d >= 0) {
            cursor = d == 0 ? 10 : d - 1;
            press(String.valueOf(d));
        } else if (pressedAny(in, Action.INTERACT, Action.CONFIRM)) {
            press(KEYS[cursor]);
        }
    }

    @Override
    protected void draw(Graphics2D g, int x, int y) {
        // the read-out
        int len = phone.code().length();
        int dx = x + (w - len * 18) / 2;
        for (int i = 0; i < len; i++) {
            g.setColor(STEEL_DARK);
            g.fillRect(dx + i * 18, y + 22, 14, 18);
            boolean lit = connecting >= 0 && (int) (connecting * 8) % 2 == 0;
            Color c = flash > 0 ? new Color(220, 60, 50) : lit ? new Color(120, 255, 140) : new Color(255, 190, 90);
            if (i < entered.length()) Text.draw(g, entered.substring(i, i + 1), dx + i * 18 + 7, y + 36, 12, c, Text.CENTER);
        }
        // what the miner has read off the walls so far
        Text.draw(g, Lang.t("panel.seen", world.codeMemo()), x + w / 2, y + 54, 8, DIM, Text.CENTER);
        // the pad
        int px = x + (w - 3 * CELL) / 2, py = y + 62;
        for (int i = 0; i < KEYS.length; i++) {
            int kx = px + i % 3 * CELL, ky = py + i / 3 * CELL;
            boolean sel = i == cursor;
            g.setColor(STEEL_DARK);
            g.fillRect(kx + 1, ky + 1, CELL - 3, CELL - 3);
            g.setColor(sel ? new Color(190, 170, 130) : new Color(132, 124, 112));
            g.fillRect(kx, ky, CELL - 3, CELL - 4);
            Text.draw(g, KEYS[i], kx + (CELL - 3) / 2, ky + 13, 10, STEEL_DARK, Text.CENTER);
            if (sel) {
                g.setColor(new Color(255, 214, 120));
                g.drawRect(kx - 1, ky - 1, CELL - 2, CELL - 2);
            }
        }
        if (connecting >= 0) Text.draw(g, Lang.t("panel.ringing"), x + w / 2, y + h - 8, 9, new Color(120, 255, 140), Text.CENTER);
    }
}
