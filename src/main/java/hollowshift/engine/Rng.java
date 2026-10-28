package hollowshift.engine;

import java.util.List;
import java.util.Random;

/** Seeded random source so a world can be regenerated from its seed. */
public final class Rng {
    private final Random random;

    public Rng(long seed) {
        random = new Random(seed);
    }

    public static long seedFromText(String text) {
        String t = text.trim();
        if (t.isEmpty()) return System.nanoTime();
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            long h = 1125899906842597L;
            for (char c : t.toUpperCase().toCharArray()) h = 31 * h + c;
            return h;
        }
    }

    public double next() {
        return random.nextDouble();
    }

    public double range(double lo, double hi) {
        return lo + random.nextDouble() * (hi - lo);
    }

    /** Integer in [lo, hi). */
    public int range(int lo, int hi) {
        return hi <= lo ? lo : lo + random.nextInt(hi - lo);
    }

    public boolean chance(double p) {
        return random.nextDouble() < p;
    }

    public <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
    }

    public long nextLong() {
        return random.nextLong();
    }
}
