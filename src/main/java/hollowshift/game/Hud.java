package hollowshift.game;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.entity.Entity;
import hollowshift.entity.Interactable;
import hollowshift.entity.Player;
import hollowshift.rules.StatusEffect;

import java.awt.Color;
import java.awt.Graphics2D;

/** Heads-up display: objectives, survival bars, exposure/noise meters, prompts and messages. */
public final class Hud {
    private static final Color PANEL = new Color(10, 10, 14, 170);
    private static final Color TEXT = new Color(225, 220, 210);
    private static final Color DIM = new Color(150, 145, 140);

    public void render(Graphics2D g, World w) {
        Player p = w.player;
        int ox = w.camera.left(), oy = w.camera.top();

        // objectives (top-left)
        int y = 14;
        for (String line : w.objectives().lines()) {
            Text.draw(g, line, 8, y, 10, line.startsWith(">") ? TEXT : DIM, Text.LEFT);
            y += 12;
        }

        // survival bars (bottom-left)
        int by = Window.HEIGHT - 12;
        for (StatusEffect e : w.effects()) {
            boolean flash = e.critical(p) && (int) (w.time * 4) % 2 == 0;
            bar(g, 8, by - 6, 70, 6, e.value(p), flash ? Color.WHITE : e.color());
            Text.draw(g, e.label(), 82, by, 9, flash ? Color.WHITE : DIM, Text.LEFT);
            by -= 12;
        }

        // exposure and noise meters (top-right)
        double vis = w.visibilityAt(p.x, p.y - 6, true);
        double noise = Math.min(1, w.noises.loudestRecent() / 340);
        meter(g, Window.WIDTH - 86, 8, Lang.t("hud.seen"), vis, new Color(250, 230, 160));
        meter(g, Window.WIDTH - 86, 20, Lang.t("hud.heard"), noise, new Color(170, 210, 255));

        // stamina under the player while it is not full
        if (p.stamina < 1) {
            int sx = (int) p.x - ox - 8, sy = (int) p.y - oy + 4;
            bar(g, sx, sy, 16, 2, p.stamina, p.stamina < 0.35 ? new Color(230, 90, 70) : new Color(200, 200, 200));
        }

        // held item (bottom-right)
        if (p.held() != null) {
            int hx = Window.WIDTH - 110, hy = Window.HEIGHT - 26;
            g.setColor(PANEL);
            g.fillRect(hx, hy, 102, 18);
            String key = Input.keyName(w.input().binding(Action.THROW)[0]);
            Text.draw(g, p.held().displayName().toUpperCase() + "  [" + key + "] "
                    + Lang.t(p.held().throwable() ? "hud.throw" : "hud.drop"), hx + 5, hy + 12, 9, TEXT, Text.LEFT);
        }
        Text.draw(g, Lang.t("hud.lantern", Input.keyName(w.input().binding(Action.LANTERN)[0]),
                Lang.t(p.lanternOn() ? "common.on" : "common.off")), Window.WIDTH - 8, Window.HEIGHT - 32 - (p.held() != null ? 0 : -18), 9, DIM, Text.RIGHT);

        // interaction prompt above the focused object
        Interactable f = p.focus();
        if (f instanceof Entity e && f.prompt(p) != null) {
            boolean can = f.canInteract(p);
            String key = Input.keyName(w.input().binding(Action.INTERACT)[0]);
            String text = can ? "[" + key + "] " + f.prompt(p) : f.prompt(p);
            int px = (int) e.x - ox, py = (int) e.y - oy - 24;
            int tw = Text.width(g, text, 9);
            g.setColor(PANEL);
            g.fillRect(px - tw / 2 - 4, py - 10, tw + 8, 13);
            Text.draw(g, text, px, py, 9, can ? TEXT : DIM, Text.CENTER);
            if (f.holdTime() > 0 && p.holdProgress() > 0) bar(g, px - 15, py + 5, 30, 3, p.holdProgress(), new Color(120, 255, 140));
        }

        // toast message
        String toast = w.toast();
        if (toast != null) {
            int a = (int) (255 * w.toastAlpha());
            Text.draw(g, toast, Window.WIDTH / 2, Window.HEIGHT - 44, 11, new Color(240, 235, 220, a), Text.CENTER);
        }
    }

    private static void bar(Graphics2D g, int x, int y, int w, int h, double v, Color c) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(c);
        g.fillRect(x, y, (int) Math.round(w * Math.max(0, Math.min(1, v))), h);
    }

    private static void meter(Graphics2D g, int x, int y, String label, double v, Color c) {
        Text.draw(g, label, x, y + 7, 8, DIM, Text.LEFT);
        int segs = 8;
        int lit = (int) Math.round(v * segs);
        for (int i = 0; i < segs; i++) {
            g.setColor(i < lit ? c : new Color(40, 40, 46));
            g.fillRect(x + 36 + i * 5, y + 1, 4, 6);
        }
    }
}
