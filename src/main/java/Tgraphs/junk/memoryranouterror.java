package Tgraphs.junk;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.PageRank;
import org.apache.flink.graph.library.SingleSourceShortestPaths;
import org.apache.flink.types.NullValue;

/**
 * Created by s133781 on 30-Nov-16.
 */
public class memoryranouterror {
    public static void main(String[] args) throws Exception {
//        Configuration conf = new Configuration();
//        conf.setString(ConfigConstants.TASK_MANAGER_MEMORY_OFF_HEAP_KEY, "2000");

//        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataSet<Tuple2<Long, Long>> twitterEdges = env.readCsvFile("C:\\git\\Temporal_Graph_library\\datasets\\out.munmun_twitter_social")
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class); // read the node IDs as Longs

// Step #2: Create a Graph and initialize vertex values
        Graph<Long, Double, NullValue> graph = Graph.fromTuple2DataSet(twitterEdges, new testclass.InitVertices(), env);
        Graph<Long, Double, Double> newgraph = graph.mapEdges(new MapFunction<Edge<Long, NullValue>, Double>() {
            @Override
            public Double map(Edge<Long, NullValue> longNullValueEdge) throws Exception {
                return 1D;
            }
        });

        Long source = 0L;
        DataSet<Vertex<Long, Double>> verticesWithCommunity = newgraph.run(new SingleSourceShortestPaths<Long>(source,30));
        verticesWithCommunity.print();
    }
}
