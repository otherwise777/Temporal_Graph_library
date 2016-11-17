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
public class SingleSourceShortestTemporalPathEATBetweenness<K,EV> implements TGraphAlgorithm<K,NullValue,EV,Double,DataSet<Vertex<K,Double>>> {

    private final Integer maxIterations;

    public SingleSourceShortestTemporalPathEATBetweenness(Integer maxIterations) {
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).getVertices().print();
        return null;
//        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>()).runScatterGatherIteration(
//                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
//                maxIterations).getVertices();

    }

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, ArrayList<Tuple3<K,Double,ArrayList<K>>>> {
        @Override
        public ArrayList<Tuple3<K, Double, ArrayList<K>>> map(Vertex<K, NullValue> value) throws Exception {
            ArrayList<Tuple3<K, Double, ArrayList<K>>> temp = new ArrayList<>();
            temp.add(new Tuple3<>(value.getId(),0D,new ArrayList<>()));
            return temp;
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

    private static final class MinDistanceMessengerforTuplewithpath<K,EV> extends
            ScatterFunction<
                    K,
                    ArrayList<Tuple3<K, Double, ArrayList<K>>>,
                    Tuple3<K,Double,ArrayList<K>>,
                    Tuple3<EV, Double, Double>
                    > {
        @Override
        public void sendMessages(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex) {
//            looping over every possible vertex value
            for(Tuple3<K, Double, ArrayList<K>> vertexValue : vertex.getValue()) {
//                looping over every edge that is connected to the vertex
                for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {

                    if (edge.getValue().f1 >= vertexValue.f1) {
                        ArrayList<K> path = new ArrayList<>(vertexValue.f2);
                        path.add(vertex.getId());
                        sendMessageTo(edge.getTarget(), new Tuple3<>(vertex.getId(),edge.getValue().f2, path));
                    }
                }
            }
        }
    }


//TODO:
//    the arraylist should be a mapping for major speedup and simplicity
    private static final class VertexDistanceUpdaterwithpath<K> extends GatherFunction<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>, Tuple3<K,Double,ArrayList<K>>> {

        @Override
        public void updateVertex(Vertex<K, ArrayList<Tuple3<K, Double, ArrayList<K>>>> vertex, MessageIterator<Tuple3<K, Double, ArrayList<K>>> inMessages) throws Exception {
//            initliazing vertex values,
            ArrayList<Tuple3<K, Double, ArrayList<K>>> vertexlist = vertex.getValue();
            boolean update = false;
            for (Tuple3<K,Double,ArrayList<K>> msg : inMessages) {
                if(!isValueInArrayList(vertexlist,msg.f0)) {
                    vertexlist.add(msg);
                }
            }
            for(Tuple3<K, Double, ArrayList<K>> vertexValue : vertex.getValue()) {
                Double minDistance = Double.MAX_VALUE;
                ArrayList<K> minpath = vertexValue.f2;
                for (Tuple3<K,Double,ArrayList<K>> msg : inMessages) {
                    if (msg.f1 < minDistance) {
                        minDistance = msg.f1;
                        minpath = msg.f2;
                    }
                }

                if (minDistance < vertexValue.f1) {
                    updatearrayList(vertexlist,vertexValue.f0,minDistance,minpath);
                }
            }
            if(update) {
                setNewVertexValue(vertexlist);
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
