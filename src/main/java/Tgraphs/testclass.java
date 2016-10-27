package Tgraphs;

import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.FunctionAnnotation;
import org.apache.flink.api.java.operators.JoinOperator;
import org.apache.flink.api.java.operators.ProjectOperator;
import org.apache.flink.api.java.tuple.*;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.LabelPropagation;
import org.apache.flink.types.NullValue;
import org.apache.flink.util.Collector;

/**
 * Created by s133781 on 24-Oct-16.
 */
public class testclass {
    public static void main(String[] args) throws Exception {
        System.out.println("and so the testing begins");
        test11();
    }


    private static void test8() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);


        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);

        Graph<Long, NullValue, Tuple3<Double, Integer, Integer>> gellygraph = testgraph.getGellyGraph();
        DataSet<Edge<Long,Tuple3<Double, Integer, Integer>>> testedge = gellygraph.getEdges().first(1);
        testedge.print();
//        System.out.println(testedge.toString());
    }
    /**
     * Initializes the vertex values with the vertex ID
     */
    public static final class InitVertices implements MapFunction<Long, Long> {

        @Override
        public Long map(Long vertexId) {
            return vertexId;
        }
    }

    private static void test9() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);
        Tgraph<Long, NullValue, Double, Integer> slicedgraph;
//        testgraph.getTemporalEdges().print();
//        slicedgraph.getTemporalEdges().print();
//        slicedgraph.getVertices().print();
        testgraph.getGraphSlice(6,7).getTemporalEdges().print();
        testgraph.getGraphSlice2(6,7).getTemporalEdges().print();

        testgraph.getGraphSlice(6,7).getVertices().print();
        testgraph.getGraphSlice2(6,7).getVertices().print();
    }
    private static void test10() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);
//        Tgraph<Long, NullValue, Double, Integer> slicedgraph;
        Graph<Long, NullValue, Tuple3<Double, Integer, Integer>> gellygraph = testgraph.getGellyGraph();
//gellygraph.run(new LabelPropagation<>())
//        testgraph.getGraphSlice2(2, 7).getEdges().print();
//        testgraph.getEdges().print();

//        testgraph.getVertices().join(testgraph.getGraphSlice(2, 7).getEdges()).where(0).equalTo(0).with(new
//                ProjectEdge<Long, NullValue, Tuple3<Double, Integer, Integer>> );

        DataSet<Tuple2<Vertex<Long, NullValue>, Edge<Long, Double>>> test = testgraph.getVertices().join(testgraph.getGraphSlice(2, 7).getEdges()).where(0).equalTo(0);
        DataSet<Tuple1<Vertex<Long, NullValue>>> test2 = test.project(0);
        DataSet<Vertex<Long, NullValue>> test3 = test2.map(new MapFunction<Tuple1<Vertex<Long, NullValue>>, Vertex<Long, NullValue>>() {
            @Override
            public Vertex<Long, NullValue> map(Tuple1<Vertex<Long, NullValue>> value) throws Exception {
                return value.getField(0);
            }
        });
        test3.distinct().print();
    }

    private static void test11() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple4<Long,Long, Long, Long>> temporalset = env.readCsvFile("./datasets/Testgraph")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Long.class,Long.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, NullValue, Long> tempgraph = Tgraph.From4TupleNoEdgesNoVertexes(temporalset,env);


        DataSet<Vertex<Long, NullValue>> startnodeset = tempgraph.getVertices().first(1);
        Long test = tempgraph.ShortestPathsEAT(1L).collect().get(0).getId();
//        System.out.println(test);

//        tempgraph.getTemporalEdges().print();
//        tempgraph.ShortestPathsEAT(test)
//        tempgraph.ShortestPathsEAT(test).print();



    }
}
