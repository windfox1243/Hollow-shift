package hollowshift.game;

import hollowshift.engine.Lang;
import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Text;
import hollowshift.entity.Entity;
import hollowshift.entity.Interactable;
import hollowshift.entity.Pack;
import hollowshift.entity.Player;
import hollowshift.entity.Supply;
import hollowshift.rules.StatusEffect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Heads-up display: objectives, survival bars, exposure/noise meters, pockets, prompts and messages. */
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

        // pockets (bottom-right): four slots, the selected one outlined
        int slot = 22, gap = 4;
        int px0 = Window.WIDTH - 8 - Supply.values().length * (slot + gap) + gap, py0 = Window.HEIGHT - 14 - slot;
        Pack pack = p.pack();
        for (Supply s : Supply.values()) {
            int sx = px0 + s.ordinal() * (slot + gap);
            int n = pack.count(s);
            g.setColor(PANEL);
            g.fillRect(sx, py0, slot, slot);
            if (n > 0) {
                BufferedImage icon = w.art.get(s.sprite);
                int iw = icon.getWidth() * 2, ih = icon.getHeight() * 2;
                g.drawImage(icon, sx + (slot - iw) / 2, py0 + (slot - ih) / 2, iw, ih, null);
                if (n > 1) Text.draw(g, "x" + n, sx + slot, py0 + slot, 8, TEXT, Text.RIGHT);
            }
            Text.draw(g, String.valueOf(s.ordinal() + 1), sx + slot / 2, Window.HEIGHT - 4, 8, s == pack.selected() ? TEXT : DIM, Text.CENTER);
            if (s == pack.selected()) {
                g.setColor(new Color(255, 214, 120));
                g.drawRect(sx - 1, py0 - 1, slot + 1, slot + 1);
            }
        }
        int ly = py0 - 5;
        Supply sel = pack.selected();
        if (pack.count(sel) > 0) {
            String key = Input.keyName(w.input().binding(Action.THROW)[0]);
            Text.draw(g, "[" + key + "] " + Lang.t("hud.use." + sel.key, sel.displayName()), Window.WIDTH - 8, ly, 9, TEXT, Text.RIGHT);
            ly -= 12;
        }
        if (p.held() != null) {
            String key = Input.keyName(w.input().binding(Action.DROP)[0]);
            Text.draw(g, p.held().displayName().toUpperCase() + "  [" + key + "] " + Lang.t("hud.drop"), Window.WIDTH - 8, ly, 9, TEXT, Text.RIGHT);
            ly -= 12;
        }
        Text.draw(g, Lang.t("hud.lantern", Input.keyName(w.input().binding(Action.LANTERN)[0]),
                Lang.t(p.lanternOn() ? "common.on" : "common.off")), Window.WIDTH - 8, ly, 9, DIM, Text.RIGHT);

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
