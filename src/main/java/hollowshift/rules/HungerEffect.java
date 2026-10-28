package hollowshift.rules;

import hollowshift.engine.Lang;
import hollowshift.entity.Player;
import hollowshift.game.World;

import java.awt.Color;

/** HUNGER: food runs out in about two minutes; below a quarter the player slows down. */
public final class HungerEffect implements StatusEffect {
    private static final double STARVE_SECONDS = 130;

    @Override
    public void update(Player p, World world, double dt) {
        double rate = p.isRunning() ? 1.6 : 1.0;
        p.food = Math.max(0, p.food - dt / STARVE_SECONDS * rate);
        if (p.food < 0.25) p.speedFactor *= 0.75 + p.food;
        if (p.food <= 0) world.kill(DeathCause.HUNGER);
    }

    @Override
    public String label() {
        return Lang.t("status.FOOD");
    }

    @Override
    public double value(Player p) {
        return p.food;
    }

    @Override
    public Color color() {
        return new Color(230, 110, 90);
    }

    @Override
    public boolean critical(Player p) {
        return p.food < 0.2;
    }
}
