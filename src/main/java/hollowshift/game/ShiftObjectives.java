package hollowshift.game;

import hollowshift.engine.Lang;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Normal shift: everything the lift needs before it will run. Always the fuse boxes, and from
 * the second shift on a mix drawn per run: valves, the gate key, the generator, the phone.
 */
public final class ShiftObjectives extends Objectives {
    private final Map<Job, Integer> needed = new EnumMap<>(Job.class);
    private final Map<Job, Integer> done = new EnumMap<>(Job.class);

    public ShiftObjectives(int fuses) {
        this(Map.of(Job.FUSE, fuses));
    }

    public ShiftObjectives(Map<Job, Integer> jobs) {
        needed.putAll(jobs);
    }

    public int needed(Job j) {
        return needed.getOrDefault(j, 0);
    }

    public int done(Job j) {
        return done.getOrDefault(j, 0);
    }

    @Override
    public void onJobDone(Job job) {
        done.merge(job, 1, Integer::sum);
        if (exitOpen()) {
            world.toast(Lang.t("toast.restored"));
            world.audio.play("elevator_bell");
        } else {
            world.toast(Lang.t("toast.job." + job.name(), done(job), needed(job)));
        }
    }

    /** Jobs still to do before the lift runs. */
    public int remaining() {
        int left = 0;
        for (Map.Entry<Job, Integer> e : needed.entrySet()) left += Math.max(0, e.getValue() - done(e.getKey()));
        return left;
    }

    @Override
    public boolean exitOpen() {
        return remaining() == 0;
    }

    @Override
    public String exitLockedReason() {
        return Lang.t("obj.locked", remaining());
    }

    /** Unfinished jobs are marked with ">", done ones are dimmed. */
    @Override
    public List<String> lines() {
        if (exitOpen()) return List.of(Lang.t("obj.restored"), Lang.t("obj.return"));
        List<String> out = new ArrayList<>();
        for (Job j : Job.values()) {
            int n = needed(j);
            if (n == 0) continue;
            boolean finished = done(j) >= n;
            out.add((finished ? "  " : "> ") + Lang.t("obj.job." + j.name(), Math.min(done(j), n), n));
        }
        out.add(Lang.t("obj.then"));
        return out;
    }
}
