package hollowshift.entity;

import hollowshift.engine.Lang;
import hollowshift.ai.Noise;

/** Restores food (HUNGER rule). Opening a can is not quiet. */
public final class FoodCan extends Consumable {
    public FoodCan(double x, double y) {
        super(x, y, "food");
    }

    @Override
    protected String sprite() {
        return "item_food";
    }

    @Override
    public String prompt(Player p) {
        return Lang.t("prompt.eat");
    }

    @Override
    protected void consume(Player p) {
        p.food = Math.min(1, p.food + 0.45);
        world.audio.play("eat");
        world.emitNoise(new Noise(x, y, 120, true));
    }
}
