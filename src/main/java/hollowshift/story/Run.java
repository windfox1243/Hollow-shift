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

    /**
     * The read notes the journal shows in this run. A story shift shows its own two notes and the
     * ones before them, never notes read on a later shift in another play-through, so replaying
     * shift 1 does not spoil what comes after.
     */
    public java.util.Set<Integer> journalNotes(java.util.Set<Integer> read) {
        if (mode != Mode.STORY) return read;
        java.util.Set<Integer> shown = new java.util.TreeSet<>();
        for (int n : read) if (n <= shift * 2) shown.add(n);
        return shown;
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
