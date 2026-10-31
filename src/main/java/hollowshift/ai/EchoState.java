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
    /** It never comes out of a doorway inside the screen around the player: 640x360, plus room for its tentacles. */
    public static final double MIN_DX = 380, MIN_DY = 240;
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
     * A doorway off the player's screen, the one nearest the noise: first
     * among the noise's room and the rooms next to it, then two rooms out. If the player is close
     * to every one of those doorways, the one farthest from them.
     */
    private double[] findSpot(Creature c) {
        double[] spot = findSpot(c, 1);
        return spot != null ? spot : findSpot(c, 2);
    }

    /** Null when no doorway within {@code rooms} rooms is far enough, unless {@code rooms} is the last ring tried. */
    private double[] findSpot(Creature c, int rooms) {
        Player p = c.world().player;
        TileMap map = c.world().map;
        double step = WorldGenerator.CHUNK * TileMap.TILE;
        List<double[]> doors = new ArrayList<>();
        for (int oy = -rooms; oy <= rooms; oy++) {
            for (int ox = -rooms; ox <= rooms; ox++) {
                if (Math.abs(ox) + Math.abs(oy) > rooms) continue;
                double x = nx + ox * step, y = ny + oy * step;
                if (x < 0 || y < 0 || x >= map.width * TileMap.TILE || y >= map.height * TileMap.TILE) continue;
                for (double[] d : WorldGenerator.doorways(map, x, y)) {
                    boolean dup = false;
                    for (double[] e : doors) dup |= Math.hypot(e[0] - d[0], e[1] - d[1]) < 1;
                    if (!dup) doors.add(d);
                }
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
            if (offScreen(p, d) && toNoise < bestD) {
                bestD = toNoise;
                best = d;
            }
        }
        if (best != null) return best;
        if (rooms < 2) return null;
        if (farthest != null) return farthest;
        return new double[]{c.x, c.y}; // not a room grid: stay put
    }

    /** True when a point lies outside the screen centred on the player. */
    public static boolean offScreen(Player p, double[] d) {
        return Math.abs(d[0] - p.x) >= MIN_DX || Math.abs(d[1] - p.y) >= MIN_DY;
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
