package hollowshift.entity;

import java.awt.Color;

/** An entity that lights its surroundings (and therefore exposes whoever stands in it). */
public interface LightSource {
    double lightX();

    double lightY();

    /** Radius in pixels; 0 when switched off. */
    double lightRadius();

    /** 0..1 */
    double lightIntensity();

    /** Optional colour wash; null for neutral light. */
    default Color lightTint() {
        return null;
    }

    /** Whether this light gives warmth against the COLD rule. */
    default boolean warms() {
        return true;
    }
}
