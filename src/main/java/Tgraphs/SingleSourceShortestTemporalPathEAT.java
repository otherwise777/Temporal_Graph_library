package Tgraphs;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.spargel.GatherFunction;
import org.apache.flink.graph.spargel.MessageIterator;
import org.apache.flink.graph.spargel.ScatterFunction;


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
public class SingleSourceShortestTemporalPathEAT<K,EV> implements TGraphAlgorithm<K,Double,EV,Double,DataSet<Vertex<K,Tuple2<Double,ArrayList<K>>>>> {

    private final K srcVertexId;
    private final Integer maxIterations;

    public SingleSourceShortestTemporalPathEAT(K srcVertexId, Integer maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K, Tuple2<Double, ArrayList<K>>>> run(Tgraph<K, Double, EV, Double> input) throws Exception {
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).getVertices();

    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, Double>, Tuple2<Double, ArrayList<K>>> {

        private K srcVertexId;

        public InitVerticesMapper(K srcId) {
            this.srcVertexId = srcId;
        }

        public Tuple2<Double, ArrayList<K>> map(Vertex<K, Double> value) throws Exception {
            ArrayList<K> emptylist = new ArrayList<>();
            if (value.f0.equals(srcVertexId)) {
                return new Tuple2<>(0.0,emptylist);
            } else {
                return new Tuple2<>(Double.MAX_VALUE,emptylist);
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
    /*
    * mindistance function from scatterfunction with:
    * K as K
    * VV as Tuple2<Double, ArrayList<K>>
    * Message as Tuple2<Double,ArrayList<K>>
    * EV as Tuple3<EV, Double, Double>
    *
    * checks if vertexvalue is < inf, then sends a message to neighboor vertexes
    * with end time and such
    * */
    private static final class MinDistanceMessengerforTuplewithpath<K,EV> extends ScatterFunction<K, Tuple2<Double, ArrayList<K>>, Tuple2<Double,ArrayList<K>>, Tuple3<EV, Double, Double>> {
        @Override
        public void sendMessages(Vertex<K, Tuple2<Double, ArrayList<K>>> vertex) {
            if (vertex.getValue().f0 < Double.POSITIVE_INFINITY) {
                for (Edge<K, Tuple3<EV,Double,Double>> edge : getEdges()) {
                    if (edge.getValue().f1 >= vertex.getValue().f0) {
                        ArrayList<K> temp = new ArrayList<>(vertex.getValue().f1);
                        temp.add(vertex.getId());
//                        System.out.println("temparray of " + vertex.getId() + ": " + temp.toString());
                        sendMessageTo(edge.getTarget(), new Tuple2<>(edge.getValue().f2.doubleValue(), temp) );
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

    private static final class VertexDistanceUpdaterwithpath<K> extends GatherFunction<K, Tuple2<Double,ArrayList<K>>, Tuple2<Double,ArrayList<K>>> {

        @Override
        public void updateVertex(Vertex<K, Tuple2<Double, ArrayList<K>>> vertex, MessageIterator<Tuple2<Double, ArrayList<K>>> inMessages) {

            Double minDistance = Double.MAX_VALUE;
            ArrayList<K> minpath = vertex.getValue().f1;
            for (Tuple2<Double,ArrayList<K>> msg : inMessages) {
                if (msg.f0 < minDistance) {
                    minDistance = msg.getField(0);
                    minpath = msg.f1;
                }
            }

            if (vertex.getValue().f0 > minDistance) {
                setNewVertexValue(new Tuple2<>(minDistance,minpath));
            }
        }


    }


}
