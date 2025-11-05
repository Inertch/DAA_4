package app;

import com.google.gson.Gson;
import graph.scc.TarjanSCC;
import graph.topo.Condensation;
import graph.topo.TopologicalSort;
import graph.dagsp.DagSP;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Main driver for SCC -> Condensation -> Topo -> DAG SP tasks.json.
 */
public class Main {

    public static class InputEdge { public int u, v; public long w; }
    public static class InputGraph {
        public boolean directed;
        public int n;
        public List<InputEdge> edges;
        public int source;
        public String weight_model;
    }

    public static void main(String[] args) {
        String defaultPath = "data/tasks.json.json";
        String path = (args.length > 0) ? args[0] : defaultPath;

        InputGraph ig;
        try {
            String json = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            ig = new Gson().fromJson(json, InputGraph.class);
        } catch (IOException e) {
            System.err.println("Failed to read input file: " + path);
            e.printStackTrace();
            return;
        }

        System.out.println("Graph n=" + ig.n + " edges=" + ig.edges.size() + " weight_model=" + ig.weight_model + " source=" + ig.source);

        // build adjacency list for SCC (ignore weights)
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < ig.n; i++) adj.add(new ArrayList<>());

        List<long[]> edgesArr = new ArrayList<>();
        for (InputEdge e : ig.edges) {
            adj.get(e.u).add(e.v);
            edgesArr.add(new long[]{e.u, e.v, e.w});
            if (!ig.directed) {
                adj.get(e.v).add(e.u);
                edgesArr.add(new long[]{e.v, e.u, e.w});
            }
        }

        // 1. SCC
        TarjanSCC tarjan = new TarjanSCC(adj);
        List<List<Integer>> sccs = tarjan.run();
        System.out.println("\nSCCs (count=" + sccs.size() + ") and sizes:");
        for (List<Integer> comp : sccs) System.out.println("  " + comp + " size=" + comp.size());
        System.out.printf("SCC instrumentation: nodeVisits=%d edgeExpl=%d timeNs=%d%n",
                tarjan.nodeVisits, tarjan.edgeExplorations, tarjan.runTimeNs);

        // 2. Condensation
        Condensation condensation = new Condensation(ig.n, edgesArr, sccs);
        List<List<Condensation.Edge>> compAdj = condensation.getAdj();
        int compCount = condensation.getCompCount();
        System.out.println("\nCondensation DAG: compCount=" + compCount);
        for (int i = 0; i < compAdj.size(); i++) {
            System.out.print("  C" + i + " -> ");
            List<String> outs = new ArrayList<>();
            for (Condensation.Edge e : compAdj.get(i)) outs.add("C" + e.to + "(w=" + e.weight + ")");
            System.out.println(String.join(", ", outs));
        }

        // 3. Topological order (Kahn)
        TopologicalSort topoSort = new TopologicalSort(compAdj);
        List<Integer> topoOrder = topoSort.kahnOrder();
        System.out.println("\nTopological order (components): " + topoOrder);
        System.out.printf("Kahn instrumentation: pushes=%d pops=%d edgeRemovals=%d timeNs=%d%n",
                topoSort.pushes, topoSort.pops, topoSort.edgeRemovals, topoSort.runTimeNs);

        // Expand to original tasks.json order: expand components in topo order; sort nodes inside each SCC ascending
        Map<Integer, List<Integer>> compToNodes = new HashMap<>();
        for (int i = 0; i < sccs.size(); i++) compToNodes.put(i, new ArrayList<>(sccs.get(i)));
        List<Integer> expandedOrder = new ArrayList<>();
        for (int cid : topoOrder) {
            List<Integer> nodes = new ArrayList<>(compToNodes.get(cid));
            Collections.sort(nodes);
            expandedOrder.addAll(nodes);
        }
        System.out.println("Derived order of original tasks.json after SCC compression: " + expandedOrder);

        // 4. DAG SP (on condensation). Find component of source.
        int[] nodeToComp = condensation.getNodeToComp();
        int compSource = nodeToComp[ig.source];
        System.out.println("\nSource node " + ig.source + " is in component C" + compSource);

        DagSP dagSp = new DagSP(compAdj);
        DagSP.Result spResult = dagSp.shortestFrom(compSource, topoOrder);
        System.out.printf("DAG SP instrumentation: relaxAttempts=%d relaxSuccesses=%d timeNs=%d%n",
                dagSp.relaxAttempts, dagSp.relaxSuccesses, dagSp.runTimeNs);

        System.out.println("\nShortest distances from source component:");
        for (int i = 0; i < spResult.dist.length; i++) {
            long d = spResult.dist[i];
            if (d >= Long.MAX_VALUE / 8) System.out.println("  C" + i + " = INF");
            else System.out.println("  C" + i + " = " + d);
        }

        // reconstruct one shortest path to the last reachable component (example)
        int lastReachable = -1;
        for (int i = 0; i < spResult.dist.length; i++) if (spResult.dist[i] < Long.MAX_VALUE / 8) lastReachable = i;
        if (lastReachable != -1) {
            List<Integer> compPath = spResult.reconstructPath(lastReachable);
            System.out.println("One shortest path (component-level) to C" + lastReachable + ": " + compPath);
            List<Integer> nodePath = new ArrayList<>();
            for (int c : compPath) {
                List<Integer> nodes = compToNodes.get(c);
                Collections.sort(nodes);
                nodePath.addAll(nodes);
            }
            System.out.println("Expanded path to original nodes: " + nodePath);
        }

        // Longest path DP
        DagSP dagLp = new DagSP(compAdj);
        DagSP.Result lpResult = dagLp.longestFrom(compSource, topoOrder);
        System.out.printf("%nDAG Longest instrumentation: relaxAttempts=%d relaxSuccesses=%d timeNs=%d%n",
                dagLp.relaxAttempts, dagLp.relaxSuccesses, dagLp.runTimeNs);

        long bestDist = Long.MIN_VALUE;
        int bestComp = -1;
        for (int i = 0; i < lpResult.dist.length; i++) {
            if (lpResult.dist[i] > bestDist) { bestDist = lpResult.dist[i]; bestComp = i; }
        }

        System.out.println("\nLongest distances from source component:");
        for (int i = 0; i < lpResult.dist.length; i++) {
            if (lpResult.dist[i] == Long.MIN_VALUE / 4) System.out.println("  C" + i + " = -INF");
            else System.out.println("  C" + i + " = " + lpResult.dist[i]);
        }

        if (bestComp != -1 && bestDist > Long.MIN_VALUE / 8) {
            List<Integer> compPath = lpResult.reconstructPath(bestComp);
            System.out.println("Critical path (component-level) ending at C" + bestComp + " length=" + bestDist + ": " + compPath);
            List<Integer> expandedPath = new ArrayList<>();
            for (int c : compPath) {
                List<Integer> nodes = compToNodes.get(c);
                Collections.sort(nodes);
                expandedPath.addAll(nodes);
            }
            System.out.println("Expanded critical path (original nodes): " + expandedPath);
        } else {
            System.out.println("No reachable nodes for longest path from source.");
        }
    }
}
