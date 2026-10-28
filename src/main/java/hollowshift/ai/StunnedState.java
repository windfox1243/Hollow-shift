package hollowshift.ai;

import hollowshift.entity.Creature;

/** Reeling after being hit by a stone. */
public final class StunnedState extends CreatureState {
    private double t = 2.0;

    @Override
    public void update(Creature c, double dt) {
        t -= dt;
        if (t <= 0) c.setState(new InvestigateState(c.world().player.x, c.world().player.y));
    }

    @Override
    public String name() {
        return "stunned";
    }

    @Override
    public boolean isBusy() {
        return true;
    }
}
