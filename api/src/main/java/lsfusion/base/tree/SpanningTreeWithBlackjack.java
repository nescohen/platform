package lsfusion.base.tree;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.prim.UndirectedGraph;

import java.util.*;

/**
 * Решает следующую задачу:
 * Дан ориентированный взвешенный граф G, в котором если есть ребро (i, j), то не существует ребра (j, i). Вершины также имеют вес.
 * Рассмотрим неориентированный граф G'', который получается из графа G заменой ориентированных ребер на неориентированные. 
 * В графе G'' нужно найти остовное дерево, на котором достигается максимум функции f, где f = SUM(v(i)), по всем вершинам i графа G,
 * где v(i) = MAX(weight(i), SUM(weight(j, i))), по всем j таким, что в графе G есть ребро (j, i) и соответствующее ребро в графе G'' попало в остовное дерево. 
 * <p>
 * Предполагается, что графы рассматриваются сильно разреженные.
 * <p>
 * Краткая идея алгоритма.
 * Выполняется перебор, который пытается удалять ребра графа, сохраняя его связность, до получения остовного дерева. 
 * Ребра рассматриваются в порядке неубывания весов.
 * Во время перебора поддерживается актуальное множество "мостов" графа, то есть ребер, удаление которых сделает граф несвязным.
 * На каждом шаге:
 * <ol><li>Если ребро не является мостом, удаляем его из графа, находим новые "мосты", и рекурсивно решаем задачу для оставшихся ребер графа.</li> 
 * <li>Оставляем ребро в графе, если это не приводит к появлению цикла, и рекурсивно решаем задачу для оставшихся ребер графа.</li></ol>
 * <p>
 * Так как перебор даже для небольших графов выполняется слишком долго, то количество итераций алгоритма ограничивается некой константой iterations.
 * Под итерацией алгоритма в данном случае подразумеваем количество удалений ребер из графа (и как следствие количество поисков "мостов" графа).
 * Также вводится некий коэффициент ratio. 
 * Перебор на исходном графе ограничивается количеством итераций равным iterations0 = iterations * ratio / 100.
 * Затем ребро с минимальным весов фиксируется как принадлежащее искомому дереву и запускается новый перебор на оставшихся ребрах
 * с количеством итераций iterations1 = (iterations - iterations0) * ratio / 100 и т.д.
 * 
 */

public class SpanningTreeWithBlackjack<T> {
    static final private int DEFAULT_ITERATIONS = 1000;  
    static final private int RATIO = 70;
    
    static private class Edge {
        public Edge(int from, int to, int w, boolean d) { this.from = from; this.to = to; this.weight = w; this.direct = d; }
        
        public int from;
        public int to;
        public int weight;
        public boolean direct;
    }
    
    private Map<T, Integer> nodeIndex = new HashMap<>();
    private List<Integer> weights = new ArrayList<>();
    private List<List<Edge>> graph = new ArrayList<>();
    private int nodesCnt;

    public void addNode(T node, int weight) {
        assert !nodeIndex.containsKey(node);
        nodeIndex.put(node, nodeIndex.size());
        weights.add(weight);
        graph.add(new ArrayList<>());
    }

    public void addEdge(T nodeFrom, T nodeTo, int weight) {
        assert nodeIndex.containsKey(nodeFrom) && nodeIndex.containsKey(nodeTo);
        int nodeFromIndex = nodeIndex.get(nodeFrom);
        int nodeToIndex = nodeIndex.get(nodeTo);
        addEdgeFrom(nodeFromIndex, new Edge(nodeFromIndex, nodeToIndex, weight, true));
        addEdgeFrom(nodeToIndex, new Edge(nodeToIndex, nodeFromIndex, weight, false));
    }
    
    void setNodeWeight(int nodeIndex, int weight) {
        weights.set(nodeIndex, weight);
    }
    
    private void addEdgeFrom(int nodeIndex, Edge addEdge) {
        List<Edge> edges = graph.get(nodeIndex);
        for (Edge edge : edges) {
            if (edge.from == addEdge.from && edge.to == addEdge.to) {
                assert edge.direct == addEdge.direct;
                edge.weight = BaseUtils.max(edge.weight, addEdge.weight);
                return;
            }
        }
        edges.add(addEdge);
    }
        
    private void getComponent(int node, boolean[] visited, List<Integer> outComponent) {
        if (!visited[node]) {
            visited[node] = true;
            outComponent.add(node);
            for (Edge e : graph.get(node)) {
                getComponent(e.to, visited, outComponent);
            }
        }
    }  
    
    private class BestTreeFinder {
        private int totalIterations;
        private ArrayList<Integer> component;
        private List<Edge> sortedEdges;
        private int curBridgesFinderIndex;
        
        // обошли все варианты
        private boolean searchCompleted;
        
        private int bestResult;
        private int curIteration;
        
