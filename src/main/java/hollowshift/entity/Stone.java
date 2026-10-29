package hollowshift.entity;

import hollowshift.ai.Noise;

/** Thrown to make noise somewhere else, or to stun the Tenant on a direct hit. */
public final class Stone extends Carryable implements Stashable {
    public static final double LAND_NOISE = 260;

    public Stone(double x, double y) {
        super(x, y, "stone");
    }

    @Override
    protected String sprite() {
        return "item_stone";
    }

    @Override
    public Supply supply() {
        return Supply.STONE;
    }

    @Override
    public String prompt(Player p) {
        return Stashable.prompt(p, Supply.STONE);
    }

    @Override
    public boolean canInteract(Player p) {
        return !isFlying() && p.pack().hasRoom(Supply.STONE);
    }

    @Override
    public void interact(Player p) {
        p.stash(this);
    }

    @Override
    public boolean throwable() {
        return true;
    }

    @Override
    protected void onLanded() {
        world.audio.playAt("stone_land", x, y, 520, 1, world.rng.range(0.9, 1.1));
        world.emitNoise(new Noise(x, y, LAND_NOISE, false));
    }

    @Override
    protected void onHitCreature(Creature c) {
        world.audio.playAt("stun", x, y, 520, 1, 1);
        c.stun();
        world.emitNoise(new Noise(x, y, 120, false));
    }
}
