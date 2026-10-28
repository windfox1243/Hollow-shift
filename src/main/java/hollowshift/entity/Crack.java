package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A fissure deep in the final level where the knocking comes from. Once every
 * note has been read and the lift has arrived, the player can choose to stay
 * below and take the lodger's place (the hidden ending).
 */
public final class Crack extends Fixture implements Interactable {
    private double knockTimer = 4;

    public Crack(double x, double y) {
        super(x, y);
    }

    private boolean open() {
        return world.settings().allNotesRead() && world.objectives().exitOpen();
    }

    @Override
    protected String sprite() {
        return "crack"; // unused: the fissure is drawn directly in render()
    }

    @Override
    public void update(double dt) {
        knockTimer -= dt;
        if (knockTimer <= 0) {
            knockTimer = world.rng.range(6, 9);
            world.audio.playAt("knock", x, y, 520, 0.9, 1);
        }
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        int sx = (int) Math.round(x) - ox, sy = (int) Math.round(y) - oy;
        g.setColor(new Color(8, 2, 4));
        g.fillPolygon(new int[]{sx - 2, sx + 1, sx - 1, sx + 3, sx, sx + 2, sx - 3},
                new int[]{sy - 14, sy - 9, sy - 5, sy - 1, sy + 3, sy + 4, sy - 6}, 7);
        if (open()) {
            // a faint warmth breathing from inside once it will let you in
            int a = (int) (60 + 50 * Math.sin(world.time * 2));
            g.setColor(new Color(150, 30, 30, a));
            g.fillRect(sx - 1, sy - 8, 2, 8);
        }
    }

    @Override
    public String prompt(Player p) {
        return open() ? Lang.t("prompt.stay") : Lang.t("prompt.knock");
    }

    @Override
    public boolean canInteract(Player p) {
        return open();
    }

    @Override
    public double holdTime() {
        return 3.0;
    }

    @Override
    public void interact(Player p) {
        world.audio.play("knock", Bus.SFX, 1, 0.8);
        world.objectives().stayBelow();
    }

    @Override
    public double interactRange() {
        return 20;
    }
}
