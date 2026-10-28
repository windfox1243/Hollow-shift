package hollowshift.entity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** A static object built into a room (fuse box, heater, lamp...). Drawn standing on its tile. */
public abstract class Fixture extends Entity {
    protected Fixture(double x, double y) {
        super(x, y);
    }

    protected abstract String sprite();

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        BufferedImage img = world.art.get(sprite());
        g.drawImage(img, (int) Math.round(x) - img.getWidth() / 2 - ox, (int) Math.round(y) + 5 - img.getHeight() - oy, null);
    }
}
