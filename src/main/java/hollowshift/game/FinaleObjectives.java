package hollowshift.game;

import hollowshift.engine.Lang;
import hollowshift.engine.audio.Bus;

import java.util.List;

/**
 * Final shift: pull every main breaker, then survive while the lift descends.
 * The Tenant gets faster once the countdown starts.
 */
public final class FinaleObjectives extends Objectives {
    private static final double LIFT_TIME = 45;
    private final int levers;
    private int pulled;
    private double countdown = -1;

    public FinaleObjectives(int levers) {
        this.levers = levers;
    }

    @Override
    public void onLeverPulled() {
        pulled++;
        if (pulled >= levers) {
            countdown = LIFT_TIME;
            world.toast(Lang.t("toast.liftcoming"));
        } else {
            world.toast(Lang.t("toast.breaker", pulled, levers));
        }
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        if (countdown > 0) {
            countdown -= dt;
            if (countdown <= 0) {
                countdown = 0;
                world.toast(Lang.t("toast.arrived"));
                world.audio.play("elevator_bell", Bus.SFX, 1, 1);
            }
        }
    }

    @Override
    public boolean exitOpen() {
        return countdown == 0;
    }

    @Override
    public String exitLockedReason() {
        if (pulled < levers) return Lang.t("obj.deadlift", pulled, levers);
        return Lang.t("obj.coming");
    }

    @Override
    public double huntMultiplier() {
        return countdown > 0 ? 1.15 : 1;
    }

    @Override
    public List<String> lines() {
        if (pulled < levers) return List.of(Lang.t("obj.levers", pulled, levers));
        if (countdown > 0) return List.of(Lang.t("obj.survive", (int) Math.ceil(countdown)));
        return List.of(Lang.t("obj.board"));
    }
}
