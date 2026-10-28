package hollowshift.rules;

import hollowshift.engine.Lang;
import hollowshift.entity.Player;
import hollowshift.game.World;

import java.awt.Color;

/**
 * COLD: standing away from warm light chills the player; the lantern only
 * halves the rate. Heaters, lamps and flares warm you back up. Cold slows you down.
 */
public final class ColdEffect implements StatusEffect {
    private static final double FREEZE_SECONDS = 45;

    @Override
    public void update(Player p, World world, double dt) {
        double warmth = world.warmthAt(p.x, p.y);
        if (warmth > 0.3) {
            p.cold = Math.max(0, p.cold - dt * (0.05 + warmth * 0.15));
        } else {
            double rate = p.lanternOn() ? 0.5 : 1.0;
            p.cold = Math.min(1, p.cold + dt / FREEZE_SECONDS * rate);
        }
        p.speedFactor *= 1 - 0.4 * p.cold;
        if (p.cold >= 1) world.kill(DeathCause.COLD);
    }

    @Override
    public String label() {
        return Lang.t("status.WARM");
    }

    @Override
    public double value(Player p) {
        return 1 - p.cold;
    }

    @Override
    public Color color() {
        return new Color(120, 190, 255);
    }

    @Override
    public boolean critical(Player p) {
        return p.cold > 0.75;
    }
}
