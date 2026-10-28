package hollowshift.ai;

import hollowshift.entity.Creature;

/** One behaviour of the Tenant (State pattern). */
public abstract class CreatureState {
    public void enter(Creature c) {
    }

    public abstract void update(Creature c, double dt);

    /** Shown in the debug overlay. */
    public abstract String name();

    public boolean isChase() {
        return false;
    }

    /** States that ignore stimuli (stunned, travelling through an echo). */
    public boolean isBusy() {
        return false;
    }
}
