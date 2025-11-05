package graph.topo;

import java.util.*;
/**
 * Kahn's algorithm for topological ordering with instrumentation.
 */
public class TopologicalSort {
    private final List<List<Condensation.Edge>> adj;
    // instrumentation
    public long pushes = 0;
    public long pops = 0;
    public long edgeRemovals = 0;
    public long runTimeNs = 0;

    public TopologicalSort(List<List<Condensation.Edge>> adj) {
        this.adj = adj;
    }

    /**
     * Returns topological order of nodes [0..n-1]. If there is a cycle, returns empty list.
     */
    public List<Integer> kahnOrder() {
        long start = System.nanoTime();
        int n = adj.size();
        int[] indeg = new int[n];
        for (int u = 0; u < n; u++) {
            for (Condensation.Edge e : adj.get(u)) indeg[e.to]++;
        }
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indeg[i] == 0) { q.add(i); pushes++; }
        List<Integer> order = new ArrayList<>();
        while (!q.isEmpty()) {
            int u = q.poll(); pops++;
            order.add(u);
            for (Condensation.Edge e : adj.get(u)) {
                edgeRemovals++;
                indeg[e.to]--;
                if (indeg[e.to] == 0) { q.add(e.to); pushes++; }
            }
        }
        runTimeNs = System.nanoTime() - start;
        if (order.size() != n) return Collections.emptyList(); // cycle detected
        return order;
    }
}
