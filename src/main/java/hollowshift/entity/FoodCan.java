package hollowshift.entity;

/** Canned food. Eating restores food (HUNGER rule) and gives a second wind; opening a can is not quiet. */
public final class FoodCan extends Consumable {
    public FoodCan(double x, double y) {
        super(x, y, "food");
    }

    @Override
    protected String sprite() {
        return "item_food";
    }

    @Override
    public Supply supply() {
        return Supply.FOOD;
    }
}
