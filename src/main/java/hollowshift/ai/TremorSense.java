package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.entity.Player;
import hollowshift.game.World;

/** TREMOR: feels the player running within range, through walls and on moss. */
public final class TremorSense implements Sense {
    private static final double RANGE = 320;

    @Override
    public void update(Creature self, World world, double dt) {
        Player p = world.player;
        if (!p.isRunning()) return;
        double d = self.distanceTo(p);
        if (d < RANGE) self.perceive(new Stimulus(p.x, p.y, d < RANGE / 2 ? 0.9 : 0.6, Stimulus.Kind.TREMOR));
    }
}
