package hollowshift.entity;

import hollowshift.game.World;
import hollowshift.world.TileMap;

import java.awt.Graphics2D;

/**
 * Base class for everything placed in the world. (x, y) is the centre of the
 * entity's footprint on the ground; the collision box extends hw/hh around it.
 */
public abstract class Entity {
    protected World world;
    public double x;
    public double y;
    protected double hw = 4;
    protected double hh = 3;
    private boolean removed;

    protected Entity(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Called when the entity is added to a world. */
    public void attach(World world) {
        this.world = world;
    }

    public void update(double dt) {
    }

    /** Draws relative to the camera's top-left corner (ox, oy). */
    public abstract void render(Graphics2D g, int ox, int oy);

    /** Draw order: larger y is drawn later (in front). */
    public double sortY() {
        return y;
    }

    public void remove() {
        removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public double distanceTo(Entity e) {
        return Math.hypot(e.x - x, e.y - y);
    }

    public double distanceTo(double px, double py) {
        return Math.hypot(px - x, py - y);
    }

    /** Moves by (dx, dy) one axis at a time, stopping at solid tiles. Returns true if blocked. */
    protected boolean moveAndSlide(double dx, double dy) {
        TileMap map = world.map;
        boolean blocked = false;
        if (dx != 0) {
            double nx = x + dx;
            if (map.boxBlocked(nx - hw, y - hh, nx + hw, y + hh)) {
                blocked = true;
                // slide up to the wall edge
                nx = dx > 0 ? Math.floor((nx + hw) / TileMap.TILE) * TileMap.TILE - hw - 0.01
                        : Math.ceil((nx - hw) / TileMap.TILE) * TileMap.TILE + hw + 0.01;
                if (map.boxBlocked(nx - hw, y - hh, nx + hw, y + hh)) nx = x;
            }
            x = nx;
        }
        if (dy != 0) {
            double ny = y + dy;
            if (map.boxBlocked(x - hw, ny - hh, x + hw, ny + hh)) {
                blocked = true;
                ny = dy > 0 ? Math.floor((ny + hh) / TileMap.TILE) * TileMap.TILE - hh - 0.01
                        : Math.ceil((ny - hh) / TileMap.TILE) * TileMap.TILE + hh + 0.01;
                if (map.boxBlocked(x - hw, ny - hh, x + hw, ny + hh)) ny = y;
            }
            y = ny;
        }
        return blocked;
    }
}
