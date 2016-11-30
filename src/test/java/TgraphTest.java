import Tgraphs.Tgraph;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.types.NullValue;

import static org.junit.Assert.*;

/**
 * Created by s133781 on 24-Oct-16.
 */
public class TgraphTest  {
    private ExecutionEnvironment init() {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        return env;
    }

    @org.junit.Test
    public void from5TupleNoVertexes() throws Exception {
        ExecutionEnvironment env = init();

//        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
//        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
//                .fieldDelimiter(",")  // node IDs are separated by spaces
//                .ignoreComments("%")  // comments start with "%"
//                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
//        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);
//
//        Graph<Long, NullValue, Tuple3<Double, Integer, Integer>> gellygraph = testgraph.getGellyGraph();
//
//        DataSet<Edge<Long,Tuple3<Double, Integer, Integer>>> testedge = gellygraph.getEdges();
//
//
//        Edge<Long,Tuple3<Double, Integer, Integer>> detestedge = new Edge<>(1L,2L,new Tuple3<>(0.6,1,2));
//        DataSet<Edge<Long,Tuple3<Double, Integer, Integer>>> testdata = env.fromElements(detestedge);
////        System.out.println(testdata.);
//        assertEquals(testdata.toString(),testedge.toString());
    }

    @org.junit.Test
    public void from5Tuple() throws Exception {

    }

    @org.junit.Test
    public void from4TupleNoEdgesNoVertexes() throws Exception {

    }

    @org.junit.Test
    public void fromEdgeSet() throws Exception {

    }

    @org.junit.Test
    public void getEdges() throws Exception {

    }

    @org.junit.Test
    public void getVertices() throws Exception {

    }

    @org.junit.Test
    public void numberOfVertices() throws Exception {

    }

    @org.junit.Test
    public void numberOfEdges() throws Exception {

    }

    @org.junit.Test
    public void getGellyGraph() throws Exception {

    }

}