package hollowshift.entity;

import hollowshift.engine.Lang;

/** The small things that go in the miner's pockets. Each kind has its own pocket and a limit. */
public enum Supply {
    STONE("stone", "item_stone", 3),
    FLARE("flare", "item_flare", 2),
    BATTERY("battery", "item_battery", 2),
    FOOD("food", "item_food", 2);

    public final String key;
    public final String sprite;
    public final int max;

    Supply(String key, String sprite, int max) {
        this.key = key;
        this.sprite = sprite;
        this.max = max;
    }

    public String displayName() {
        return Lang.t("item." + key);
    }
}
