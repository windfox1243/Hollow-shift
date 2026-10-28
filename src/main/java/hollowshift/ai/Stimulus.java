package hollowshift.ai;

/**
 * Something the Tenant perceived. strength >= {@link #CHASE} means it knows
 * where the player is and starts chasing; weaker stimuli make it investigate.
 */
public record Stimulus(double x, double y, double strength, Kind kind) {
    public static final double CHASE = 0.8;

    public enum Kind { SIGHT, SOUND, SMELL, TREMOR, LURE, TOUCH }

    public boolean chase() {
        return strength >= CHASE;
    }
}
