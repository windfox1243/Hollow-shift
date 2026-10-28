package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.game.World;

/**
 * HEARING: any noise within its radius makes the Tenant investigate;
 * close noises (inside half the radius) from the player trigger a chase.
 */
public final class HearingSense implements Sense {
    @Override
    public void update(Creature self, World world, double dt) {
        for (Noise n : world.noises.thisTick()) {
            double d = Math.hypot(n.x() - self.x, n.y() - self.y);
            if (d > n.radius()) continue;
            boolean close = d < n.radius() / 2 && n.fromPlayer();
            self.perceive(new Stimulus(n.x(), n.y(), close ? 0.85 : 0.5, Stimulus.Kind.SOUND));
        }
    }
}
