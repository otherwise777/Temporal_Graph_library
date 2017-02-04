package Tgraphs.BenchmarkingClasses;

import Tgraphs.SingleSourceShortestTemporalPathEAT;
import Tgraphs.TGraphAlgorithm;
import Tgraphs.Tgraph;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.spargel.GatherFunction;
import org.apache.flink.graph.spargel.MessageIterator;
import org.apache.flink.graph.spargel.ScatterFunction;
import org.apache.flink.graph.spargel.ScatterGatherConfiguration;
import org.apache.flink.types.NullValue;

/**
 * Created by s133781 on 25-Jan-17.
 */
public class SSSTPLDT  {

    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        int maxIterations = 30;
        Integer sourceVertex = 1;

        DataSet<Tuple4<Integer, Integer, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Temporal_wiki_hyperlinks")
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Integer.class, Integer.class, Double.class, Double.class); // read the node IDs as Longs

        Tgraph<Integer, NullValue, NullValue, Double> temporalGraph = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles, env);

        DataSet<Vertex<Integer, Double>> result = temporalGraph.getGellyGraph().mapVertices(new InitVerticesMapper<Integer>(sourceVertex)).runScatterGatherIteration(
                new Signal(), new Collect(), maxIterations).getVertices();

        result.first(20).print();
    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, Double> {
        private K srcVertexId;
        public InitVerticesMapper(K srcId) {
            this.srcVertexId = srcId;
        }
        public Double map(Vertex<K, NullValue> value) throws Exception {
            if (value.f0.equals(srcVertexId)) {
                return 0.0;
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
    }

    private static final class Signal extends ScatterFunction<Integer, Double, Double, Tuple3<NullValue,Double,Double>> {

        @Override
        public void sendMessages(Vertex<Integer, Double> vertex) {
            if (vertex.getValue() > Double.NEGATIVE_INFINITY) {
                for (Edge<Integer, Tuple3<NullValue,Double,Double>> edge : getEdges()) {
                    if(edge.getValue().f2 <= vertex.getValue()) {
                        sendMessageTo(edge.getTarget(), edge.getValue().f1.doubleValue());
                    }
                }
            }
        }
    }

    private static final class Collect extends GatherFunction<Integer, Double, Double> {

        public void updateVertex(Vertex<Integer, Double> vertex, MessageIterator<Double> inMessages) {
            Double minDistance = Double.MIN_VALUE;
            for (Double msg : inMessages) {
                if (msg > minDistance) {
                    minDistance = msg;
                }
            }
            if (vertex.getValue() < minDistance) {
                setNewVertexValue(minDistance);
            }
        }
    }

}
