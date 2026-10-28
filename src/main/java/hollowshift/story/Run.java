package hollowshift.story;

/** The current play-through: story or endless, which shift, and how levels are seeded. */
public final class Run {
    public enum Mode { STORY, ENDLESS }

    public final Mode mode;
    public final long endlessSeed;
    public final String seedText;
    private int shift;
    private long attemptSeed = System.nanoTime();

    private Run(Mode mode, int shift, long endlessSeed, String seedText) {
        this.mode = mode;
        this.shift = shift;
        this.endlessSeed = endlessSeed;
        this.seedText = seedText;
    }

    public static Run story(int shift) {
        return new Run(Mode.STORY, shift, 0, "");
    }

    public static Run endless(long seed, String seedText) {
        return new Run(Mode.ENDLESS, 1, seed, seedText);
    }

    public int shift() {
        return shift;
    }

    public ShiftDef def(Campaign c) {
        return mode == Mode.STORY ? c.shift(shift) : c.endless(shift, endlessSeed);
    }

    /** Endless levels are fully determined by the seed; story levels change on every attempt. */
    public long levelSeed() {
        return mode == Mode.ENDLESS ? endlessSeed * 1_000_003L + shift : attemptSeed;
    }

    public void newAttempt() {
        attemptSeed = System.nanoTime();
    }

    public boolean isLastStoryShift(Campaign c) {
        return mode == Mode.STORY && shift >= c.shifts().size();
    }

    public void advance() {
        shift++;
        newAttempt();
    }
}
