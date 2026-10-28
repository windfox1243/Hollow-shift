package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.entity.Player;

/** Close enough to grab: stops striding and swings its hands at the player. */
public final class ReachState extends CreatureState {
    private double sinceSeen;

    public void refresh() {
        sinceSeen = 0;
    }

    @Override
    public void enter(Creature c) {
        c.halt();
    }

    @Override
    public void update(Creature c, double dt) {
        sinceSeen += dt;
        Player p = c.world().player;
        if (c.distanceTo(p) > Creature.REACH_RANGE * 1.2) {
            ChaseState chase = new ChaseState();
            c.setState(chase);
            if (sinceSeen < 0.5) chase.refresh();
            return;
        }
        c.lastKnownX = p.x;
        c.lastKnownY = p.y;
        if (!c.handsBusy()) c.lunge(p.x, p.y - 6);
    }

    @Override
    public String name() {
        return "reach";
    }

    @Override
    public boolean isChase() {
        return true;
    }
}
