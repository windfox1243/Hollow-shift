package hollowshift.rules;

import hollowshift.engine.Lang;
import hollowshift.entity.Player;
import hollowshift.game.World;

import java.awt.Color;

/**
 * COLD: away from a fire the player chills. In the dark (lantern off or dead) it takes
 * about 40 seconds to freeze; the lantern's little warmth stretches that to 80. Heaters,
 * lamps and flares warm you back up. Cold slows you down and tires you out.
 */
public final class ColdEffect implements StatusEffect {
    public static final double FREEZE_SECONDS_DARK = 40;
    public static final double FREEZE_SECONDS_LIT = 80;

    @Override
    public void update(Player p, World world, double dt) {
        double warmth = world.warmthAt(p.x, p.y);
        if (warmth > 0.3) {
            p.cold = Math.max(0, p.cold - dt * (0.05 + warmth * 0.15));
        } else {
            double seconds = p.lanternOn() ? FREEZE_SECONDS_LIT : FREEZE_SECONDS_DARK;
            p.cold = Math.min(1, p.cold + dt / seconds);
        }
        // stiff legs: up to 40% slower, and once badly chilled running tires you sooner
        p.speedFactor *= 1 - 0.4 * p.cold;
        if (p.isRunning() && p.cold > 0.5) p.stamina = Math.max(0, p.stamina - dt * 0.15 * (p.cold - 0.5) * 2);
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
