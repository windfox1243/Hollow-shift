package hollowshift.entity;

import hollowshift.ai.Noise;
import hollowshift.engine.Lang;

/** The key to the lift gate, left somewhere deep by the last crew. */
public final class LiftKey extends Item {
    public LiftKey(double x, double y) {
        super(x, y, "key");
    }

    @Override
    protected String sprite() {
        return "item_key";
    }

    @Override
    public String prompt(Player p) {
        return Lang.t("prompt.pickup", displayName());
    }

    @Override
    public boolean canInteract(Player p) {
        return true;
    }

    @Override
    public void interact(Player p) {
        world.audio.play("grab", hollowshift.engine.audio.Bus.SFX, 0.8, 1.4);
        world.emitNoise(new Noise(x, y, Player.PICKUP_NOISE, true));
        world.objectives().onJobDone(hollowshift.game.Job.KEY);
        remove();
    }
}
