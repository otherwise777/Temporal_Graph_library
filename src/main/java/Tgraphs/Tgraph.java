package Tgraphs;

import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.FunctionAnnotation;
import org.apache.flink.api.java.tuple.*;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.GraphAlgorithm;
import org.apache.flink.graph.Vertex;
import org.apache.flink.types.NullValue;
import org.apache.flink.util.Collector;
import scala.math.Numeric;

import javax.xml.crypto.Data;

import java.util.*;

import static org.apache.commons.math3.util.Precision.compareTo;

/**
 * Created by s133781 on 18-Oct-16.
 */
public class Tgraph<K,VV,EV,N> {

    protected final ExecutionEnvironment context;
    protected final DataSet<Edge<K,Tuple3<EV,N,N>>> edges;
    protected final DataSet<Vertex<K, VV>> vertices;

    /*
    * Constructor that creates the temporal graph from the Tuple5 set
    * */
    public Tgraph(DataSet<Vertex<K, VV>> vertices, DataSet<Edge<K,Tuple3<EV,N,N>>> edges, ExecutionEnvironment context) throws Exception {
        this.vertices = vertices;
        this.edges = edges;
        this.context = context;
    }


    /*
    * Transforms a tuple5 dataset with (source node, target node, edge value, start time, end time) to a
    * temporalgraph set with no vertex values.
    * @param tupleset DataSet Tuple5 with (source node, target node, edge value, start time, end time)
    * @param context the flink execution environment.
    * @return newly created Tgraphs.Tgraph
    * */
    public static <K,EV,N> Tgraph<K,NullValue,EV,N> From5TupleNoVertexes(DataSet<Tuple5<K,K,EV,N,N>> tupleset, ExecutionEnvironment context) throws Exception {
        DataSet<Edge<K,Tuple3<EV,N,N>>> edges = tupleset.map(new MapFunction<Tuple5<K, K, EV, N, N>, Edge<K, Tuple3<EV, N, N>>>() {
            @Override
            public Edge<K, Tuple3<EV, N, N>> map(Tuple5<K, K, EV, N, N> value) throws Exception {
                return new Edge<K,Tuple3<EV,N,N>>(value.f0, value.f1, new Tuple3<EV,N,N>(value.f2, value.f3, value.f4));
            }
        });
        return FromEdgeSet(edges,context);
    }
    /*
    * Transforms a tuple5 dataset with (source node, target node, edge value, start time, end time) to a
    * temporalgraph set with no vertex values.
    * @param tupleset DataSet Tuple5 with (source node, target node, edge value, start time, end time)
    * @param context the flink execution environment.
    * @return newly created Tgraphs.Tgraph
    * */
    public static <K,VV,EV,N> Tgraph<K,VV,EV,N> From5Tuple(DataSet<Tuple5<K,K,EV,N,N>> tupleset, DataSet<Vertex<K,VV>> vertices, ExecutionEnvironment context) throws Exception {
        DataSet<Edge<K,Tuple3<EV,N,N>>> edges = tupleset.map(new MapFunction<Tuple5<K, K, EV, N, N>, Edge<K, Tuple3<EV, N, N>>>() {
            @Override
            public Edge<K, Tuple3<EV, N, N>> map(Tuple5<K, K, EV, N, N> value) throws Exception {
                return new Edge<K,Tuple3<EV,N,N>>(value.f0, value.f1, new Tuple3<EV,N,N>(value.f2, value.f3, value.f4));
            }
        });
        return new Tgraph(vertices,edges,context);
    }

