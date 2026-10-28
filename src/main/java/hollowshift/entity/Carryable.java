package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.world.TileMap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** An item the player carries in their hands (one at a time) and can drop or throw. */
public abstract class Carryable extends Item {
    private static final double FLIGHT_TIME = 0.45;

    private boolean held;
    private boolean flying;
    private double fromX, fromY, toX, toY, flightT;

    protected Carryable(double x, double y, String displayName) {
        super(x, y, displayName);
    }

    /** Whether Throw sends it flying (true) or just drops it at the player's feet (false). */
    public abstract boolean throwable();

    /** Called when a thrown item hits the ground. */
    protected void onLanded() {
    }

    /** Called when a thrown item hits the Tenant mid-flight. */
    protected void onHitCreature(Creature c) {
        onLanded();
    }

    public boolean isHeld() {
        return held;
    }

    public boolean isFlying() {
        return flying;
    }

    void setHeld(boolean h) {
        held = h;
    }

    @Override
    public String prompt(Player p) {
        return Lang.t("prompt.pickup", displayName());
    }

    @Override
    public boolean canInteract(Player p) {
        return !held && !flying && !p.isHolding();
    }

    @Override
    public void interact(Player p) {
        p.pickUp(this);
    }

    /** Launches the item toward (tx, ty), stopping short of walls. */
    public void launch(double sx, double sy, double tx, double ty) {
        held = false;
        flying = true;
        fromX = x = sx;
        fromY = y = sy;
        double d = Math.hypot(tx - sx, ty - sy);
        int steps = Math.max(1, (int) (d / 4));
        double lx = sx, ly = sy;
        for (int i = 1; i <= steps; i++) {
            double px = sx + (tx - sx) * i / steps, py = sy + (ty - sy) * i / steps;
            if (world.map.solidAt(px, py)) break;
            lx = px;
            ly = py;
        }
        toX = lx;
        toY = ly;
        flightT = 0;
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        if (held) {
            Player p = world.player;
            x = p.x;
            y = p.y + 0.1;
            return;
        }
        if (!flying) return;
        flightT += dt / FLIGHT_TIME;
        double t = Math.min(1, flightT);
        x = fromX + (toX - fromX) * t;
        y = fromY + (toY - fromY) * t;
        for (Creature c : world.creatures()) {
            if (c.distanceTo(x, y) < 14 && c.isVisibleInWorld()) {
                flying = false;
                onHitCreature(c);
                return;
            }
        }
        if (t >= 1) {
            flying = false;
            if (world.map.solid(TileMap.toTile(x), TileMap.toTile(y))) {
                x = fromX;
                y = fromY;
            }
            onLanded();
        }
    }

    @Override
    public void render(Graphics2D g, int ox, int oy) {
        if (held) return;
        if (!flying) {
            super.render(g, ox, oy);
            return;
        }
        double t = Math.min(1, flightT);
        double arc = Math.sin(Math.PI * t) * 22;
        BufferedImage img = world.art.get(sprite());
        int sx = (int) Math.round(x) - ox, sy = (int) Math.round(y) - oy;
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(sx - 3, sy - 1, 6, 3);
        g.drawImage(img, sx - img.getWidth() / 2, (int) (sy - img.getHeight() - arc), null);
    }

    @Override
    public double sortY() {
        return held ? world.player.y + 0.1 : y;
    }
}
