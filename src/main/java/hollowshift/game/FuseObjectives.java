package hollowshift.game;

import hollowshift.engine.Lang;
import java.util.List;

/** Normal shift: install a fuse in every box, then return to the lift. */
public final class FuseObjectives extends Objectives {
    private final int needed;
    private int installed;

    public FuseObjectives(int needed) {
        this.needed = needed;
    }

    @Override
    public void onFuseInstalled() {
        installed++;
        if (installed >= needed) {
            world.toast(Lang.t("toast.restored"));
            world.audio.play("elevator_bell");
        } else {
            world.toast(Lang.t("toast.box", installed, needed));
        }
    }

    public int installed() {
        return installed;
    }

    @Override
    public boolean exitOpen() {
        return installed >= needed;
    }

    @Override
    public String exitLockedReason() {
        return Lang.t("obj.locked", installed, needed);
    }

    @Override
    public List<String> lines() {
        if (installed >= needed) return List.of(Lang.t("obj.restored"), Lang.t("obj.return"));
        return List.of(Lang.t("obj.fuses", installed, needed), Lang.t("obj.then"));
    }
}
