# Smart City Smart Campus Scheduling

## Project Overview
This repository implements a scheduling pipeline that combines two graph topics into a single practical case:
- Strongly Connected Components detection and compression (Tarjan).
- Topological ordering of the condensation DAG (Kahn).
- Single-source shortest paths and longest path (critical path) on the condensation DAG (topological DP).

It targets dependency graphs for city services and internal analytics where cycles must be detected and compressed before optimal DAG scheduling.

---

## Data Summary
All datasets are stored under `data`. Each JSON dataset includes:
- `directed`: boolean  
- `n`: number of nodes  
- `edges`: array of `{u, v, w}` (edge weights)  
- `source`: source vertex for DAG shortest/longest paths  
- `weight_model`: `"edge"` or `"node"` (this project uses edge weights)

Example datasets included:
- `data/tasks.json` — small sample (n=8) with one 3-node cycle and a 4-node chain.
- `data/tasks-large.json` — mixed example (n=12) with multiple SCCs and cross-edges.

When multiple original edges collapse to one condensation edge, the condensation keeps the minimal inter-component edge weight for shortest-path computations.

---

## Implementation
- Language: Java 11, built with Maven.
- Packages:
  - `graph.scc` — `TarjanSCC` (Tarjan algorithm; instrumentation counters).
  - `graph.topo` — `Condensation`, `TopologicalSort` (Kahn; counters).
  - `graph.dagsp` — `DagSP` (shortestFrom and longestFrom via topo DP; counters).
  - `app` — `Main` (JSON parsing, orchestration, printing, simple path expansion).
- Weight model: edge weights by default.
- Instrumentation: operation counters and `System.nanoTime()` timings for SCC, topo, DAG-SP, and longest-path phases.
- Path reconstruction: component-level parents are used to reconstruct paths. Expansion to original nodes appends component nodes deterministically (sorted by id).

---

## Sample Results
Example output for `data/tasks.json` (run printed by `app.Main`)
- n = 8, m = 7
- SCC count = 6 (largest SCC size = 3)
- Condensation nodes = 6, edges = 4
- Topo order (components) = [1,5,0,4,3,2] (component IDs depend on Tarjan order)
- Derived original task order = [0,4,1,2,3,5,6,7]
- Source node = 4 (component C5)
- Shortest path 4 → 5 → 6 → 7 distance = 8
- Critical path 4 → 5 → 6 → 7 length = 8

Instrumentation example
- SCC: nodeVisits=8 edgeExpl=7 timeNs≈358100
- Kahn: pushes=6 pops=6 edgeRemovals=4 timeNs≈51500
- DAG SP: relaxAttempts=3 relaxSuccesses=3 timeNs≈28900
- Longest: relaxAttempts=3 relaxSuccesses=3 timeNs≈8000

---

## Analysis and Recommendations
- Complexity: SCC detection and topo/DP algorithms are O(n + m); dense graphs increase edge processing time.
- Compress SCCs before DAG algorithms to avoid undefined behavior inside cycles and reduce problem size.
- For scheduling tasks with intrinsic durations prefer node-duration model; for transit/communication cost prefer edge-weight model.
- For reproducible component IDs, sort SCCs by minimum node id before building the condensation graph.
- For multi-node SCCs consider computing representative entry/exit nodes or internal optimal paths instead of listing all nodes during expansion.
- Export metrics to CSV to simplify per-dataset reporting and comparisons.
