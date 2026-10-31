package hollowshift.ai;

/**
 * A sound event in the world. radius is how far (px) it carries. machine marks the jobs the
 * player sets off (fuse box surge, breaker, valve, generator): those are the only player noises
 * that count for ECHO.
 */
public record Noise(double x, double y, double radius, boolean fromPlayer, boolean machine) {
    public Noise(double x, double y, double radius, boolean fromPlayer) {
        this(x, y, radius, fromPlayer, false);
    }

    /** A machine the player has just worked: heard as the player's, and loud enough for ECHO. */
    public static Noise machine(double x, double y, double radius) {
        return new Noise(x, y, radius, true, true);
    }

    /**
     * Loud enough to trigger the ECHO rule: a stone landing somewhere else, or machinery.
     * The player's own footsteps and pick-ups never count, however far they carry, otherwise
     * running or picking up a fuse would teleport it right next to you.
     */
    public boolean loud() {
        return fromPlayer ? machine && radius >= 340 : radius >= 220;
    }
}
