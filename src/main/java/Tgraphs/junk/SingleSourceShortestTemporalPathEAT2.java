package Tgraphs.junk;

import Tgraphs.TGraphAlgorithm;
import Tgraphs.Tgraph;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by s133781 on 03-Nov-16.
 *
 * input:
 * Tgraph <K,Double,EV,Double>
 * output:
 * Dataset<Vertex<K,tuple2<Double,Arraylist<Double>>>>
 *
 */
public class SingleSourceShortestTemporalPathEAT2<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,Double>>> {

    private final Integer maxIterations;

    public SingleSourceShortestTemporalPathEAT2( Integer maxIterations) {
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        input.getGellyGraph().mapVertices(new InitVerticesMapper2<K>()).getVertices().print();
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).getVertices();

    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, Double> {

        public Double map(Vertex<K, NullValue> value) throws Exception {
            return 0.0;
        }
    }
    public static final class InitVerticesMapper2<K>	implements MapFunction<Vertex<K, NullValue>, Map<K,Tuple2<Double,ArrayList<K>>>> {
        @Override
        public Map<K, Tuple2<Double,ArrayList<K>>> map(Vertex<K, NullValue> value) throws Exception {
            Map temp = new HashMap();
            temp.put(value.getId(),new Tuple2<>(0D, new ArrayList<K>()));
            return temp;
        }
    }
    public static final class InitVerticesMapper3<K>	implements MapFunction<Vertex<K, NullValue>, ArrayList<Tuple3<Double,Double,ArrayList<K>>>> {

        public ArrayList<Tuple3<Double,Double,ArrayList<K>>> map(Vertex<K, NullValue> value) throws Exception {
            return new ArrayList<>();
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

    private static final class MinDistanceMessengerforTuplewithpath2<K,EV> extends
            ScatterFunction<
                    K,
                    ArrayList<Tuple3<K, Double, ArrayList<K>>>,
                    Tuple3<K,Double,ArrayList<K>>,
                    Tuple3<EV, Double, Double>
                    > {
        @Override
        public void sendMessages(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex) {
            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {

                ArrayList<K> path = new ArrayList<>();
                path.add(vertex.getId());
                sendMessageTo(edge.getTarget(), new Tuple3<>(vertex.getId(),edge.getValue().f2, path));

            }

        }
    }


    //TODO:
//    the arraylist should be a mapping for major speedup and simplicity
    private static final class VertexDistanceUpdaterwithpath2<K> extends GatherFunction<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>, Tuple3<K,Double,ArrayList<K>>> {

        @Override
        public void updateVertex(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex, MessageIterator<Tuple3<K, Double, ArrayList<K>>> inMessages) throws Exception {

            for(Tuple3<K, Double, ArrayList<K>> vertexValue : vertex.getValue()) {
                Double minDistance = Double.MAX_VALUE;
                ArrayList<K> minpath = vertexValue.f2;
                for (Tuple3<K,Double,ArrayList<K>> msg : inMessages) {
                    if (msg.f1 < minDistance) {
                        minDistance = msg.f1;
                        minpath = msg.f2;
                    }
                }
            }
        }

    }


}
