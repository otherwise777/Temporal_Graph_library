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
import org.apache.flink.graph.spargel.ScatterGatherConfiguration;
import org.apache.flink.types.NullValue;

import java.util.ArrayList;
import java.util.Iterator;
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
public class SSSTPCloseness<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,Double>>> {

    private final Integer maxIterations;
    private final Integer method;
    private final boolean Normalized;

    public SSSTPCloseness(Integer maxIterations, Integer method, boolean Normalized) {
        this.maxIterations = maxIterations;
        this.method = method;
        this.Normalized = Normalized;
    }
    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
//        input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
//                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
//                maxIterations).mapVertices(new debugmapper<>()).getVertices().print();
//        return null;
        ScatterGatherConfiguration parameters = new ScatterGatherConfiguration();
        parameters.setName("closeness");
        parameters.setSolutionSetUnmanagedMemory(true);

        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations, parameters).mapVertices(new inbetweenmapper<>()).runScatterGatherIteration(
                new MinDistanceMessengerCountingPaths<K,EV>(), new VertexDistanceUpdaterCountingPaths<K>(),
                1, parameters).mapVertices(new finalisationmapper<>()).getVertices();


    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, BetVertexValue<K>> {
        @Override
        public BetVertexValue<K> map(Vertex<K, NullValue> value) throws Exception {
            return new BetVertexValue<>(value.getId());
        }
    }
    public static final class inbetweenmapper<K>	implements MapFunction<Vertex<K,BetVertexValue<K>> , Tuple2<Double, ArrayList<Tuple2<K,Double>>>> {
        @Override
        public Tuple2<Double, ArrayList<Tuple2<K,Double>>> map(Vertex<K, BetVertexValue<K>> vertexValue) throws Exception {
            ArrayList<Tuple2<K,Double>> vertexresult = new ArrayList<>();

            for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertexValue.getValue().getentrySet()) {
//                    initialize
                K value = entry.getKey();
                Tuple3<Double, ArrayList<K>,Boolean> tuple = entry.getValue();
                Double dist = tuple.f0;
                vertexresult.add(new Tuple2<>(value, dist));
            }
            return new Tuple2<>(0D,vertexresult);
        }
    }
    public static final class finalisationmapper<K>	implements MapFunction<Vertex<K, Tuple2<Double, ArrayList<Tuple2<K,Double>>>>, Double> {
        @Override
        public Double map(Vertex<K, Tuple2<Double, ArrayList<Tuple2<K, Double>>>> vertex) throws Exception {
            return vertex.getValue().f0;
        }
    }
    public static final class debugmapper<K>	implements MapFunction<Vertex<K,BetVertexValue<K>> , ArrayList<Tuple3<K,Double,ArrayList<K>>>> {
        @Override
        public ArrayList<Tuple3<K,Double,ArrayList<K>>> map(Vertex<K, BetVertexValue<K>> vertexValue) throws Exception {
            ArrayList<Tuple3<K,Double,ArrayList<K>>> vertexvalue = new ArrayList<>();

            for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertexValue.getValue().getentrySet()) {
//                    initialize
                K value = entry.getKey();
                Tuple3<Double, ArrayList<K>,Boolean> tuple = entry.getValue();

                Double dist = tuple.f0;
                vertexvalue.add(new Tuple3<>(value, dist, null));

            }
            return vertexvalue;
        }
    }


    private static final class MinDistanceMessengerCountingPaths<K,EV> extends
            ScatterFunction<
                    K,
                    Tuple2<Double, ArrayList<Tuple2<K,Double>>>,
                    Double,
                    Tuple3<EV, Double, Double>
                    > {


        @Override
        public void sendMessages(Vertex<K, Tuple2<Double, ArrayList<Tuple2<K,Double>>>> vertex) throws Exception {
//            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
            for (Tuple2<K,Double> tuple: vertex.getValue().f1) {
//                    initialize
                Double dist = tuple.f1;
                K value = tuple.f0;
                if (dist != 0) {
//                        sends message to source value with the inverse of the distance
//                    System.out.println("message send: " + vertex.getId().toString() + "->" + value.toString() + " : " + 1/dist);

                    sendMessageTo(value,1/dist );
                }
            }
//            }
        }
    }
    private static final class VertexDistanceUpdaterCountingPaths<K> extends
            GatherFunction<
                    K,
                    Tuple2<Double, ArrayList<Tuple2<K,Double>>>,
                    Double
                    > {

        @Override
        public void updateVertex(Vertex<K, Tuple2<Double, ArrayList<Tuple2<K,Double>>>> vertex, MessageIterator<Double> messageIterator) throws Exception {
            Double total = 0D;
            for (Double dist: messageIterator) {
                total += dist;
            }
            setNewVertexValue(new Tuple2<>(total,null));

        }
    }

    private static final class MinDistanceMessengerforTuplewithpath<K,EV> extends
            ScatterFunction<
                    K,
                    BetVertexValue<K>,
                    BetMsg<K>,
                    Tuple3<EV, Double, Double>
                    > {


        @Override
        public void sendMessages(Vertex<K, BetVertexValue<K>> vertex) throws Exception {

//            System.out.println("start of iteration");
            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
                Iterator it = vertex.getValue().getIterator();
                for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertex.getValue().getentrySet()) {
                    K value = entry.getKey();
                    Tuple3<Double,ArrayList<K>,Boolean> tuple = entry.getValue();
//                    check if the vertex needs to be send
                    if(tuple.f2) {
//                    Checks whether it can be send
                        if (edge.getValue().f1 >= tuple.f0) {
                            BetMsg<K> temp = new BetMsg<>(value, edge.getValue().f2, null);
//                            System.out.println("message send: " + value.toString() + ".." + edge.getSource().toString() + "->" + edge.getTarget().toString() + " : " + edge.getValue().f2.toString());
                            sendMessageTo(edge.getTarget(), temp);
                        }
                    }
                }
            }
        }
    }


    //TODO:
//    the arraylist should be a mapping for major speedup and simplicity
    private static final class VertexDistanceUpdaterwithpath<K> extends
            GatherFunction<
                    K,
                    BetVertexValue<K>,
                    BetMsg<K>
                    > {

        @Override
        public void updateVertex(Vertex<K, BetVertexValue<K>> vertex, MessageIterator<BetMsg<K>> inMessages) throws Exception {
//            initliazing vertex values,
            BetVertexValue<K> vertexValue = vertex.getValue();
            vertexValue.clearUpdates();

            boolean update = false;

            for (BetMsg<K> msg : inMessages) {
//                Check if K exists in vertexlist and adds if it it doesnt
                if(vertexValue.addIfNotExist(msg.getValue(),msg.getDist(),null)) {
//                    sets update to true if value is updated
                    update = true;
                }
                for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertexValue.getentrySet()) {
//                    initialize
                    K value = entry.getKey();
                    Tuple3<Double, ArrayList<K>,Boolean> tuple = entry.getValue();
                    Double dist = tuple.f0;
//                    check if the values are the same and if the distance is shorter
                    if (value.equals(msg.getValue()) && msg.getDist() < dist) {
                        update = true;
                        dist = msg.getDist();
                        vertexValue.setDistandPath(value,dist,null);
                    }
                }
            }
            if(update) {
                setNewVertexValue(vertexValue);
            }
        }
    }



}
