package hollowshift.ai;

/** A sound event in the world. radius is how far (px) it carries. */
public record Noise(double x, double y, double radius, boolean fromPlayer) {
    /** Loud enough to trigger the ECHO rule. */
    public boolean loud() {
        return radius >= 220;
    }
}
