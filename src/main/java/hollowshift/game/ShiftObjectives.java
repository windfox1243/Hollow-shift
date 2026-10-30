package hollowshift.game;

import hollowshift.engine.Lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Normal shift: everything the lift needs before it will run. Power every fuse box, and in
 * later shifts also bleed the hydraulic valves and find the key to the lift gate.
 */
public final class ShiftObjectives extends Objectives {
    private final int fuses;
    private final int valves;
    private final boolean needsKey;
    private int installed;
    private int opened;
    private boolean hasKey;

    public ShiftObjectives(int fuses) {
        this(fuses, 0, false);
    }

    public ShiftObjectives(int fuses, int valves, boolean needsKey) {
        this.fuses = fuses;
        this.valves = valves;
        this.needsKey = needsKey;
    }

    @Override
    public void onFuseInstalled() {
        installed++;
        progress(Lang.t("toast.box", installed, fuses));
    }

    @Override
    public void onValveOpened() {
        opened++;
        progress(Lang.t("toast.valve", opened, valves));
    }

    @Override
    public void onKeyFound() {
        hasKey = true;
        progress(Lang.t("toast.key"));
    }

    private void progress(String message) {
        if (exitOpen()) {
            world.toast(Lang.t("toast.restored"));
            world.audio.play("elevator_bell");
        } else {
            world.toast(message);
        }
    }

    public int installed() {
        return installed;
    }

    /** Jobs still to do before the lift runs. */
    public int remaining() {
        return Math.max(0, fuses - installed) + Math.max(0, valves - opened) + (needsKey && !hasKey ? 1 : 0);
    }

    @Override
    public boolean exitOpen() {
        return remaining() == 0;
    }

    @Override
    public String exitLockedReason() {
        return Lang.t("obj.locked", remaining());
    }

    @Override
    public List<String> lines() {
        if (exitOpen()) return List.of(Lang.t("obj.restored"), Lang.t("obj.return"));
        List<String> out = new ArrayList<>();
        out.add(line(installed >= fuses, Lang.t("obj.fuses", installed, fuses)));
        if (valves > 0) out.add(line(opened >= valves, Lang.t("obj.valves", opened, valves)));
        if (needsKey) out.add(line(hasKey, Lang.t(hasKey ? "obj.keyFound" : "obj.key")));
        out.add(Lang.t("obj.then"));
        return out;
    }

    /** Unfinished jobs are marked with ">", done ones are dimmed. */
    private static String line(boolean done, String text) {
        return done ? "  " + text : "> " + text;
    }
}
