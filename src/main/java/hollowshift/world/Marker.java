package hollowshift.world;

/**
 * A spot authored in a template (item, fuse box, lamp...), in tile coordinates.
 * depth is the number of rooms between this marker's room and the start room.
 */
public record Marker(char type, int tx, int ty, int depth, int cell) {
    public double x() {
        return TileMap.center(tx);
    }

    public double y() {
        return TileMap.center(ty);
    }
}
