package Tgraphs;

import org.apache.commons.collections.map.HashedMap;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeutils.base.DoubleComparator;
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
public class SSSTPBetweenness<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,Double>>> {

    private final Integer maxIterations;


    public SSSTPBetweenness(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {

        input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).mapVertices(new debugmapper<>()).getVertices().print();
//return null;
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).mapVertices(new inbetweenmapper<>()).reverse().runScatterGatherIteration(
                new MinDistanceMessengerCountingPaths<K,EV>(), new VertexDistanceUpdaterCountingPaths<K>(),
                maxIterations).mapVertices(new finalisationmapper<>()).getVertices();


    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, BetVertexValue<K>> {
        @Override
        public BetVertexValue<K> map(Vertex<K, NullValue> value) throws Exception {
            return new BetVertexValue<>(value.getId());
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
                ArrayList<K> path = tuple.f1;
                Double dist = tuple.f0;
                if (path.size() > 1) {
//                    path.remove(0);
                    vertexvalue.add(new Tuple3<K, Double, ArrayList<K>>(value,dist,path));
                }
            }
            return vertexvalue;
        }
    }
    public static final class inbetweenmapper<K>	implements MapFunction<Vertex<K,BetVertexValue<K>> , Tuple2<Double,ArrayList<ArrayList<K>>>> {
        @Override
        public Tuple2<Double, ArrayList<ArrayList<K>>> map(Vertex<K, BetVertexValue<K>> vertexValue) throws Exception {
            ArrayList<ArrayList<K>> vertexvalue = new ArrayList<>();

            for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertexValue.getValue().getentrySet()) {
//                    initialize
                Tuple3<Double, ArrayList<K>,Boolean> tuple = entry.getValue();
                ArrayList<K> path = tuple.f1;
                if(path.size() > 1) {
                    path.remove(0);
                    vertexvalue.add(path);
                }
            }
            return new Tuple2<>(0D,vertexvalue);
        }
    }

    public static final class finalisationmapper<K>	implements MapFunction<Vertex<K, Tuple2<Double,ArrayList<ArrayList<K>>>>, Double> {
        @Override
        public Double map(Vertex<K, Tuple2<Double, ArrayList<ArrayList<K>>>> value) throws Exception {
            return value.getValue().f0;
        }
    }

    private static final class MinDistanceMessengerCountingPaths<K,EV> extends
            ScatterFunction<
                    K,
                    Tuple2<Double, ArrayList<ArrayList<K>>>,
                    ArrayList<K>,
                    Tuple3<EV, Double, Double>
                    > {

        @Override
        public void sendMessages(Vertex<K, Tuple2<Double, ArrayList<ArrayList<K>>>> vertex) throws Exception {
            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
                for(ArrayList<K> path: vertex.getValue().f1) {
                    K lastelement = path.get(path.size() - 1);
                    if(edge.getTarget().equals(lastelement)) {
                        ArrayList<K> newpath = new ArrayList<K>(path);
                        newpath.remove(newpath.size() - 1);
                        sendMessageTo(edge.getTarget(),newpath);
                    }
                }
            }
        }
    }

    private static final class VertexDistanceUpdaterCountingPaths<K> extends
            GatherFunction<
                    K,
                    Tuple2<Double, ArrayList<ArrayList<K>>>,
                    ArrayList<K>
                    > {

        @Override
        public void updateVertex(Vertex<K, Tuple2<Double, ArrayList<ArrayList<K>>>> vertex, MessageIterator<ArrayList<K>> messageIterator) throws Exception {
            ArrayList<ArrayList<K>> vertexvalue = new ArrayList<>();
            boolean update = false;
            for (ArrayList<K> incommingpath : messageIterator) {
                vertex.getValue().f0 = vertex.getValue().f0 + 1;
                update = true;
                if(incommingpath.size() > 0 ) {
                    vertexvalue.add(incommingpath);
                }
            }
            if (update) {
                setNewVertexValue(new Tuple2<>(vertex.getValue().f0,vertexvalue));
            }
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
            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
                Iterator it = vertex.getValue().getIterator();
                for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertex.getValue().getentrySet()) {
                    K value = entry.getKey();
                    Tuple3<Double,ArrayList<K>,Boolean> tuple = entry.getValue();
//                    check if the vertex needs to be send
                    if(tuple.f2) {
//                    Checks whether it can be send
                        if (edge.getValue().f1 >= tuple.f0) {
                            ArrayList<K> path = new ArrayList<>(tuple.f1);
                            path.add(vertex.getId());
                            BetMsg<K> temp = new BetMsg<>(value, edge.getValue().f2, path);
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
//            ArrayList<Tuple3<K, Double, ArrayList<K>>> vertexlist = vertex.getValue();
            BetVertexValue<K> vertexValue = vertex.getValue();
            vertexValue.clearUpdates();

            boolean update = false;

            for (BetMsg<K> msg : inMessages) {
//                Check if K exists in vertexlist and adds if it it doesnt
                if(vertexValue.addIfNotExist(msg.getValue(),msg.getDist(),msg.getPath())) {
//                    sets update to true if value is updated
                    update = true;
                }
                for (Map.Entry<K, Tuple3<Double,ArrayList<K>,Boolean>> entry : vertexValue.getentrySet()) {
//                    initialize
                    K value = entry.getKey();
                    Tuple3<Double, ArrayList<K>,Boolean> tuple = entry.getValue();
                    ArrayList<K> path = tuple.f1;
                    Double dist = tuple.f0;
//                    check if the values are the same and if the distance is shorter
                    if (value.equals(msg.getValue()) && msg.getDist() < dist) {
                        update = true;
                        dist = msg.getDist();
                        path = msg.getPath();
                        vertexValue.setDistandPath(value,dist,path);
                    }
                }
            }
            if(update) {
                setNewVertexValue(vertexValue);
            }
        }
    }
}