    /*
    * Transforms a tuple4 of (source node, target node, start time, end time) to a temporal graph set
    * with no vertex values and no edge values
    * @param tupleset DataSet Tuple4 with (source node, target node, start time, end time)
    * @param context the flink execution environment.
    * @return newly created Tgraphs.Tgraph
    * */
    public static <K,N> Tgraph<K,NullValue,NullValue,N> From4TupleNoEdgesNoVertexes(DataSet<Tuple4<K,K,N,N>> tupleset, ExecutionEnvironment context) throws Exception {
        DataSet<Edge<K,Tuple3<NullValue,N,N>>> edges = tupleset.map(new MapFunction<Tuple4<K, K, N, N>, Edge<K, Tuple3<NullValue, N, N>>>() {
            @Override
            public Edge<K, Tuple3<NullValue, N, N>> map(Tuple4<K, K, N, N> value) throws Exception {
                return new Edge<>(value.f0, value.f1, new Tuple3<>(NullValue.getInstance(), value.f2, value.f3));
            }
        });
        return FromEdgeSet(edges,context);
    }
    /*
    * Transforms a tuple4 of (source node, target node, start time, end time) to a temporal graph set
    * with no vertex values and no edge values
    * @param tupleset DataSet Tuple4 with (source node, target node, start time, end time)
    * @param context the flink execution environment.
    * @return newly created Tgraphs.Tgraph
    * */
    public static <K,VV,N> Tgraph<K,VV,NullValue,N> From4TupleNoEdgesWithVertices(DataSet<Tuple4<K,K,N,N>> tupleset,final MapFunction<K, VV> vertexValueInitializer, ExecutionEnvironment context) throws Exception {
        DataSet<Edge<K,Tuple3<NullValue,N,N>>> edges = tupleset.map(new MapFunction<Tuple4<K, K, N, N>, Edge<K, Tuple3<NullValue, N, N>>>() {
            @Override
            public Edge<K, Tuple3<NullValue, N, N>> map(Tuple4<K, K, N, N> value) throws Exception {
                return new Edge<>(value.f0, value.f1, new Tuple3<>(NullValue.getInstance(), value.f2, value.f3));
            }
        });
        Graph<K,VV,Tuple3<NullValue,N,N>> temporalgraph = Graph.fromDataSet(edges,vertexValueInitializer,context);
        return new Tgraph<>(temporalgraph.getVertices(), edges, context);
    }
    public static <K,VV,EV,N> Tgraph<K,VV,EV,N> From5TuplewithEdgesandVertices(DataSet<Tuple5<K,K,N,N,EV>> tupleset,final MapFunction<K, VV> vertexValueInitializer, ExecutionEnvironment context) throws Exception {
        DataSet<Edge<K,Tuple3<EV,N,N>>> edges = tupleset.map(new MapFunction<Tuple5<K, K, N, N, EV>, Edge<K, Tuple3<EV, N, N>>>() {
             @Override
             public Edge<K, Tuple3<EV,N,N>> map(Tuple5<K, K, N, N, EV> value) throws Exception {
                 return new Edge<>(value.f0, value.f1, new Tuple3<>(value.f4, value.f2, value.f3));
             }
         });
        Graph<K,VV,Tuple3<EV,N,N>> temporalgraph = Graph.fromDataSet(edges,vertexValueInitializer,context);
        return new Tgraph<>(temporalgraph.getVertices(), edges, context);
    }

    /*
    * @param edges edge dataset
    * @param context the flink execution environment.
    * @return newly created Tgraphs.Tgraph
    * */
    public static <K,EV,N> Tgraph<K,NullValue,EV,N> FromEdgeSet(DataSet<Edge<K, Tuple3<EV, N, N>>> edges, ExecutionEnvironment context) throws Exception {
        Graph<K,NullValue,Tuple3<EV,N,N>> temporalgraph = Graph.fromDataSet(edges,context);
        return new Tgraph<K,NullValue,EV,N>(temporalgraph.getVertices(),edges,context);
    }

