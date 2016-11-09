package Tgraphs;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.spargel.GatherFunction;
import org.apache.flink.graph.spargel.MessageIterator;
import org.apache.flink.graph.spargel.ScatterFunction;
import org.apache.flink.types.NullValue;

import java.util.ArrayList;

/**
 * Created by s133781 on 03-Nov-16.
 *
 * input:
 * Tgraph <K,Double,EV,Double>
 * output:
 * Dataset<Vertex<K,tuple2<Double,Arraylist<Double>>>>
 *
 */
public class SingleSourceShortestTemporalPathSTT<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,Double>>> {

    private final K srcVertexId;
    private final Integer maxIterations;
    private DataSet<Double> startedges;

    public SingleSourceShortestTemporalPathSTT(K srcVertexId, Integer maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        //Filling up the startingedges with distinct values of the starting times of the starting edges
        DataSet<Edge<K,Tuple3<EV,Double,Double>>> startedgesset = input.getGellyGraph().getEdges().filter(new Filterstartingedges(srcVertexId));
        startedges = startedgesset.map(new MapFunction<Edge<K, Tuple3<EV, Double, Double>>, Double>() {
            @Override
            public Double map(Edge<K, Tuple3<EV, Double, Double>> value) throws Exception {
                return value.getValue().f1;
            }
        }).distinct();



        startedges.print();
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).getVertices();

    }
    public class Filterstartingedges implements FilterFunction<Edge<K,Tuple3<EV,Double,Double>>> {
        private K srcVertexId;
        public Filterstartingedges(K srcVertexId) { this.srcVertexId = srcVertexId; }

        @Override
        public boolean filter(Edge<K, Tuple3<EV, Double, Double>> value) throws Exception {
            if(value.getSource().equals(srcVertexId)) {
                return true;
            } else {
                return false;
            }
        }
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
                return Double.MAX_VALUE;
            }
        }
    }


    /*
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
