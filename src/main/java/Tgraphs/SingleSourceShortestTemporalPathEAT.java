package Tgraphs;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.spargel.GatherFunction;
import org.apache.flink.graph.spargel.MessageIterator;
import org.apache.flink.graph.spargel.ScatterFunction;
import org.apache.flink.graph.spargel.ScatterGatherConfiguration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.types.NullValue;

import java.util.ArrayList;

/**
 * Created by Wouter Ligtenberg on 03-Nov-16.
 *
 * This is an implementation of the Single-Source-Shortest-Temporal Paths EAT (earliest arrival time) algorithm
 * using a scatter gather iteration. It determines the shortest temporal EAT path from the source node to all other nodes
 * in the temporal graph.
 * Input:
 * Tgraph <K,Double,EV,Double>
 * Output:
 * Dataset<Vertex<K,Double>>
 *
 */
public class SingleSourceShortestTemporalPathEAT<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,Double>>> {

    private final K srcVertexId;
    private final int maxIterations;

    /**
    * Creates an instance of the SingleSourceShortestTemporalPathEAT algorithm
    * @param srcVertexId The ID of the source vertex.
    * @param maxIterations The maximum number of iterations to run.
    *
    */
    public SingleSourceShortestTemporalPathEAT(K srcVertexId, int maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }



    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        ScatterGatherConfiguration parameters = new ScatterGatherConfiguration();
        parameters.setName("SSSTP");
        parameters.setSolutionSetUnmanagedMemory(true);

        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations,parameters).getVertices();

    }

    /**
    * Initialize function for the vertices of the Tgraph, it transforms the vertices from NullValue to Double to be
    * used in the signal collect algorithm
    * */
    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, Double> {
        private K srcVertexId;
        public InitVerticesMapper(K srcId) {
            this.srcVertexId = srcId;
        }
        public Double map(Vertex<K, NullValue> value) throws Exception {
             if (value.f0.equals(srcVertexId)) {
                return 0.0;
            } else {
                return Double.MAX_VALUE;
            }
        }
    }


    /**
    * mindistance function from scatterfunction with:
    * K as K
    * VV as Double
    * Message as Tuple2<Double,ArrayList<K>>
    * EV as Tuple3<EV, Double, Double>
    *
    * checks if vertexvalue is < inf, then sends a message to neighboor vertexes
    * with end time and such
    * */
    private static final class MinDistanceMessengerforTuplewithpath<K,EV> extends ScatterFunction<K, Double, Double, Tuple3<EV, Double, Double>> {
        @Override

        public void sendMessages(Vertex<K, Double> vertex) {

            if (vertex.getValue() < Double.POSITIVE_INFINITY) { //Checks if it has been passed for the first time
                for (Edge<K, Tuple3<EV,Double,Double>> edge : getEdges()) {
                    if (edge.getValue().f1 >= vertex.getValue()) { //If starting time of the edge

                        sendMessageTo(edge.getTarget(), edge.getValue().f2.doubleValue());
                    }
                }
            }
        }
    }

    /**
     * Gatherfunction of the SSSTPEAT
     * checks all incomming messages and stores
     *
     * K as K
     * VV as Tuple2<Double,ArrayList<K>>
     * Message as Tuple2<Double,ArrayList<K>>
     *
     * @param <K>
     */
    private static final class VertexDistanceUpdaterwithpath<K> extends GatherFunction<K, Double, Double> {

        @Override
        public void updateVertex(Vertex<K, Double> vertex, MessageIterator<Double> inMessages) {

            Double minDistance = Double.MAX_VALUE;
            for (Double msg : inMessages) {
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