    /*
    * @param edges edge dataset
    * @param vertices vertex set
    * @param context the flink execution environment.
    * @return newly created Tgraphs.Tgraph
    * */
    public static <K,VV,EV,N> Tgraph<K,VV,EV,N> FromDataSet(DataSet<Edge<K, Tuple3<EV, N, N>>> edges,DataSet<Vertex<K,VV>> vertices, ExecutionEnvironment context) throws Exception {
        return new Tgraph<K,VV,EV,N>(vertices,edges,context);
    }

    /**
     * @return a DataSet<Edge> with source/ target/ edge value of the temporal graph
     */
    public DataSet<Edge<K, Tuple3<EV,N,N>>> getTemporalEdges() {
        return edges;
    }
    /**
     * @return a DataSet<Edge> with source/ target/ edge value of the temporal graph
     */
    public DataSet<Edge<K, EV>> getEdges() {
        DataSet<Edge<K, EV>> newedges = edges.map(new MapFunction<Edge<K, Tuple3<EV, N, N>>, Edge<K, EV>>() {
            @Override
            public Edge<K, EV> map(Edge<K, Tuple3<EV, N, N>> value) throws Exception {
                return new Edge<>(value.f0,value.f1,value.f2.getField(0));
            }
        });
        return newedges;
    }
    /*
    * @return Dataset(Vertex) of vertexes
    * */
    public DataSet<Vertex<K, VV>> getVertices() { return vertices; }

    /**
     * @return a long integer representing the number of vertices
     */
    public long numberOfVertices() throws Exception {
        return vertices.count();
    }
    /**
     * @return a long integer representing the number of edges
     */
    public long numberOfEdges() throws Exception {
        return edges.count();
    }

    /*
    * @return temporal graph as a Gelly Graph
    * */
    public Graph<K,VV,Tuple3<EV,N,N>> getGellyGraph() {
        Graph<K,VV,Tuple3<EV,N,N>> tempgraph = Graph.fromDataSet(vertices,edges,context);
        return tempgraph;
    }
    /*
    * Gets a slice of the graph where the start of every edge is >= start
     * and the finish time of every edge is <= finish
     * returns a new graph
    * */
    public Tgraph<K,VV,EV,N> getGraphSlice2(long start, long finish) throws Exception {
        Graph<K,VV,Tuple3<EV,N,N>> tempgraph = Graph.fromDataSet(vertices,edges,context).filterOnEdges(new FilterFunction<Edge<K, Tuple3<EV, N, N>>>() {
            @Override
            public boolean filter(Edge<K, Tuple3<EV, N, N>> value) throws Exception {
                return (int) value.getValue().getField(1) >= start && (int) value.getValue().getField(2) <= finish;
            }
        });

        return new Tgraph<>(tempgraph.getVertices(), tempgraph.getEdges(), context);
    }
    /*
    * Gets a slice of the graph where the start of every edge is >= start
     * and the finish time of every edge is <= finish
     * returns a new graph
    * */
    public Tgraph<K,VV,EV,N> getGraphSlice(long start, long end) throws Exception {

        DataSet<Edge<K,Tuple3<EV,N,N>>> newedges = edges.filter(new TemporalSlicer(start,end));

//        slice the vertices as well
        DataSet<Tuple2<K,VV>> vertex1 = newedges.project(0,2);
        DataSet<Tuple2<K,VV>> vertex2 = newedges.project(1,2);
        DataSet<Tuple2<K,VV>> vertexset = vertex1.union(vertex2).distinct();
//        DataSet<Vertex<K,VV>> vertexset2 = vertexset.map(MapFunction)

        return new Tgraph<>(vertices, newedges, context);
    }


