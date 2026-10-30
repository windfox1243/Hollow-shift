package hollowshift.game;

/**
 * What a room was used for, which decides what was left in it. Each room template maps to a
 * theme; props are picked from the theme's lists, so a store room holds barrels and sacks, a
 * shrine holds graves and candles, a tunnel holds carts and rubble.
 */
public enum RoomTheme {
    LIFT(2, 3, 2, 4, 1,
            new String[]{"decor_sandbags", "decor_lockers", "decor_toolrack", "decor_table"},
            new String[]{"decor_helmet", "decor_lantern", "decor_papers", "decor_tin", "decor_boots"}),
    STORAGE(3, 5, 2, 4, 3,
            new String[]{"decor_barrel", "decor_drum", "decor_sack", "decor_chest", "decor_chest_open", "crate", "decor_shelf"},
            new String[]{"decor_rope", "decor_bolts", "decor_papers", "decor_coalbits_0"}),
    WORKSHOP(2, 4, 3, 5, 2,
            new String[]{"decor_anvil", "decor_toolrack", "decor_spool", "decor_drum", "decor_stove", "decor_tnt", "decor_wheelbarrow"},
            new String[]{"decor_hammer", "decor_shovel", "decor_bolts", "decor_chain", "decor_stain_1", "decor_coalbits_1", "decor_pick"}),
    QUARTERS(2, 4, 3, 5, 2,
            new String[]{"decor_lockers", "decor_table", "decor_bookcase", "decor_shelf", "decor_chest", "decor_stove"},
            new String[]{"decor_boots", "decor_helmet", "decor_tin", "decor_papers", "decor_candle", "decor_bottle", "decor_lantern", "decor_stool"}),
    SHRINE(2, 4, 3, 5, 3,
            new String[]{"decor_grave", "decor_cross", "decor_grave"},
            new String[]{"decor_candle", "decor_candle", "decor_bones", "decor_stain_0", "decor_papers", "decor_chain", "decor_vial"}),
    TUNNEL(2, 3, 3, 6, 2,
            new String[]{"decor_minecart", "decor_tipped", "decor_beam", "decor_coal", "decor_sandbags", "decor_wheelbarrow"},
            new String[]{"decor_rubble_0", "decor_rubble_1", "decor_rubble_2", "decor_rail", "decor_pick", "decor_shovel",
                    "decor_coalbits_0", "decor_helmet", "decor_bones"}),
    FLOODED(0, 2, 3, 5, 1,
            new String[]{"decor_drum", "decor_beam"},
            new String[]{"decor_puddle_0", "decor_puddle_1", "decor_shrooms", "decor_shrooms", "decor_rubble_3", "decor_bones"});

    /** How many heavy props, and how much clutter (min inclusive, max exclusive). */
    public final int heavyMin, heavyMax, flatMin, flatMax;
    /** Longest row of heavy props stood side by side along a wall. */
    public final int maxRun;
    public final String[] heavy, flat;

    RoomTheme(int heavyMin, int heavyMax, int flatMin, int flatMax, int maxRun, String[] heavy, String[] flat) {
        this.heavyMin = heavyMin;
        this.heavyMax = heavyMax;
        this.flatMin = flatMin;
        this.flatMax = flatMax;
        this.maxRun = maxRun;
        this.heavy = heavy;
        this.flat = flat;
    }

    /** The theme of a room template, by name. */
    public static RoomTheme of(String template) {
        if (template == null) return TUNNEL;
        return switch (template) {
            case "lift" -> LIFT;
            case "storage", "closet" -> STORAGE;
            case "workshop", "machine", "boiler" -> WORKSHOP;
            case "dormitory", "office", "hall" -> QUARTERS;
            case "chapel", "cells", "gallery" -> SHRINE;
            case "flooded", "cistern", "mossgarden" -> FLOODED;
            default -> TUNNEL; // mineshaft, collapse, cave, ruin, zigzag, pillars, cross
        };
    }
}
