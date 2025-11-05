package graph.dagsp;

import graph.topo.Condensation;
import java.util.*;

/**
 * Single-source shortest paths and longest path in a DAG.
 * Assumes input adjacency list is a DAG. Works on condensation graph edges with weights.
 * Instrumentation: relaxAttempts, relaxSuccesses, runTimeNs.
 */
public class DagSP {
    private final List<List<Condensation.Edge>> adj;
    public long relaxAttempts = 0;
    public long relaxSuccesses = 0;
    public long runTimeNs = 0;

    public DagSP(List<List<Condensation.Edge>> adj) {
        this.adj = adj;
    }

    /**
     * Shortest distances from source (component index) and parent pointers for reconstruction.
     * Uses topological order provided (must be a topo order for the DAG).
     */
    public Result shortestFrom(int source, List<Integer> topoOrder) {
        long start = System.nanoTime();
        int n = adj.size();
        final long INF = Long.MAX_VALUE / 4;
        long[] dist = new long[n];
        int[] parent = new int[n];
        Arrays.fill(dist, INF);
        Arrays.fill(parent, -1);
        dist[source] = 0;
        // process nodes in topo order
        for (int u : topoOrder) {
            if (dist[u] == INF) continue;
            for (Condensation.Edge e : adj.get(u)) {
                relaxAttempts++;
                int v = e.to;
                long nd = dist[u] + e.weight;
                if (nd < dist[v]) {
                    dist[v] = nd;
                    parent[v] = u;
                    relaxSuccesses++;
                }
            }
        }
        runTimeNs = System.nanoTime() - start;
        return new Result(dist, parent);
    }

    /**
     * Longest path from source: DP maximizing distances.
     * Returns distances (negative INF for unreachable) and parents.
     */
    public Result longestFrom(int source, List<Integer> topoOrder) {
        long start = System.nanoTime();
        int n = adj.size();
        final long NEG_INF = Long.MIN_VALUE / 4;
        long[] dist = new long[n];
        int[] parent = new int[n];
        Arrays.fill(dist, NEG_INF);
        Arrays.fill(parent, -1);
        dist[source] = 0;
        for (int u : topoOrder) {
            if (dist[u] == NEG_INF) continue;
            for (Condensation.Edge e : adj.get(u)) {
                relaxAttempts++;
                int v = e.to;
                long nd = dist[u] + e.weight;
                if (nd > dist[v]) {
                    dist[v] = nd;
                    parent[v] = u;
                    relaxSuccesses++;
                }
            }
        }
        runTimeNs = System.nanoTime() - start;
        return new Result(dist, parent);
    }

    public static class Result {
        public final long[] dist;
        public final int[] parent;
        public Result(long[] d, int[] p) { dist = d; parent = p; }
        public List<Integer> reconstructPath(int target) {
            if (dist[target] == Long.MAX_VALUE / 4 || dist[target] == Long.MIN_VALUE / 4) return Collections.emptyList();
            LinkedList<Integer> path = new LinkedList<>();
            int cur = target;
            while (cur != -1) {
                path.addFirst(cur);
                cur = parent[cur];
            }
            return path;
        }
    }
}