    public Graph<K,Tuple2<VV,ArrayList<Long>>,Tuple3<EV,N,N>> getGellyGraph2() {
        DataSet<Vertex<K,Tuple2<VV,ArrayList<Long>>>> newvertices = vertices.map(new MapFunction<Vertex<K, VV>, Vertex<K, Tuple2<VV, ArrayList<Long>>>>() {
            @Override
            public Vertex<K, Tuple2<VV, ArrayList<Long>>> map(Vertex<K, VV> value) throws Exception {
//                Long[] tempar = {};
                ArrayList<Long> tempar = new ArrayList<Long>();
                return new Vertex<K, Tuple2<VV, ArrayList<Long>>>(value.getId(), new Tuple2<>(value.getValue(),tempar));
            }
        });
        Graph<K,Tuple2<VV,ArrayList<Long>>,Tuple3<EV,N,N>> tempgraph = Graph.fromDataSet(newvertices,edges,context);
        return tempgraph;
    }

    // FilterFunction that filters out all Integers smaller than zero.
    public class TemporalSlicer implements FilterFunction<Edge<K,Tuple3<EV,N,N>>> {
        long finish;
        long start;
        public TemporalSlicer(long start, long finish) {
            this.finish = finish;
            this.start = start;
        }

        @Override
        public boolean filter(Edge<K,Tuple3<EV,N,N>> edge) {
//            checks the 2nd field, so the starting time is greater then 3
            return (int) edge.getValue().getField(1) >= start && (int) edge.getValue().getField(2) <= finish;
        }
    }


    /*public Tgraph<K,VV,EV,N> addVertex(final Vertex<K,VV> vertex) throws Exception {
        List<Vertex<K, VV>> newVertex = new ArrayList<Vertex<K, VV>>();
        newVertex.add(vertex);

        return addVertices(newVertex);
    }

    public Tgraph<K,VV,EV,N> addVertices(List<Vertex<K, VV>> verticesToAdd) throws Exception {

        DataSet<Vertex<K, VV>> newVertices = this.vertices.coGroup(this.context.fromCollection(verticesToAdd))
                .where(0).equalTo(0).with(new VerticesUnionCoGroup<K, VV>());

        return new Tgraph<>(newVertices, this.edges, this.context);
    }

    private static final class VerticesUnionCoGroup<K, VV> implements CoGroupFunction<Vertex<K, VV>, Vertex<K, VV>, Vertex<K, VV>> {

        @Override
        public void coGroup(Iterable<Vertex<K, VV>> oldVertices, Iterable<Vertex<K, VV>> newVertices,
                            Collector<Vertex<K, VV>> out) throws Exception {

            final Iterator<Vertex<K, VV>> oldVerticesIterator = oldVertices.iterator();
            final Iterator<Vertex<K, VV>> newVerticesIterator = newVertices.iterator();

            // if there is both an old vertex and a new vertex then only the old vertex is emitted
            if (oldVerticesIterator.hasNext()) {
                out.collect(oldVerticesIterator.next());
            } else {
                out.collect(newVerticesIterator.next());
            }
        }
    }

    public Tgraph<K,VV,EV,N> addEdge(K source, K target, EV edgeValue, N start, N end) {

        List<Edge<K,Tuple3<EV,N,N>>> newEdge = new ArrayList<Edge<K,Tuple3<EV,N,N>>>();
        newEdge.add(new Edge<K, Tuple3<EV, N, N>>(source,target,new Tuple3<EV, N, N>(edgeValue,start,end)));

        return addEdges(newEdge);
    }

    *//**
     * Adds the given list edges to the graph.
     *
     * When adding an edge for a non-existing set of vertices, the edge is considered invalid and ignored.
     *
     * @param newEdges the data set of edges to be added
     * @return a new graph containing the existing edges plus the newly added edges.
     *//*
    public Tgraph<K,VV,EV,N> addEdges(List<Edge<K,Tuple3<EV,N,N>>> newEdges) {

        DataSet<Edge<K,EV>> newEdgesDataSet = this.context.fromCollection(newEdges);

        DataSet<Edge<K,EV>> validNewEdges = this.getVertices().join(newEdgesDataSet)
                .where(0).equalTo(0)
                .with(new Graph.JoinVerticesWithEdgesOnSrc<K, VV, EV>())
                .join(this.getVertices()).where(1).equalTo(0)
                .with(new Graph.JoinWithVerticesOnTrg<K, VV, EV>());

        return Graph.fromDataSet(this.vertices, this.edges.union(validNewEdges), this.context);
    }

    @FunctionAnnotation.ForwardedFieldsSecond("f0; f1; f2")
    private static final class JoinVerticesWithEdgesOnSrc<K, VV, EV> implements
            JoinFunction<Vertex<K, VV>, Edge<K, EV>, Edge<K, EV>> {

        @Override
        public Edge<K, EV> join(Vertex<K, VV> vertex, Edge<K, EV> edge) throws Exception {
            return edge;
        }
    }

    @FunctionAnnotation.ForwardedFieldsFirst("f0; f1; f2")
    private static final class JoinWithVerticesOnTrg<K, VV, EV> implements
            JoinFunction<Edge<K, EV>, Vertex<K, VV>, Edge<K, EV>> {

        @Override
        public Edge<K, EV> join(Edge<K, EV> edge, Vertex<K, VV> vertex) throws Exception {
            return edge;
        }
    }*/




