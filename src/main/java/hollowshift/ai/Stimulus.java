package hollowshift.ai;

/**
 * Something the Tenant perceived. strength >= {@link #CHASE} means it knows
 * where the player is and starts chasing; weaker stimuli make it investigate.
 */
public record Stimulus(double x, double y, double strength, Kind kind) {
    public static final double CHASE = 0.8;

    /** TRAP: the player touched its growth (CONSUME rule), so it knows exactly where they are. */
    public enum Kind { SIGHT, SOUND, SMELL, TRAP, LURE, TOUCH }

    public boolean chase() {
        return strength >= CHASE;
    }
}
