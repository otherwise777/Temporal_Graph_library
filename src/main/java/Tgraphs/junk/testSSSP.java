package Tgraphs.junk;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.GraphAlgorithm;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.spargel.*;


import org.apache.flink.api.common.functions.MapFunction;
        import org.apache.flink.api.java.DataSet;
        import org.apache.flink.graph.Edge;
        import org.apache.flink.graph.Graph;
        import org.apache.flink.graph.GraphAlgorithm;
        import org.apache.flink.graph.Vertex;
        import org.apache.flink.graph.spargel.GatherFunction;
        import org.apache.flink.graph.spargel.MessageIterator;
        import org.apache.flink.graph.spargel.ScatterFunction;

/**
 * This is an implementation of the Single-Source-Shortest Paths algorithm, using a scatter-gather iteration.
 */
@SuppressWarnings("serial")
public class testSSSP<K> implements GraphAlgorithm<K, Double, Double, DataSet<Vertex<K, Double>>> {

    private final K srcVertexId;
    private final Integer maxIterations;

    /**
     * Creates an instance of the SingleSourceShortestPaths algorithm.
     *
     * @param srcVertexId The ID of the source vertex.
     * @param maxIterations The maximum number of iterations to run.
     */
    public testSSSP(K srcVertexId, Integer maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }

    @Override
    public DataSet<Vertex<K, Double>> run(Graph<K, Double, Double> input) {

        ScatterGatherConfiguration parameters = new ScatterGatherConfiguration();
        parameters.setName("test SSSP");
//        parameters.setSolutionSetUnmanagedMemory(true);
//        parameters.setParallelism(3);
//        parameters.set?

        return input.mapVertices(new org.apache.flink.graph.library.SingleSourceShortestPaths.InitVerticesMapper<K>(srcVertexId))
                .runScatterGatherIteration(new MinDistanceMessenger<K>(), new VertexDistanceUpdater<K>(),
                        maxIterations, parameters).getVertices();
    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, Double>, Double> {

        private K srcVertexId;

        public InitVerticesMapper(K srcId) {
            this.srcVertexId = srcId;
        }

        public Double map(Vertex<K, Double> value) {
            if (value.f0.equals(srcVertexId)) {
                return 0.0;
            } else {
                return Double.POSITIVE_INFINITY;
            }
        }
    }

    /**
     * Distributes the minimum distance associated with a given vertex among all
     * the target vertices summed up with the edge's value.
     *
     * @param <K>
     */
    public static final class MinDistanceMessenger<K> extends ScatterFunction<K, Double, Double, Double> {

        @Override
        public void sendMessages(Vertex<K, Double> vertex) {

            if (vertex.getValue() < Double.POSITIVE_INFINITY) {
                for (Edge<K, Double> edge : getEdges()) {
                    sendMessageTo(edge.getTarget(), vertex.getValue() + edge.getValue());
                }
            }
        }
    }

    /**
     * Function that updates the value of a vertex by picking the minimum
     * distance from all incoming messages.
     *
     * @param <K>
     */
    public static final class VertexDistanceUpdater<K> extends GatherFunction<K, Double, Double> {

        @Override
        public void updateVertex(Vertex<K, Double> vertex,
                                 MessageIterator<Double> inMessages) {

            Double minDistance = Double.MAX_VALUE;

            for (double msg : inMessages) {
                if (msg < minDistance) {
                    minDistance = msg;
                }
            }

            if (vertex.getValue() > minDistance) {
                setNewVertexValue(minDistance);
            }
        }
    }
}
