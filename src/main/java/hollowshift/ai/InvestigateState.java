package hollowshift.ai;

import hollowshift.entity.Creature;

/** Strides to a point of interest, then shuffles around it for a while before giving up. */
public final class InvestigateState extends CreatureState {
    private double tx, ty;
    private double linger = -1;
    private double lookTimer;
    private double lookX, lookY;

    public InvestigateState(double x, double y) {
        tx = x;
        ty = y;
    }

    public double targetX() {
        return tx;
    }

    public double targetY() {
        return ty;
    }

    /** Re-targets without restarting the state. */
    public void retarget(double x, double y) {
        tx = x;
        ty = y;
        linger = -1;
    }

    @Override
    public void update(Creature c, double dt) {
        if (linger < 0) {
            if (c.walkTo(tx, ty, Gait.ROAM)) linger = c.world().rng.range(2.0, 4.0);
            return;
        }
        linger -= dt;
        lookTimer -= dt;
        if (lookTimer <= 0) {
            lookTimer = c.world().rng.range(1.0, 2.0);
            double a = c.world().rng.range(0, Math.PI * 2);
            lookX = tx + Math.cos(a) * 30;
            lookY = ty + Math.sin(a) * 30;
        }
        c.walkTo(lookX, lookY, Gait.ROAM);
        if (linger <= 0) c.setState(new WanderState());
    }

    @Override
    public String name() {
        return "investigate";
    }
}
