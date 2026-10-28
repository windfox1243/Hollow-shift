package hollowshift.ai;

import hollowshift.entity.Creature;

/**
 * Strides hard at the last known position of the player (straight through walls).
 * Switches to grabbing when close, gives up after losing track.
 */
public final class ChaseState extends CreatureState {
    /** Roughly the original's aggro budget: it keeps coming for a couple of seconds after losing you. */
    private static final double LOSE_AFTER = 2.5;
    private double sinceSeen;

    /** Called whenever a strong stimulus confirms where the player is. */
    public void refresh() {
        sinceSeen = 0;
    }

    @Override
    public void enter(Creature c) {
        c.world().audio.playAt("creature_step_heavy", c.x, c.y, 700, 1, 0.8);
    }

    @Override
    public void update(Creature c, double dt) {
        sinceSeen += dt;
        if (c.distanceTo(c.world().player) < Creature.REACH_RANGE && sinceSeen < 0.5) {
            c.setState(new ReachState());
            return;
        }
        boolean arrived = c.walkTo(c.lastKnownX, c.lastKnownY, Gait.CHASE);
        if (sinceSeen > LOSE_AFTER || (arrived && sinceSeen > 0.8)) {
            c.setState(new InvestigateState(c.lastKnownX, c.lastKnownY));
        }
    }

    @Override
    public String name() {
        return "chase";
    }

    @Override
    public boolean isChase() {
        return true;
    }
}