        private int[] visitedColor = new int[nodesCnt];
        private int[] indicesForFindingBridges = new int[nodesCnt]; 
        
        public BestTreeFinder(ArrayList<Integer> component, List<Edge> edges, int iterations) {
            this.component = component;
            this.sortedEdges = edges;
            this.totalIterations = iterations;
        }

        // алгоритм нахождения мостов в графе за O(|E| + |V|)
        private int findBridges(int node, UndirectedGraph<Integer> graph, int[] indices, int prevNode, List<Pair<Integer, Integer>> newBridges, boolean[][] bridgeMatrix) {
            indices[node] = curBridgesFinderIndex;
            ++curBridgesFinderIndex;
            int minIndex = indices[node];
            
            for (int next : graph.edgesFrom(node).keySet()) {
                if (next != prevNode) {
                    if (indices[next] == 0) {
                        minIndex = Math.min(minIndex, findBridges(next, graph, indices, node, newBridges, bridgeMatrix));
                    }
                    minIndex = Math.min(minIndex, indices[next]);
                }
                
            }
            if (minIndex >= indices[node] && prevNode >= 0) {
                if (!bridgeMatrix[prevNode][node]) {
                    bridgeMatrix[prevNode][node] = true;
                    bridgeMatrix[node][prevNode] = true;
                    newBridges.add(new Pair<>(prevNode, node));
                }
            }
            return minIndex;
        } 
        
        private List<Pair<Integer, Integer>> findBridges(UndirectedGraph<Integer> graph, boolean[][] bridgeMatrix) {
            curBridgesFinderIndex = 1;
            Arrays.fill(indicesForFindingBridges, 0); // переиспользование коллекции для оптимизации
            List<Pair<Integer, Integer>> newBridges = new ArrayList<>();
            findBridges(component.get(0), graph, indicesForFindingBridges, -1, newBridges, bridgeMatrix);
            return newBridges;
        } 
        
        private boolean isAcyclicDfs(int node, int prev, UndirectedGraph<Integer> graph, int[] visitedColor) {
            visitedColor[node] =  2;
            for (int next : graph.edgesFrom(node).keySet()) {
                if (next != prev) {
                    if (visitedColor[next] == 2) {
                        return false;
                    }
                    if (visitedColor[next] == 0) {
                        if (!isAcyclicDfs(next, node, graph, visitedColor)) {
                            return false;
                        }
                    }
                }
            }
            visitedColor[node] = 1;
            return true;           
        }
        
        // параметром идет список ребер, ребра направленные
        private boolean graphIsAcyclic(UndirectedGraph<Integer> graph) {
            Arrays.fill(visitedColor, 0);
            for (int i : graph.getNodes()) {
                if (visitedColor[i] == 0) {
                    if (!isAcyclicDfs(i, -1, graph, visitedColor)) {
                        return false;        
                    }
                }
            }
            return true;
        }
        
        private int edgesCount(UndirectedGraph<Integer> graph) {
            int res = 0;
            for (Integer node : graph.getNodes()) {
                res += graph.edgesFrom(node).size();    
            }
            return res / 2;
        }
        
        private void find(List<Edge> edges, int index, int iterations, int[] curResults, int curResult, UndirectedGraph<Integer> edgesToAdd, UndirectedGraph<Integer> edgesWithoutToRemove, boolean[][] bridgeMatrix) {
            if (curResult <= bestResult)
                return;
            if (curIteration > iterations)
                return;
            
            if (edgesCount(edgesWithoutToRemove) == component.size() - 1) {
                bestResult = curResult;
                return;
            }
            
            Edge curEdge = edges.get(index);

            if (!bridgeMatrix[curEdge.from][curEdge.to]) {
                ++curIteration;
                // Пробуем удалить ребро
                removeEdge(edgesWithoutToRemove, curEdge);
                List<Pair<Integer, Integer>> newBridges = findBridges(edgesWithoutToRemove, bridgeMatrix);
                int oldRes = curResults[curEdge.to];
                int w = curEdge.weight;
                int newRes = Math.max(weights.get(curEdge.to), oldRes - w);
                curResults[curEdge.to] = newRes;
                find(edges, index + 1, iterations, curResults, curResult + newRes - oldRes, edgesToAdd, edgesWithoutToRemove, bridgeMatrix);
                curResults[curEdge.to] = oldRes;
                for (Pair<Integer, Integer> pair : newBridges) {
                    bridgeMatrix[pair.first][pair.second] = false;
                    bridgeMatrix[pair.second][pair.first] = false;
                }
                addEdge(edgesWithoutToRemove, curEdge);
            }
            
            // Пробуем оставить ребро
            addEdge(edgesToAdd, curEdge);
            if (bridgeMatrix[curEdge.from][curEdge.to] || graphIsAcyclic(edgesToAdd)) { // такое условие для оптимизации
                find(edges, index + 1, iterations, curResults, curResult, edgesToAdd, edgesWithoutToRemove, bridgeMatrix);                
            }
            removeEdge(edgesToAdd, curEdge);
        }
        
