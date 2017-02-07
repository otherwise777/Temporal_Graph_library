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
public class    SSSTPClosenessSingleNode<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,Double> {

    private final K srcVertexId;
    private final Integer maxIterations;
    private final Integer method;
    private final boolean Normalized;

    public SSSTPClosenessSingleNode(K srcVertexId, Integer maxIterations, Integer method, boolean Normalized) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
        this.method = method;
        this.Normalized = Normalized;
    }

    public Double run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        Double result;
//       if chosen method is EAT
        if(method == 1) {
            result = (Double) input.run(new SingleSourceShortestTemporalPathEAT<K, EV>(srcVertexId, maxIterations))
                    .map(new MapVertexForSummation()).reduce(new ReduceCloseness()).collect().get(0);
//            if chosen method i SP
        } else {
            result = (Double) input.run(new SingleSourceShortestTemporalPathSTT<K, EV>(srcVertexId, maxIterations))
                    .map(new MapVertexForSummation()).reduce(new ReduceCloseness()).collect().get(0);
        }
        if(Normalized) {
            Long nodecount = input.numberOfVertices() - 1;
            return result / nodecount;
        } else {
            return result;
        }
    }
    public static final class ReduceCloseness<K>	implements ReduceFunction<Double> {
        @Override
        public Double reduce(Double t1, Double t2) throws Exception {
            return t1 + t2;
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
