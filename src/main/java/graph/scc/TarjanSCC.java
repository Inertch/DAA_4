package graph.scc;

import java.util.*;
/**
 * Tarjan's algorithm for Strongly Connected Components.
 * Instrumentation: nodeVisits, edgeExplorations, runTimeNs.
 */
public class TarjanSCC {
    private final List<List<Integer>> g;
    private final int n;

    // instrumentation
    public long nodeVisits = 0;
    public long edgeExplorations = 0;
    public long runTimeNs = 0;

    // algorithm state
    private int time = 0;
    private int[] disc;
    private int[] low;
    private boolean[] inStack;
    private Deque<Integer> stack;
    private final List<List<Integer>> sccs = new ArrayList<>();

    public TarjanSCC(List<List<Integer>> graph) {
        this.g = graph;
        this.n = graph.size();
    }

    public List<List<Integer>> run() {
        long start = System.nanoTime();
        disc = new int[n];
        Arrays.fill(disc, -1);
        low = new int[n];
        inStack = new boolean[n];
        stack = new ArrayDeque<>();

        for (int v = 0; v < n; v++) {
            if (disc[v] == -1) {
                dfs(v);
            }
        }
        runTimeNs = System.nanoTime() - start;
        return sccs;
    }

    private void dfs(int u) {
        disc[u] = low[u] = time++;
        nodeVisits++;
        stack.push(u);
        inStack[u] = true;

        for (int v : g.get(u)) {
            edgeExplorations++;
            if (disc[v] == -1) {
                dfs(v);
                low[u] = Math.min(low[u], low[v]);
            } else if (inStack[v]) {
                low[u] = Math.min(low[u], disc[v]);
            }
        }

        if (low[u] == disc[u]) {
            List<Integer> comp = new ArrayList<>();
            int w;
            do {
                w = stack.pop();
                inStack[w] = false;
                comp.add(w);
            } while (w != u);
            Collections.sort(comp);
            sccs.add(comp);
        }
    }
}
