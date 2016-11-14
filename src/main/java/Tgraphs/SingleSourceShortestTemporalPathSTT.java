package Tgraphs;

import org.apache.flink.api.common.functions.FilterFunction;
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
import java.util.List;

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
    private DataSet<Tuple2<Double,Double>> startedges;
    private ArrayList<Tuple2<Double,Double>> listedges;

    public SingleSourceShortestTemporalPathSTT(K srcVertexId, Integer maxIterations) {
        this.srcVertexId = srcVertexId;
        this.maxIterations = maxIterations;
    }
    @Override
    public DataSet<Vertex<K,Double>> run(Tgraph<K, NullValue, EV, Double> input) throws Exception {
        //Filling up the startingedges with distinct values of the starting times of the starting edges
        DataSet<Edge<K,Tuple3<EV,Double,Double>>> startedgesset = input.getGellyGraph().getEdges().filter(new Filterstartingedges(srcVertexId));
        startedges = startedgesset.map(new MapFunction<Edge<K, Tuple3<EV, Double, Double>>, Tuple2<Double,Double>>() {
            @Override
            public Tuple2<Double,Double> map(Edge<K, Tuple3<EV, Double, Double>> value) throws Exception {
                return new Tuple2<>(value.getValue().f1,Double.MAX_VALUE);
            }
        }).distinct();

        listedges = (ArrayList<Tuple2<Double, Double>>) startedges.collect();
//        System.out.println("printing edges");
//        System.out.println(listedges.toString());
//        startedges.print();
        input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId,listedges)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).getVertices().print();
        return input.getGellyGraph().mapVertices(new InitVerticesMapper<K>(srcVertexId,listedges)).runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath<K,EV>(), new VertexDistanceUpdaterwithpath<K>(),
                maxIterations).mapVertices(new finalVerticesMapper<>()).getVertices();

    }
    /*
    * Filter to filter out the edges which have srcvertexid as source veretx
    * */
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

    public static final class InitVerticesMapper<K>	implements MapFunction<Vertex<K, NullValue>, ArrayList<Tuple2<Double,Double>>> {

        private K srcVertexId;
        private ArrayList<Tuple2<Double,Double>> startedges;

        public InitVerticesMapper(K srcId, ArrayList<Tuple2<Double,Double>> startedges) {
            this.startedges = startedges;
            this.srcVertexId = srcId;
        }

        /*
        * adds the startingedge dataset to every edge, and changes the starting vertex with null values in that dataset
        * */
        public ArrayList<Tuple2<Double,Double>> map(Vertex<K, NullValue> value) throws Exception {
            ArrayList<Tuple2<Double,Double>> nodeList = new ArrayList<>();
//            return nodeList;
             if (value.f0.equals(srcVertexId)) {
                 for (Tuple2<Double,Double> tuple: startedges) {
                     nodeList.add(new Tuple2<>(tuple.f0,0D));
                 }
                return nodeList;
            } else {
                return new ArrayList<>(startedges);
            }
        }
    }
    /*
    * final mapper, gets arraylist with values and maps it to
    * */
    public static final class finalVerticesMapper<K>	implements MapFunction<Vertex<K, ArrayList<Tuple2<Double,Double>>>, Double> {
        @Override
        public Double map(Vertex<K, ArrayList<Tuple2<Double, Double>>> value) throws Exception {
            Double mindist = Double.MAX_VALUE;
            for (Tuple2<Double,Double> tuple : value.getValue()) {
                if(Math.abs(tuple.f1 - tuple.f0) < mindist) {
                    mindist = Math.abs(tuple.f1 - tuple.f0);
                }
            }
            return mindist;
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
    private static final class MinDistanceMessengerforTuplewithpath<K,EV> extends ScatterFunction<K, ArrayList<Tuple2<Double,Double>>, Tuple2<Double,Double>, Tuple3<EV, Double, Double>> {
        @Override
        public void sendMessages(Vertex<K, ArrayList<Tuple2<Double, Double>>> vertex) throws Exception {
            for (Edge<K, Tuple3<EV, Double, Double>> edge : getEdges()) {
                for (Tuple2<Double,Double> tuple : vertex.getValue()) {
//                    check if the starting time of the edge is geq final time of the vertex
                    if(edge.getValue().f1 >= tuple.f1 && edge.getValue().f1 >= tuple.f0) {
//                        sends message to the target vertex with starting time of the node and finishing tiem of the edge
                        sendMessageTo(edge.getTarget(),new Tuple2<>(tuple.f0,edge.getValue().f2));
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
    private static final class VertexDistanceUpdaterwithpath<K> extends GatherFunction<K, ArrayList<Tuple2<Double, Double>>, Tuple2<Double,Double>> {

//        @Override
//        public void updateVertex(Vertex<K, Double> vertex, MessageIterator<Double> inMessages) {
//
//            Double minDistance = Double.MAX_VALUE;
//            for (Double msg : inMessages) {
//                if (msg < minDistance) {
//                    minDistance = msg;
//                }
//            }
//
//            if (vertex.getValue() > minDistance) {
//                setNewVertexValue(minDistance);
//            }
//        }


        @Override
        public void updateVertex(Vertex<K, ArrayList<Tuple2<Double, Double>>> vertex, MessageIterator<Tuple2<Double,Double>> inMessages) throws Exception {
            ArrayList<Tuple2<Double,Double>> minimums = new ArrayList<>(vertex.getValue());

            for (Tuple2<Double,Double> tuple: inMessages)  {
                for(Tuple2<Double,Double> min : minimums) {
                    if(tuple.f0.equals(min.f0) && tuple.f1 < min.f1) {
//                        starting times are the same but new path is smaller
                        min.f1 = tuple.f1;
                    }
                }
            }
            setNewVertexValue(minimums);
        }
    }
}
