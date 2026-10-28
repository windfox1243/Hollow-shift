package hollowshift.entity;

import hollowshift.engine.Lang;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** A small object lying on the floor that the player can interact with. */
public abstract class Item extends Entity implements Interactable {
    protected final String displayName;
    private double bobTime;

    protected Item(double x, double y, String displayName) {
        super(x, y);
        this.displayName = displayName;
        hw = 3;
        hh = 2;
        bobTime = (x * 13 + y * 7) % 6;
    }

    /** Localised name; displayName holds the item key (fuse, stone...). */
    public String displayName() {
        return Lang.t("item." + displayName);
    }

    protected abstract String sprite();

    @Override
    public void update(double dt) {
        bobTime += dt;
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        BufferedImage img = world.art.get(sprite());
        int sx = (int) Math.round(x) - ox;
        int sy = (int) Math.round(y) - oy;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(sx - 4, sy - 1, 8, 3);
        int bob = (int) Math.round(Math.sin(bobTime * 3) * 0.6);
        g.drawImage(img, sx - img.getWidth() / 2, sy - img.getHeight() + 1 + bob, null);
    }

    @Override
    public double interactRange() {
        return 16;
    }
}
