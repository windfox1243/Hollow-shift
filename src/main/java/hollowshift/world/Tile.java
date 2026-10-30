package hollowshift.world;

/** Tile kinds. Solid tiles block movement; opaque tiles also block line of sight. */
public enum Tile {
    VOID(true, true, false, '#'),
    WALL(true, true, false, '#'),
    FLOOR(false, false, false, '.'),
    MOSS(false, false, true, ','),
    PLANK(false, false, false, '='),
    CRATE(true, false, false, 'x'),
    /** Floor under a heavy prop (cart, barrel, shelf...): blocks movement, not sight. The prop draws itself. */
    PROP(true, false, false, '.');

    public final boolean solid;
    public final boolean opaque;
    /** Footsteps on this tile make no noise. */
    public final boolean quiet;
    public final char symbol;

    Tile(boolean solid, boolean opaque, boolean quiet, char symbol) {
        this.solid = solid;
        this.opaque = opaque;
        this.quiet = quiet;
        this.symbol = symbol;
    }

    public static Tile fromSymbol(char c) {
        return switch (c) {
            case '.' -> FLOOR;
            case ',' -> MOSS;
            case '=' -> PLANK;
            case 'x' -> CRATE;
            case '#' -> WALL;
            // markers stand on plain floor
            default -> FLOOR;
        };
    }
}
