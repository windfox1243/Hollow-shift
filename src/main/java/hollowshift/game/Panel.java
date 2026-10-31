package hollowshift.game;

import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Lang;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A close-up of a machine the player is working on: dialling the phone, bleeding a valve,
 * rewiring a junction box. The shift does not stop while it is open, so the plate is kept small
 * and the dark around it stays in view; the miner just stands there, hands busy.
 */
public abstract class Panel {
    protected static final Color TEXT = new Color(225, 220, 210);
    protected static final Color DIM = new Color(150, 145, 140);
    protected static final Color STEEL = new Color(52, 50, 48);
    protected static final Color STEEL_DARK = new Color(26, 24, 24);
    protected static final Color STEEL_LIGHT = new Color(96, 90, 84);

    protected final World world;
    protected final int w, h;
    private boolean closed;

    protected Panel(World world, int w, int h) {
        this.world = world;
        this.w = w;
        this.h = h;
    }

    /** Called once per frame while open, before the entities move. */
    public abstract void update(Input in, double dt);

    /** Draws the inside of the plate; (x, y) is its top-left corner. */
    protected abstract void draw(Graphics2D g, int x, int y);

    protected abstract String title();

    public final void close() {
        closed = true;
    }

    public final boolean isClosed() {
        return closed;
    }

    protected final int left() {
        return (Window.WIDTH - w) / 2;
    }

    protected final int top() {
        // a little above the middle, so the miner standing at the machine shows below it
        return (Window.HEIGHT - h) / 2 - 30;
    }

    public final void render(Graphics2D g) {
        int x = left(), y = top();
        // a riveted steel plate
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x + 3, y + 3, w, h);
        g.setColor(STEEL_DARK);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(STEEL);
        g.fillRect(x, y, w, h);
        g.setColor(STEEL_LIGHT);
        g.drawLine(x, y, x + w - 1, y);
        g.drawLine(x, y, x, y + h - 1);
        g.setColor(new Color(120, 70, 40, 90));
        for (int i = 0; i < 6; i++) g.fillRect(x + (i * 37 + 11) % (w - 6), y + (i * 23 + 7) % (h - 6), 3 + i % 3, 2);
        g.setColor(STEEL_DARK);
        for (int[] r : new int[][]{{3, 3}, {w - 5, 3}, {3, h - 5}, {w - 5, h - 5}}) g.fillRect(x + r[0], y + r[1], 2, 2);
        Text.draw(g, title(), x + w / 2, y + 14, 10, new Color(236, 214, 170), Text.CENTER);
        draw(g, x, y);
        String back = Input.keyName(world.input().binding(Action.BACK)[0]);
        Text.draw(g, Lang.t("panel.leave", back), x + w / 2, y + h + 12, 8, DIM, Text.CENTER);
    }

    /** Number key 0..9 pressed this frame (top row or keypad), or -1. */
    protected static int digitPressed(Input in) {
        for (int d = 0; d <= 9; d++) {
            if (in.keyPressed(java.awt.event.KeyEvent.VK_0 + d) || in.keyPressed(java.awt.event.KeyEvent.VK_NUMPAD0 + d)) return d;
        }
        return -1;
    }

    protected static boolean pressedAny(Input in, Action... actions) {
        for (Action a : actions) if (in.pressed(a)) return true;
        return false;
    }
}
