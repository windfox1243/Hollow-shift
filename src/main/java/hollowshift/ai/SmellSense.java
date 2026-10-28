package hollowshift.ai;

import hollowshift.engine.MathUtil;
import hollowshift.entity.Creature;
import hollowshift.entity.Player;
import hollowshift.game.World;

/**
 * SMELL: periodically sniffs and heads toward a rough guess of the player's
 * position. The guess (and the pause between sniffs) improves with proximity.
 * The sniff sound is the player's warning.
 */
public final class SmellSense implements Sense {
    private double timer = 3;

    @Override
    public void update(Creature self, World world, double dt) {
        // like the original, it only sniffs while it is not already on your trail,
        // so a scent never keeps a chase going on its own
        if (self.isChasing()) return;
        timer -= dt;
        if (timer > 0) return;
        Player p = world.player;
        double d = self.distanceTo(p);
        // as in the original: sniffs more often the closer it is, within 750 px (1000 in 320 px rooms)
        timer = MathUtil.clamp(d / 90, 1.5, 8);
        if (d > 750) return;
        world.audio.playAt("sniff", self.x, self.y, 650, 1.0, world.rng.range(0.85, 1.05));
        double err = d * 0.3;
        double gx = p.x + world.rng.range(-err, err);
        double gy = p.y + world.rng.range(-err, err);
        self.perceive(new Stimulus(gx, gy, d < 150 ? 0.9 : 0.55, Stimulus.Kind.SMELL));
    }
}
