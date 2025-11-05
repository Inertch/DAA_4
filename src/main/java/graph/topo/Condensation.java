package graph.topo;

import java.util.*;
/**
 * Build condensation graph from original graph and SCC mapping.
 * If multiple edges between same components exist, keep minimum weight.
 * Works with 0-based node ids.
 */
public class Condensation {
    public static class Edge {
        public final int from;
        public final int to;
        public final long weight;
        public Edge(int f, int t, long w) { from = f; to = t; weight = w; }
    }

    private final int compCount;
    private final List<List<Edge>> compAdj;
    private final int[] nodeToComp;

    /**
     * Build condensation DAG.
     * @param n original node count
     * @param edges list of original edges as (u,v,w)
     * @param sccs list of SCCs as lists of original nodes
     */
    public Condensation(int n, List<long[]> edges, List<List<Integer>> sccs) {
        // map node -> comp id
        nodeToComp = new int[n];
        Arrays.fill(nodeToComp, -1);
        for (int i = 0; i < sccs.size(); i++) {
            for (int v : sccs.get(i)) nodeToComp[v] = i;
        }
        compCount = sccs.size();
        // use map to collapse multi-edges and keep minimal weight
        Map<Long, Long> edgeMin = new HashMap<>(); // key = (from<<32)|to , value = weight
        for (long[] e : edges) {
            int u = (int) e[0];
            int v = (int) e[1];
            long w = e[2];
            int cu = nodeToComp[u];
            int cv = nodeToComp[v];
            if (cu == cv) continue; // intra-SCC; skip
            long key = (((long) cu) << 32) | (cv & 0xffffffffL);
            edgeMin.merge(key, w, Math::min);
        }
        compAdj = new ArrayList<>();
        for (int i = 0; i < compCount; i++) compAdj.add(new ArrayList<>());
        for (Map.Entry<Long, Long> en : edgeMin.entrySet()) {
            long key = en.getKey();
            int cu = (int) (key >> 32);
            int cv = (int) (key & 0xffffffffL);
            long w = en.getValue();
            compAdj.get(cu).add(new Edge(cu, cv, w));
        }
    }

    public int getCompCount() { return compCount; }
    public List<List<Edge>> getAdj() { return compAdj; }
    public int[] getNodeToComp() { return nodeToComp; }
}
