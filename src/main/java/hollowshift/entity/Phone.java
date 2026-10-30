package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;
import hollowshift.game.Job;

/**
 * The old crank telephone to the surface. Crank it long enough and it rings through the whole
 * mine: the lift crew upstairs are told you are ready, and so is everything down here.
 */
public final class Phone extends Fixture implements Interactable {
    public static final double RING_NOISE = 600;
    private boolean called;

    public Phone(double x, double y) {
        super(x, y);
    }

    @Override
    protected String sprite() {
        return called ? "phone_1" : "phone_0";
    }

    @Override
    public String prompt(Player p) {
        return called ? null : Lang.t("prompt.phone");
    }

    @Override
    public boolean canInteract(Player p) {
        return !called;
    }

    @Override
    public double holdTime() {
        return 4;
    }

    @Override
    public void interact(Player p) {
        called = true;
        world.audio.playAt("elevator_bell", x, y, 2000, 1, 1.4);
        world.audio.playAt("elevator_bell", x, y, 2000, 0.8, 1.2);
        world.emitNoise(new Noise(x, y, RING_NOISE, false));
        // nobody answers. The ringing is heard all the same
        for (Creature c : world.creatures()) c.alarm(x, y);
        world.objectives().onJobDone(Job.PHONE);
        world.toast(Lang.t("toast.phoneAnswered"));
    }
}