        public int[] initStartValues(List<Integer> component, Collection<Edge> edges) {
            int[] result = new int[nodesCnt];
            for (Edge e : edges) {
                result[e.to] += e.weight; 
            }
            for (int node : component) {
                result[node] = Math.max(result[node], weights.get(node));
            }
            return result;
        }
        
        private UndirectedGraph<Integer> createUndirectedGraph(List<Edge> edges) {
            UndirectedGraph<Integer> graph = new UndirectedGraph<>();
            for (Edge e : edges) {
                addEdge(graph, e);
            }
            if (edges.isEmpty()) {
                for (int node : component) {
                    graph.addNode(node);
                }
            }
            return graph;
        }
        
        public int find() {
            // признак завершения полного перебора всех вариантов
            searchCompleted = false;
            
            // находим нижнюю границу ответа
            bestResult = 0;
            for (Integer nodeIndex : component) {
                bestResult += weights.get(nodeIndex);
            }
            
            int iterations = totalIterations;
            List<Edge> edges = new ArrayList<>(sortedEdges);
            
            int firstEdgeIndex = 0; // порядковый номер ребра, с которого мы начинаем перебор
            UndirectedGraph<Integer> edgesToAdd = new UndirectedGraph<>(); // подграф, содержащий ребра, принадлежащие остовному дереву
            UndirectedGraph<Integer> edgesWithoutToRemove = createUndirectedGraph(edges); // подграф, содержащий все ребра исходного графа за исключением удаленных из него
            boolean[][] bridgeMatrix = new boolean[nodesCnt][nodesCnt]; // признак, является ли ребро "мостом"
            findBridges(edgesWithoutToRemove, bridgeMatrix);
            
            while (iterations > 0 && !searchCompleted && firstEdgeIndex < edges.size()) {
                int localIterations = iterations * RATIO / 100;
                if (localIterations == 0) break;
                
                // находим начальные суммы на всем графе
                int[] initPoints = initStartValues(component, edges);
                int sumValue = 0;
                for (int value : initPoints) {
                    sumValue += value;
                }
                
                curIteration = 0;
                find(edges, firstEdgeIndex, localIterations, initPoints, sumValue, edgesToAdd, edgesWithoutToRemove, bridgeMatrix);
                
                iterations -= localIterations;
                // фиксируем в остовном дереве очередное ребрро
                addEdge(edgesToAdd, edges.get(firstEdgeIndex));
                if (!graphIsAcyclic(edgesToAdd)) {
//                    removeEdge(edgesToAdd, edges.get(firstEdgeIndex));
//                    removeEdge(edgesWithoutToRemove, edges.get(firstEdgeIndex));
                    // если ребра, которые мы оставляем в графе, образуют цикл, то пока прекращаем перебор. 
                    break;
                }
                ++firstEdgeIndex;
            }
            return bestResult;
        }
        
    }
    
    private int calculateComponent(ArrayList<Integer> component, int iterations) {
        // Строим список направленных ребер, отсортированный по невозрастанию веса 
        ArrayList<Edge> componentEdges = new ArrayList<>();
        for (Integer nodeIndex : component) {
            for (Edge e : graph.get(nodeIndex)) {
                if (e.direct) {
                    componentEdges.add(e);
                }
            }
        }
        
        componentEdges.sort(Comparator.comparingInt(e -> e.weight));
        
        BestTreeFinder finder = new BestTreeFinder(component, componentEdges, iterations); 
        return finder.find();
    }
    
    public int calculate() {
        return calculate(DEFAULT_ITERATIONS);
    }

    public int calculate(int iterations) {
         //  Разбиваем граф на компоненты связности, для каждой компоненты решаем задачу отдельно 
        nodesCnt = graph.size();
        int result = 0;
        boolean[] visited = new boolean[nodesCnt];
        
        for (int i = 0; i < nodesCnt; ++i) {
            ArrayList<Integer> outComponent = new ArrayList<>();
            if (!visited[i]) {
                getComponent(i, visited, outComponent);
                result += calculateComponent(outComponent, iterations);
            }
        }
        return result;
    }

    static private void addEdge(UndirectedGraph<Integer> graph, Edge e) {
        graph.addNode(e.from);
        graph.addNode(e.to);
        graph.addEdge(e.from, e.to, 1);
    }
    
    static private void removeEdge(UndirectedGraph<Integer> graph, Edge e) {
        graph.removeEdge(e.from, e.to);
        if (graph.edgesFrom(e.from).isEmpty()) {
            graph.removeNode(e.from);    
        }
        if (graph.edgesFrom(e.to).isEmpty()) {
            graph.removeNode(e.to);
        }
    }
}
