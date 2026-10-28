package hollowshift.ai;

/** A sound event in the world. radius is how far (px) it carries. */
public record Noise(double x, double y, double radius, boolean fromPlayer) {
    /**
     * Loud enough to trigger the ECHO rule: a stone landing somewhere else, or machinery
     * (fuse box surge, breaker). The player's own footsteps and pick-ups never count,
     * otherwise running would teleport it right next to you.
     */
    public boolean loud() {
        return fromPlayer ? radius >= 340 : radius >= 220;
    }
}
