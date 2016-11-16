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
public class SingleSourceShortestTemporalPathEATJustPaths<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,ArrayList<K>>>> {

    private final K srcVertexId;
    private final Integer maxIterations;

    public SingleSourceShortestTemporalPathEATJustPaths(K srcVertexId, Integer maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,ArrayList<K>>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).mapVertices(new finalisationmapper<>(srcVertexId)).getVertices();

    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, Tuple2<Double, ArrayList<K>>> {

        private K srcVertexId;

        public InitVerticesMapper(K srcId) {
            this.srcVertexId = srcId;
        }

        public Tuple2<Double, ArrayList<K>> map(Vertex<K, NullValue> value) throws Exception {
            ArrayList<K> emptylist = new ArrayList<>();
            if (value.f0.equals(srcVertexId)) {
                return new Tuple2<>(0.0,emptylist);
            } else {
                return new Tuple2<>(Double.MAX_VALUE,emptylist);
            }
        }
    }

    public static final class finalisationmapper<K>	implements MapFunction<Vertex<K, Tuple2<Double,ArrayList<K>>>, ArrayList<K>> {
        private K srcVertexId;
        public finalisationmapper(K srcVertexId) {
            this.srcVertexId = srcVertexId;
        }

        @Override
        public ArrayList<K> map(Vertex<K, Tuple2<Double, ArrayList<K>>> value) throws Exception {
            ArrayList<K> temp = value.getValue().f1;
            if(temp.size() >  0 ) { temp.remove(0); }
            return temp;
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
