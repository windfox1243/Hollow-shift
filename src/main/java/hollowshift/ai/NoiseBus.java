package hollowshift.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects noises emitted during one update tick. Listeners (the Tenant's
 * hearing and echo senses, the HUD) read them before {@link #clear()} is called.
 */
public final class NoiseBus {
    private final List<Noise> current = new ArrayList<>();
    private double loudestRecent;

    public void emit(Noise n) {
        current.add(n);
        if (n.fromPlayer()) loudestRecent = Math.max(loudestRecent, n.radius());
    }

    public List<Noise> thisTick() {
        return current;
    }

    public void clear() {
        current.clear();
    }

    /** Decaying "how loud have you been" value for the HUD meter, in px of radius. */
    public double loudestRecent() {
        return loudestRecent;
    }

    public void decay(double dt) {
        loudestRecent = Math.max(0, loudestRecent - 260 * dt);
    }
}
