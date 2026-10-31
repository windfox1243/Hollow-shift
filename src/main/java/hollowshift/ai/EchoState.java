package hollowshift.ai;

import hollowshift.entity.Creature;
import hollowshift.entity.Player;
import hollowshift.world.TileMap;
import hollowshift.world.WorldGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * ECHO travel: it fades into the rock, moves through it for a couple of seconds, then comes out
 * of a doorway near the noise and searches there. A low rumble from that doorway a second before
 * it emerges tells the player which way not to run. It never comes out close to the player, so
 * the loud jobs (fuse boxes, breakers, valves) leave time to get away.
 */
public final class EchoState extends CreatureState {
    private static final double FADE = 0.6;
    /** Seconds spent inside the rock, unseen, between fading out and coming back. */
    public static final double TRAVEL = 2.2;
    /** How long before it emerges the rumble starts. */
    private static final double WARNING = 1.0;
    /** It never comes out of a doorway closer than this to the player. */
    public static final double MIN_DISTANCE = 150;
    private final double nx, ny;
    private double t;
    private double[] spot;
    private boolean warned, moved;

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
        double out = FADE + TRAVEL;
        if (spot == null && t >= FADE) spot = findSpot(c);
        if (!warned && t >= out - WARNING) {
            warned = true;
            c.world().audio.playAt("creature_step_heavy", spot[0], spot[1], 700, 1, 0.5);
            c.world().camera.shakeFrom(spot[0], spot[1], 6, 0.6);
        }
        if (!moved && t >= out) {
            moved = true;
            c.placeAt(spot[0], spot[1]);
            c.world().audio.playAt("echo", c.x, c.y, 600, 1, 0.7);
        }
        c.alpha = t < FADE ? 1 - t / FADE : t < out ? 0 : Math.min(1, (t - out) / FADE);
        if (t >= out + FADE) {
            c.alpha = 1;
            c.setState(new InvestigateState(nx, ny));
        }
    }

    /**
     * A doorway of the room the noise came from, or of a room next to it, at least
     * {@link #MIN_DISTANCE} from the player; the one nearest the noise among those. If the player
     * is close to every doorway around, the one farthest from them.
     */
    private double[] findSpot(Creature c) {
        Player p = c.world().player;
        TileMap map = c.world().map;
        double step = WorldGenerator.CHUNK * TileMap.TILE;
        List<double[]> doors = new ArrayList<>();
        double[][] around = {{0, 0}, {step, 0}, {-step, 0}, {0, step}, {0, -step}};
        for (double[] o : around) {
            double x = nx + o[0], y = ny + o[1];
            if (x < 0 || y < 0 || x >= map.width * TileMap.TILE || y >= map.height * TileMap.TILE) continue;
            for (double[] d : WorldGenerator.doorways(map, x, y)) {
                boolean dup = false;
                for (double[] e : doors) dup |= Math.hypot(e[0] - d[0], e[1] - d[1]) < 1;
                if (!dup) doors.add(d);
            }
        }
        double[] best = null, farthest = null;
        double bestD = Double.MAX_VALUE, farD = -1;
        for (double[] d : doors) {
            double toPlayer = p.distanceTo(d[0], d[1]);
            if (toPlayer > farD) {
                farD = toPlayer;
                farthest = d;
            }
            double toNoise = Math.hypot(d[0] - nx, d[1] - ny);
            if (toPlayer >= MIN_DISTANCE && toNoise < bestD) {
                bestD = toNoise;
                best = d;
            }
        }
        if (best != null) return best;
        if (farthest != null) return farthest;
        return new double[]{c.x, c.y}; // not a room grid: stay put
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
