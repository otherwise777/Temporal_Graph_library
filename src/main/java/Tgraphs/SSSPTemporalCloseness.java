package Tgraphs;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.operators.ReduceOperator;
import org.apache.flink.graph.Vertex;
import org.apache.flink.types.NullValue;

/**
 * Created by s133781 on 03-Nov-16.
 *
 */
public class SSSPTemporalCloseness<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,Double> {

    private final K srcVertexId;
    private final Integer maxIterations;
    private final Integer method;
    private final boolean Normalized;

    public SSSPTemporalCloseness(K srcVertexId, Integer maxIterations, Integer method, boolean Normalized) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
        this.method = method;
        this.Normalized = Normalized;
    }

    public Double run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        DataSet<Vertex<K,Double>> a = input.run(new SingleSourceShortestTemporalPathEAT<K, EV>(srcVertexId, maxIterations))
                .map(new MapVertexForSummation());
        DataSet<Vertex<K, Double>> b = a.reduce(new ReduceFunction<Vertex<K, Double>>() {
            @Override
            public Vertex<K, Double> reduce(Vertex<K, Double> kDoubleVertex, Vertex<K, Double> t1) throws Exception {
                return null;
            }
        });
        b.print();
        return null;
    }
    public static final class ReduceCloseness<K>	implements ReduceFunction<Vertex<K,Double>> {
        @Override
        public Vertex<K, Double> reduce(Vertex<K, Double> vertex1, Vertex<K, Double> vertex2) throws Exception {
            return new Vertex<>(vertex1.getId(), vertex1.getValue() + vertex2.getValue());
        }
    }
    /*
    * This mapping maps Double.MAX and 0 to 0, and the rest to it's inverse
    * */
    public static final class MapVertexForSummation<K>	implements MapFunction<Vertex<K, Double>, Double> {
        @Override
        public Double map(Vertex<K, Double> vertex) throws Exception {
            if(vertex.getValue() == 0D || vertex.getValue() == Double.MAX_VALUE) {
                return 0D;
            } else {
                return 1 / vertex.getValue();
            }
        }
    }
}
