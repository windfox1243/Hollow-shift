package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.entity.Player;
import hollowshift.world.TileMap;
import hollowshift.world.WorldGenerator;

import java.util.ArrayList;
import java.util.List;

/** ECHO travel: fades out, reappears in a doorway of the room the noise came from, then investigates it. */
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

    /**
     * It comes in through one of the room's doorways, never out of thin air in the middle of
     * the room: the doorway farthest from the player is likeliest, and never one right beside them.
     */
    private double[] findSpot(Creature c) {
        Player p = c.world().player;
        List<double[]> doors = new ArrayList<>(WorldGenerator.doorways(c.world().map, nx, ny));
        doors.removeIf(d -> p.distanceTo(d[0], d[1]) < 80);
        if (!doors.isEmpty()) {
            double total = 0;
            for (double[] d : doors) total += p.distanceTo(d[0], d[1]);
            double roll = c.world().rng.range(0, total);
            for (double[] d : doors) {
                roll -= p.distanceTo(d[0], d[1]);
                if (roll <= 0) return d;
            }
            return doors.get(doors.size() - 1);
        }
        // no usable doorway (the player stands in it): an open spot near the noise, away from the player
        TileMap map = c.world().map;
        for (int i = 0; i < 40; i++) {
            double a = c.world().rng.range(0, Math.PI * 2);
            double r = c.world().rng.range(60, 110);
            double x = nx + Math.cos(a) * r, y = ny + Math.sin(a) * r;
            if (!map.boxBlocked(x - 7, y - 4, x + 7, y + 4) && p.distanceTo(x, y) > 70) return new double[]{x, y};
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
