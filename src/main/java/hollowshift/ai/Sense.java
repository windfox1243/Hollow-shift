package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.game.World;

/** One way the Tenant can perceive the player. Enabled per shift by the rules (Strategy pattern). */
public interface Sense {
    void update(Creature self, World world, double dt);
}