    /**
     * @param algorithm the algorithm to run on the Graph
     * @param <T> the return type
     * @return the result of the graph algorithm
     * @throws Exception
     */
    public <T> T run(TGraphAlgorithm<K, VV, EV,N, T> algorithm) throws Exception {
        return algorithm.run(this);
    }

    /**
     * This operation adds all inverse-direction edges to the graph.
     *
     * @return the undirected graph.
     */
    public Tgraph<K, VV, EV, N> getUndirected() throws Exception {

        return new Tgraph<K, VV, EV, N>(vertices,getGellyGraph().getUndirected().getEdges(),context);
    }

    private static final class RegularAndReversedEdgesMap<K, EV>
            implements FlatMapFunction<Edge<K, EV>, Edge<K, EV>> {

        @Override
        public void flatMap(Edge<K, EV> edge, Collector<Edge<K, EV>> out) throws Exception {
            out.collect(new Edge<K, EV>(edge.f0, edge.f1, edge.f2));
            out.collect(new Edge<K, EV>(edge.f1, edge.f0, edge.f2));
        }
    }

    /*
    Determines the shortest path from @startingnode to every node in the graph
    Stores the results in a Vertex Dataset where the vertex value is te distance

    */
    public DataSet<Vertex<K,Long>> ShortestPathsEAT(K startingnode, Long TimeLowerBound, Long TimeUpperBound) throws Exception {
//      getting the edges and sorting them by their edge value
//      somehow this works, but it should sort on the starting value of the temporal edge, currently
//      it sorts on the tuple
        Map<K,Long> Vertexes = new HashMap<>();

        DataSet<Edge<K,Tuple3<EV,N,N>>> tempedges = this.getTemporalEdges().sortPartition(2, Order.ASCENDING);
        DataSet<Vertex<K,Long>> Tempvertices = this.getVertices().distinct().map(new MapFunction<Vertex<K, VV>, Vertex<K, Long>>() {
            @Override
            public Vertex<K, Long> map(Vertex<K, VV> value) throws Exception {
                Vertexes.put(value.getId(),1L);
                System.out.println(value.getId());
                if (startingnode == value.getId()) {
                    return new Vertex<K, Long>(value.getId(), 0L);
                } else {
                    return new Vertex<K, Long>(value.getId(), Long.MAX_VALUE);
                }
            }
        });
        System.out.println("printing vertexes");
        for(Map.Entry<K,Long> vertex : Vertexes.entrySet()) {
            System.out.println("vertex x");
            System.out.println(vertex.getKey());
            System.out.println(vertex.getValue());
        }
//        tempedges.map(new MapFunction<Edge<K,Tuple3<EV,N,N>>, Object>() {
//        })
        tempedges.print();
        return Tempvertices;
    }

//
}


