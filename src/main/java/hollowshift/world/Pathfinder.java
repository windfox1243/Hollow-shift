package hollowshift.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A* over the tile grid, 8-directional without cutting corners. Buffers are reused between searches. */
public final class Pathfinder {
    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};
    private static final float DIAG = 1.4142f;

    private final TileMap map;
    private final float[] g;
    private final int[] parent;
    private final int[] stamp;
    private final boolean[] closed;
    private int search;
    private int[] heap = new int[1024];
    private float[] heapF = new float[1024];
    private int heapSize;

    public Pathfinder(TileMap map) {
        this.map = map;
        int n = map.width * map.height;
        g = new float[n];
        parent = new int[n];
        stamp = new int[n];
        closed = new boolean[n];
    }

    /**
     * Returns tile-centre waypoints from (sx, sy) to (gx, gy) in world pixels, excluding the start.
     * Empty if unreachable or the search exceeds maxNodes.
     */
    public List<double[]> find(double sx, double sy, double gx, double gy, int maxNodes) {
        int stx = TileMap.toTile(sx), sty = TileMap.toTile(sy);
        int gtx = TileMap.toTile(gx), gty = TileMap.toTile(gy);
        if (map.solid(gtx, gty)) {
            int[] near = nearestOpen(gtx, gty);
            if (near == null) return List.of();
            gtx = near[0];
            gty = near[1];
        }
        if (map.solid(stx, sty) || (stx == gtx && sty == gty)) return List.of();
        search++;
        Arrays.fill(closed, false);
        heapSize = 0;
        int w = map.width;
        int startIdx = sty * w + stx, goal = gty * w + gtx;
        visit(startIdx, 0, -1);
        push(startIdx, heuristic(stx, sty, gtx, gty));
        int expanded = 0;
        while (heapSize > 0) {
            int cur = pop();
            if (closed[cur]) continue;
            if (cur == goal) return build(goal);
            closed[cur] = true;
            if (++expanded > maxNodes) return List.of();
            int cx = cur % w, cy = cur / w;
            for (int d = 0; d < 8; d++) {
                int nx = cx + DX[d], ny = cy + DY[d];
                if (map.solid(nx, ny)) continue;
                if (d >= 4 && (map.solid(cx + DX[d], cy) || map.solid(cx, cy + DY[d]))) continue;
                int ni = ny * w + nx;
                if (closed[ni]) continue;
                float cost = g[cur] + (d >= 4 ? DIAG : 1f);
                if (stamp[ni] != search || cost < g[ni]) {
                    visit(ni, cost, cur);
                    push(ni, cost + heuristic(nx, ny, gtx, gty));
                }
            }
        }
        return List.of();
    }

    private void visit(int i, float cost, int from) {
        stamp[i] = search;
        g[i] = cost;
        parent[i] = from;
    }

    private static float heuristic(int x, int y, int gx, int gy) {
        int dx = Math.abs(x - gx), dy = Math.abs(y - gy);
        return Math.max(dx, dy) + (DIAG - 1) * Math.min(dx, dy);
    }

    private List<double[]> build(int goal) {
        List<double[]> path = new ArrayList<>();
        int w = map.width;
        for (int i = goal; parent[i] != -1; i = parent[i]) path.add(new double[]{TileMap.center(i % w), TileMap.center(i / w)});
        Collections.reverse(path);
        return path;
    }

    private int[] nearestOpen(int tx, int ty) {
        for (int r = 1; r < 4; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++)
                    if (!map.solid(tx + dx, ty + dy)) return new int[]{tx + dx, ty + dy};
        return null;
    }

    // ---- binary min-heap keyed by f ---------------------------------------------

    private void push(int node, float f) {
        if (heapSize == heap.length) {
            heap = Arrays.copyOf(heap, heapSize * 2);
            heapF = Arrays.copyOf(heapF, heapSize * 2);
        }
        int i = heapSize++;
        while (i > 0) {
            int p = (i - 1) / 2;
            if (heapF[p] <= f) break;
            heap[i] = heap[p];
            heapF[i] = heapF[p];
            i = p;
        }
        heap[i] = node;
        heapF[i] = f;
    }

    private int pop() {
        int top = heap[0];
        int last = heap[--heapSize];
        float lf = heapF[heapSize];
        int i = 0;
        while (true) {
            int c = 2 * i + 1;
            if (c >= heapSize) break;
            if (c + 1 < heapSize && heapF[c + 1] < heapF[c]) c++;
            if (heapF[c] >= lf) break;
            heap[i] = heap[c];
            heapF[i] = heapF[c];
            i = c;
        }
        heap[i] = last;
        heapF[i] = lf;
        return top;
    }
}
