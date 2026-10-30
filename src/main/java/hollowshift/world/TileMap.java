package hollowshift.world;

import hollowshift.engine.Camera;
import hollowshift.engine.Window;
import hollowshift.engine.gfx.Art;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Grid of tiles with collision, line-of-sight and rendering helpers. */
public final class TileMap {
    public static final int TILE = 16;

    public final int width;
    public final int height;
    private final Tile[] tiles;
    private final byte[] variant;
    private static final int BLOCK = 16;
    private static final int BLOCK_PX = BLOCK * TILE;
    private final int blocksW;
    private final int blocksH;
    private final BufferedImage[] blocks;

    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        tiles = new Tile[width * height];
        variant = new byte[width * height];
        Arrays.fill(tiles, Tile.VOID);
        for (int i = 0; i < variant.length; i++) variant[i] = (byte) ((i * 2654435761L >>> 13) & 3);
        blocksW = (width + BLOCK - 1) / BLOCK;
        blocksH = (height + BLOCK - 1) / BLOCK;
        blocks = new BufferedImage[blocksW * blocksH];
    }

    public boolean inside(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < width && ty < height;
    }

    public Tile get(int tx, int ty) {
        return inside(tx, ty) ? tiles[ty * width + tx] : Tile.VOID;
    }

    public void set(int tx, int ty, Tile t) {
        if (!inside(tx, ty)) return;
        tiles[ty * width + tx] = t;
        // invalidate this block and the one above (wall faces depend on the tile below)
        blocks[(ty / BLOCK) * blocksW + tx / BLOCK] = null;
        if (ty > 0) blocks[((ty - 1) / BLOCK) * blocksW + tx / BLOCK] = null;
    }

    public boolean solid(int tx, int ty) {
        return get(tx, ty).solid;
    }

    public boolean solidAt(double wx, double wy) {
        return solid(toTile(wx), toTile(wy));
    }

    public Tile tileAt(double wx, double wy) {
        return get(toTile(wx), toTile(wy));
    }

    public static int toTile(double w) {
        return (int) Math.floor(w / TILE);
    }

    public static double center(int t) {
        return t * TILE + TILE / 2.0;
    }

    /** True if any solid tile overlaps the axis-aligned box. */
    public boolean boxBlocked(double x0, double y0, double x1, double y1) {
        int tx0 = toTile(x0), ty0 = toTile(y0), tx1 = toTile(x1 - 0.001), ty1 = toTile(y1 - 0.001);
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++)
                if (solid(tx, ty)) return true;
        return false;
    }

    /** Line of sight between two world points, sampled every 4 px against opaque tiles. */
    public boolean lineOfSight(double x0, double y0, double x1, double y1) {
        double d = Math.hypot(x1 - x0, y1 - y0);
        int steps = (int) Math.ceil(d / 4);
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            if (get(toTile(x0 + (x1 - x0) * t), toTile(y0 + (y1 - y0) * t)).opaque) return false;
        }
        return true;
    }

    /**
     * Draws the visible part of the map. Tiles never change during play, so the
     * map is baked lazily into 256x256 px blocks and only those blocks are blitted
     * (a handful of drawImage calls per frame instead of ~1000).
     */
    public void render(Graphics2D g, Camera cam, Art art) {
        int left = cam.left(), top = cam.top();
        int bx0 = Math.max(0, Math.floorDiv(left, BLOCK_PX)), by0 = Math.max(0, Math.floorDiv(top, BLOCK_PX));
        int bx1 = Math.min(blocksW - 1, Math.floorDiv(left + Window.WIDTH, BLOCK_PX));
        int by1 = Math.min(blocksH - 1, Math.floorDiv(top + Window.HEIGHT, BLOCK_PX));
        for (int by = by0; by <= by1; by++) {
            for (int bx = bx0; bx <= bx1; bx++) {
                int i = by * blocksW + bx;
                if (blocks[i] == null) blocks[i] = bake(bx, by, art);
                g.drawImage(blocks[i], bx * BLOCK_PX - left, by * BLOCK_PX - top, null);
            }
        }
    }

    private BufferedImage bake(int bx, int by, Art art) {
        BufferedImage img = new BufferedImage(BLOCK_PX, BLOCK_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        for (int ty = by * BLOCK; ty < Math.min(height, (by + 1) * BLOCK); ty++) {
            for (int tx = bx * BLOCK; tx < Math.min(width, (bx + 1) * BLOCK); tx++) {
                Tile t = tiles[ty * width + tx];
                int v = variant[ty * width + tx];
                String sprite = switch (t) {
                    case VOID -> null;
                    case WALL -> !get(tx, ty + 1).opaque ? "tile_wall_face_" + (v & 1) : "tile_wall_" + (v & 1);
                    case FLOOR, CRATE, PROP -> "tile_floor_" + v;
                    case MOSS -> "tile_moss_" + (v & 1);
                    case PLANK -> "tile_plank_" + (v & 1);
                };
                if (sprite == null) continue;
                int sx = (tx - bx * BLOCK) * TILE, sy = (ty - by * BLOCK) * TILE;
                g.drawImage(art.get(sprite), sx, sy, null);
                if (t == Tile.CRATE) g.drawImage(art.get("crate"), sx, sy, null);
            }
        }
        g.dispose();
        return img;
    }
}
