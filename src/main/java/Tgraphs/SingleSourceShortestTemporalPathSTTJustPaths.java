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
public class SingleSourceShortestTemporalPathSTTJustPaths<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,ArrayList<K>>>> {

    private final K srcVertexId;
    private final Integer maxIterations;

    public SingleSourceShortestTemporalPathSTTJustPaths(K srcVertexId, Integer maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,ArrayList<K>>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).mapVertices(new finalVerticesMapper<>()).getVertices();
    }
/*
* Initialization mapper for the vertex values, sets the source vertex value to an empty arraylist and the the rest to null,
* this way don't need an initializer with a collect() function
*
*
* input: Tgraph <K,Double,EV,Double>
* output: K as Arraylist
* */
    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, ArrayList<Tuple3<Double,Double,ArrayList<K>>>> {

        private K srcVertexId;

        public InitVerticesMapper(K srcId) {
            this.srcVertexId = srcId;
        }

        public ArrayList<Tuple3<Double,Double,ArrayList<K>>> map(Vertex<K, NullValue> value) throws Exception {
             if (value.f0.equals(srcVertexId)) {
                return new ArrayList<>();
            } else {
                return null;
            }
        }
    }

    /*
    * finalization mapper, maps the Arrays in the vertex values to double values
    * Of all the values in the arraylist we take the difference between the 2nd
    * and the first tuple value and determine the minimum value of the List
    * then we return that value as it is the fastest travel time
    *
    * Input: ArrayList<Tuple2<Double,Double>>>
    * Output: Double
    *
    * */
    public static final class finalVerticesMapper<K>	implements MapFunction<Vertex<K, ArrayList<Tuple3<Double,Double,ArrayList<K>>>>, ArrayList<K>> {
        @Override
        public ArrayList<K> map(Vertex<K, ArrayList<Tuple3<Double,Double,ArrayList<K>>>> value) throws Exception {
            ArrayList<K> shortestpath = new ArrayList<K>();
            if(value.getValue() == null) {
                return shortestpath;
            }

            Double mindist = Double.MAX_VALUE;
            for (Tuple3<Double,Double,ArrayList<K>> tuple : value.getValue()) {
                if(Math.abs(tuple.f1 - tuple.f0) < mindist) {
                    mindist = Math.abs(tuple.f1 - tuple.f0);
                    shortestpath = tuple.f2;
                }
            }
            return shortestpath;
        }
    }
    /*
    * mindistance function from scatterfunction with:
    * K as K
    * VV as ArrayList<Tuple2<Double,Double>>
    * Message as Tuple2<Double,Double>
    * EV as Tuple3<EV, Double, Double>
    *
    * Checks if the vertexvalue is initialized and then checks the minimum path value and propagates it forward
    * */
    private static final class MinDistanceMessengerforTuplewithpath<K,EV> extends
            ScatterFunction<K, ArrayList<Tuple3<Double,Double,ArrayList<K>>>, Tuple3<Double,Double,ArrayList<K>>, Tuple3<EV, Double, Double>> {
        @Override

        public void sendMessages(Vertex<K, ArrayList<Tuple3<Double, Double, ArrayList<K>>>> vertex) throws Exception {

            if (vertex.getValue() != null) {
//                check if the vertex is initalized, we only accept vertexes with null values at the collect function
                if (vertex.getValue().size() == 0) {
//                    we have source vertex, time to fill it up with all source id's
                    for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
//                        vertex.getValue().add(new Tuple2<>(edge.getValue().f1, edge.getValue().f2));
                        ArrayList<K> temp = new ArrayList<>();
//                        temp.add(vertex.getId());
                        sendMessageTo(edge.getTarget(), new Tuple3<>(edge.getValue().f1, edge.getValue().f2,temp));
                    }
                } else {
//                    the size of the vertex value is not 0, so there have been incomming edges
                    for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
                        for (Tuple3<Double, Double, ArrayList<K>> tuple : vertex.getValue()) {
//                            check if the starting time of the edge is geq final time of the vertex
                            if (edge.getValue().f1 >= tuple.f1 && edge.getValue().f1 >= tuple.f0) {
//                               sends message to the target vertex with starting time of the node and finishing tiem of the edge
                                ArrayList<K> temp = new ArrayList<>(tuple.f2);
                                temp.add(vertex.getId());

                                sendMessageTo(edge.getTarget(), new Tuple3<>(tuple.f0, edge.getValue().f2,temp));
                            }
                        }
                    }
                }
            }
        }
    }





    /**
     * K as K
     * VV as  ArrayList<Tuple2<Double, Double>>
     * Message as Tuple2<Double,Double>
     *
     * collects messages from the Vertices and check the minimum paths, then stores those minimum paths in the Vertex
     */
    private static final class VertexDistanceUpdaterwithpath<K> extends GatherFunction<K, ArrayList<Tuple3<Double, Double, ArrayList<K>>>, Tuple3<Double,Double,ArrayList<K>>> {

        @Override
        public void updateVertex(Vertex<K, ArrayList<Tuple3<Double, Double, ArrayList<K>>>> vertex, MessageIterator<Tuple3<Double,Double, ArrayList<K>>> inMessages) throws Exception {
            ArrayList<Tuple3<Double,Double, ArrayList<K>>> vertexvalue = vertex.getValue();
            if(vertexvalue == null) {
                vertexvalue = new ArrayList<>();
            }

//            ArrayList<Tuple2<Double,Double>> minimums = new ArrayList<>(vertex.getValue());

            for (Tuple3<Double, Double, ArrayList<K>> tuple: inMessages)  {
                boolean tuplefound = false;
                for(Tuple3<Double,Double, ArrayList<K>> min : vertexvalue) {
                    if(tuple.f0.equals(min.f0)) {
                        tuplefound = true;
                        if (tuple.f1 < min.f1) {
//                        starting times are the same but new path is smaller
                            min.f1 = tuple.f1;
                            min.f2 = tuple.f2;

                        }
                    }
                }
                if(!tuplefound) {
                    vertexvalue.add(tuple);
                }
            }
            setNewVertexValue(vertexvalue);
        }
    }
}
