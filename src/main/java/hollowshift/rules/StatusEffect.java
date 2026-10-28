package hollowshift.rules;

import hollowshift.entity.Player;
import hollowshift.game.World;

import java.awt.Color;

/** A survival rule applied to the player every tick, shown as a HUD bar (Strategy pattern). */
public interface StatusEffect {
    void update(Player p, World world, double dt);

    String label();

    /** 0..1 fill of the HUD bar. */
    double value(Player p);

    Color color();

    /** True when the bar should flash as a warning. */
    boolean critical(Player p);
}
