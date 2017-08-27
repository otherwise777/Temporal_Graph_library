package Tgraphs;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.types.NullValue;

/**
 * Class for doing some benchmarking tests with the EAT algorithm
 * The class iterates over the different graphs indicated by @param graphs
 * the class then appends the results in the results.txt file for the results
 *
 */
public class analyze_rita {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env;
        env = ExecutionEnvironment.getExecutionEnvironment();

        int paralelInstances = 1;
        int testsPerLoop = 1;
        int maxiterations = 30;
        String output = "results.txt";
        String input = "..\\..\\..\\..\\datasets\\rita_data.csv";
        input = "C:\\Users\\ia02ui\\IdeaProjects\\Temporal_Graph_library\\datasets\\rita_data.csv";


        DataSet<Tuple6<Double, String, String, Double, Double, Double>> flightSet = env.readCsvFile(input)
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("0100000100010010010001")
                .types(Double.class, String.class, String.class, Double.class, Double.class, Double.class); // read the node IDs as Longs


        DataSet<Tuple4<String, String, Double, Double>> flightSetMapped = flightSet.map(new MapFunction<Tuple6<Double, String, String, Double, Double, Double>, Tuple4<String, String, Double, Double>>() {
            @Override
            public Tuple4<String, String, Double, Double> map(Tuple6<Double, String, String, Double, Double, Double> v) throws Exception {
                return new Tuple4<>(v.f1,v.f2,v.f0,v.f4 + v.f0);
            }
        });
        flightSetMapped.first(1).print();

        Tgraph<String, NullValue, NullValue, Double> Rita_graph = Tgraph.From4TupleNoEdgesNoVertexes(flightSetMapped, env);

        Rita_graph.getUndirected().run(new SingleSourceShortestTemporalPathEAT<>("CLE", maxiterations))
                .first(10).print();
    }
}
