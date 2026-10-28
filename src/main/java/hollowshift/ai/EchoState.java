package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.world.TileMap;

/** ECHO travel: fades out, reappears near the noise, then investigates it. */
public final class EchoState extends CreatureState {
    private static final double FADE = 0.6;
    private final double nx, ny;
    private double t;
    private boolean moved;

    public EchoState(double x, double y) {
        nx = x;
        ny = y;
    }

    @Override
    public void enter(Creature c) {
        c.world().audio.playAt("echo", c.x, c.y, 600, 1, 1);
    }

    @Override
    public void update(Creature c, double dt) {
        t += dt;
        if (!moved && t >= FADE) {
            moved = true;
            double[] spot = findSpot(c);
            c.placeAt(spot[0], spot[1]);
            c.world().audio.playAt("echo", c.x, c.y, 600, 1, 0.7);
        }
        c.alpha = t < FADE ? 1 - t / FADE : Math.min(1, (t - FADE) / FADE);
        if (t >= FADE * 2) {
            c.alpha = 1;
            c.setState(new InvestigateState(nx, ny));
        }
    }

    /** An open spot 60-110 px from the noise, so it does not land on top of the player. */
    private double[] findSpot(Creature c) {
        TileMap map = c.world().map;
        for (int i = 0; i < 40; i++) {
            double a = c.world().rng.range(0, Math.PI * 2);
            double r = c.world().rng.range(60, 110);
            double x = nx + Math.cos(a) * r, y = ny + Math.sin(a) * r;
            if (!map.boxBlocked(x - 7, y - 4, x + 7, y + 4) && c.world().player.distanceTo(x, y) > 70) return new double[]{x, y};
        }
        return new double[]{c.x, c.y};
    }

    @Override
    public String name() {
        return "echo";
    }

    @Override
    public boolean isBusy() {
        return true;
    }
}
