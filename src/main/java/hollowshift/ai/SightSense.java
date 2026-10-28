package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.entity.Flare;
import hollowshift.entity.Player;
import hollowshift.game.World;

/**
 * SIGHT, tuned like the original monster: no line-of-sight check (it sees
 * through rock just as it walks through it), range = brightness at the player
 * times 280 px to notice and 525 px to keep chasing (the original's 370/700 in
 * its 320 px rooms, scaled to our 240 px rooms). Lit flares act as lures.
 */
public final class SightSense implements Sense {
    private double lureCooldown;

    @Override
    public void update(Creature self, World world, double dt) {
        Player p = world.player;
        double vis = world.visibilityAt(p.x, p.y - 6, true);
        double range = (self.isChasing() ? 525 : 280) * vis;
        if (vis > 0.05 && self.distanceTo(p) < range) {
            self.perceive(new Stimulus(p.x, p.y, 1.0, Stimulus.Kind.SIGHT));
            return;
        }
        lureCooldown -= dt;
        if (lureCooldown > 0 || self.isChasing()) return;
        lureCooldown = 1.0;
        for (Flare f : world.entitiesOf(Flare.class)) {
            if (!f.isLit() || f.isHeld()) continue;
            if (self.distanceTo(f) < 280) {
                self.perceive(new Stimulus(f.x, f.y, 0.45, Stimulus.Kind.LURE));
                return;
            }
        }
    }
}
