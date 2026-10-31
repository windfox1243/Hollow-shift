package hollowshift.ai;

import hollowshift.entity.Creature;

/** Roams from room to room in a straight line (through walls), pausing to listen in between. */
public final class WanderState extends CreatureState {
    private double[] goal;
    private double pause;

    @Override
    public void enter(Creature c) {
        goal = c.world().randomRoomPoint();
    }

    @Override
    public void update(Creature c, double dt) {
        if (pause > 0) {
            pause -= dt;
            if (pause <= 0) goal = c.world().randomRoomPoint();
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
