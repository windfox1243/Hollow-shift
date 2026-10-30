package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.game.World;

/** ECHO: a loud noise anywhere pulls the Tenant to it (a short fade, then it comes in through a doorway of that room). */
public final class EchoSense implements Sense {
    private double cooldown;

    @Override
    public void update(Creature self, World world, double dt) {
        cooldown -= dt;
        if (cooldown > 0 || self.isChasing() || self.isBusy()) return;
        for (Noise n : world.noises.thisTick()) {
            if (!n.loud()) continue;
            if (Math.hypot(n.x() - self.x, n.y() - self.y) < 200) continue;
            cooldown = 8;
            self.echoTo(n.x(), n.y());
            return;
        }
    }
}
