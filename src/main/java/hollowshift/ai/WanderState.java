package hollowshift.ai;

import hollowshift.entity.Creature;

/**
 * Roams from room to room in a straight line (through walls), pausing to listen in between.
 * It does not head for the player's own room: finding them is what the senses are for.
 */
public final class WanderState extends CreatureState {
    /** Room centres closer than this to the player are not picked as roaming goals. */
    public static final double AVOID_PLAYER = 260;
    private double[] goal;
    private double pause;

    @Override
    public void enter(Creature c) {
        goal = c.world().randomRoomPointAway(AVOID_PLAYER);
    }

    @Override
    public void update(Creature c, double dt) {
        if (pause > 0) {
            pause -= dt;
            if (pause <= 0) goal = c.world().randomRoomPointAway(AVOID_PLAYER);
            return;
        }
        if (c.walkTo(goal[0], goal[1], Gait.ROAM)) {
            c.halt();
            pause = c.world().rng.range(1.0, 3.5);
        }
    }

    @Override
    public String name() {
        return "wander";
    }
}
