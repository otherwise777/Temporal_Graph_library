package Tgraphs;

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
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).mapVertices(new inbetweenmapper<>()).reverse().runScatterGatherIteration(
                new MinDistanceMessengerCountingPaths<K,EV>(), new VertexDistanceUpdaterCountingPaths<K>(),
                maxIterations).mapVertices(new finalisationmapper<>()).getVertices();


    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, ArrayList<Tuple3<K,Double,ArrayList<K>>>> {
        @Override
        public ArrayList<Tuple3<K, Double, ArrayList<K>>> map(Vertex<K, NullValue> value) throws Exception {
            ArrayList<Tuple3<K, Double, ArrayList<K>>> temp = new ArrayList<>();
            temp.add(new Tuple3<>(value.getId(),0D,new ArrayList<>()));
            return temp;
        }
    }

    public static final class inbetweenmapper<K>	implements MapFunction<Vertex<K,ArrayList<Tuple3<K, Double, ArrayList<K>>>> , Tuple2<Double,ArrayList<ArrayList<K>>>> {
        @Override
        public Tuple2<Double, ArrayList<ArrayList<K>>> map(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex) throws Exception {
            ArrayList<ArrayList<K>> vertexvalue = new ArrayList<>();

            for(Tuple3<K, Double, ArrayList<K>> tuple: vertex.getValue()) {
                if (tuple.f2.size() > 1) {
                    tuple.f2.remove(0);
                    vertexvalue.add(tuple.f2);
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
                ArrayList<Tuple3<K, Double, ArrayList<K>>>,
                ArrayList<Tuple3<K,Double,ArrayList<K>>>,
                Tuple3<EV, Double, Double>
                > {
        @Override
        public void sendMessages(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex) {
//            looping over every possible vertex value
            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
                for (Tuple3<K, Double, ArrayList<K>> vertexValue : vertex.getValue()) {
//                looping over every edge that is connected to the vertex

                    if (edge.getValue().f1 >= vertexValue.f1) {
                        ArrayList<K> path = new ArrayList<>(vertexValue.f2);
                        path.add(vertex.getId());
                        ArrayList<Tuple3<K, Double, ArrayList<K>>> temp = new ArrayList<>();
                        temp.add(new Tuple3<>(vertexValue.f0, edge.getValue().f2, path));
                        sendMessageTo(edge.getTarget(), temp);
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
                ArrayList<Tuple3<K, Double, ArrayList<K>>>,
                ArrayList<Tuple3<K,Double,ArrayList<K>>>
                > {

        @Override
        public void updateVertex(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex, MessageIterator<ArrayList<Tuple3<K,Double,ArrayList<K>>>> inMessages) throws Exception {
//            initliazing vertex values,
            ArrayList<Tuple3<K, Double, ArrayList<K>>> vertexlist = vertex.getValue();
            boolean update = false;

            for (ArrayList<Tuple3<K,Double,ArrayList<K>>> msgout : inMessages) {
                Tuple3<K,Double,ArrayList<K>> msg = msgout.get(0);
//                Check if K exists in vertexlist
                if(!isValueInArrayList(vertexlist,msg.f0)) {
//                    msg.f1 = Double.MAX_VALUE;
                    vertexlist.add(new Tuple3<>(msg.f0,msg.f1,new ArrayList<K>(msg.f2)));
                    update = true;
                }
                Double minDistance = Double.MAX_VALUE;
                ArrayList<K> minpath = msg.f2;
                for(Tuple3<K, Double, ArrayList<K>> vertexValue : vertex.getValue()) {
//                    check if the K is the same
                    if (vertexValue.f0.equals(msg.f0)) {
                        if(msg.f1 < vertexValue.f1) {
                            vertexValue.f1 = msg.f1;
                            vertexValue.f2 = new ArrayList<K>(msg.f2);
                            update = true;
                        }
                    }

                }

            }

//            for (ArrayList<Tuple3<K,Double,ArrayList<K>>> msgout : inMessages) {
//                Tuple3<K,Double,ArrayList<K>> msg = msgout.get(0);
//                if(!isValueInArrayList(vertexlist,msg.f0)) {
//                    msg.f1 = Double.MAX_VALUE;
//                    vertexlist.add(new Tuple3<>(msg.f0,msg.f1,new ArrayList<K>(msg.f2)));
//                }
//            }
//            for(Tuple3<K, Double, ArrayList<K>> vertexValue : vertex.getValue()) {
//                Double minDistance = Double.MAX_VALUE;
//                ArrayList<K> minpath = vertexValue.f2;
//                for (ArrayList<Tuple3<K,Double,ArrayList<K>>> msgout : inMessages) {
//                    Tuple3<K,Double,ArrayList<K>> msg = msgout.get(0);
//                    if (msg.f1 < minDistance) {
//                        minDistance = msg.f1;
//                        minpath = msg.f2;
//                    }
//                }
//
//                if (minDistance < vertexValue.f1) {
//                    updatearrayList(vertexlist,vertexValue.f0,minDistance,minpath);
//                }
//            }
            if(update) {
                setNewVertexValue(new ArrayList<Tuple3<K, Double, ArrayList<K>>>(vertexlist));
            }

        }
        private boolean isValueInArrayList(ArrayList<Tuple3<K, Double, ArrayList<K>>> thelist,K value) {
            for(Tuple3<K, Double, ArrayList<K>> somevalue: thelist) {
                if (somevalue.f0.equals(value)) {
                    return true;
                }
            }
            return false;
        }
        public void updatearrayList(ArrayList<Tuple3<K, Double, ArrayList<K>>> thelist, K index, Double newdouble, ArrayList<K> newlist) {
            for(Tuple3<K, Double, ArrayList<K>> somevalue: thelist) {
                if(somevalue.f0.equals(index)) {
                    somevalue.f1 = newdouble;
                    somevalue.f2 = newlist;
                }
            }
        }
    }



}
