package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;
import hollowshift.game.DialPanel;
import hollowshift.game.Job;

import java.awt.Graphics2D;

/**
 * The old telephone to the surface. It only gets through on the shaft's own number, chalked on
 * the walls a digit at a time; once it does it rings through the whole mine: the lift crew
 * upstairs are told you are ready, and so is everything down here.
 */
public final class Phone extends Fixture implements Interactable, Beacon {
    public static final double RING_NOISE = 600;
    private final String code;
    private boolean called;

    public Phone(double x, double y, String code) {
        super(x, y);
        this.code = code;
    }

    /** The number that gets through, e.g. "407". */
    public String code() {
        return code;
    }

    public boolean isCalled() {
        return called;
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
    public void interact(Player p) {
        world.audio.play("grab", hollowshift.engine.audio.Bus.SFX, 0.6, 0.8);
        world.openPanel(new DialPanel(world, this));
    }

    /** The right number was dialled. */
    public void connect() {
        if (called) return;
        called = true;
        world.audio.playAt("elevator_bell", x, y, 2000, 1, 1.4);
        world.audio.playAt("elevator_bell", x, y, 2000, 0.8, 1.2);
        world.emitNoise(new Noise(x, y, RING_NOISE, false));
        // nobody answers. The ringing is heard all the same
        for (Creature c : world.creatures()) c.alarm(x, y);
        world.objectives().onJobDone(Job.PHONE);
        world.toast(Lang.t("toast.phoneAnswered"));
    }

    @Override
    public void renderSignal(Graphics2D g, int ox, int oy) {
        SignalLamp.render(g, x + 4 - ox, y - 12 - oy, !called, world.time, (long) (x * 7 + y));
    }
}
