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

        /*
        Define some objects like input output files.
         */
        final ExecutionEnvironment env;
        env = ExecutionEnvironment.getExecutionEnvironment();

        int maxiterations = 30;
        String output = "results.txt";
        String input = "..\\..\\..\\..\\datasets\\rita_data.csv";
        input = "C:\\Users\\ia02ui\\IdeaProjects\\Temporal_Graph_library\\datasets\\rita_data.csv";

        /*
        * create a Flink DataSet tuple 6 and import the data from the csv file.
        * with .includeFields("0100000100010010010001") we indicate that we want the 2nd, 8th column etc..
        * The types define the parsing type for importing.
        * */
        DataSet<Tuple6<Double, String, String, Double, Double, Double>> flightSet = env.readCsvFile(input)
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("0100000100010010010001")
                .types(Double.class, String.class, String.class, Double.class, Double.class, Double.class); // read the node IDs as Longs

        /*
        * Since the data is not formatted in the way we want we map it to a dataset that we can use for the graph in the format
        * (Source id, Target id, starting time, ending time)
        *
        * */
        DataSet<Tuple4<String, String, Double, Double>> flightSetMapped = flightSet.map(new MapFunction<Tuple6<Double, String, String, Double, Double, Double>, Tuple4<String, String, Double, Double>>() {
            @Override
            public Tuple4<String, String, Double, Double> map(Tuple6<Double, String, String, Double, Double, Double> v) throws Exception {
                Double starttime = v.f0 + ((v.f3 / 100) * 60  + v.f3 % 100) * 1000;
                Double endtime = v.f0 + ((v.f4 / 100) * 60  + v.f4 % 100) * 1000;
                return new Tuple4<>(v.f1,v.f2,starttime ,endtime);
            }
        });
//        flightSetMapped.first(10).print();

        Tgraph<String, NullValue, NullValue, Double> Rita_graph = Tgraph.From4TupleNoEdgesNoVertexes(flightSetMapped, env);
//
        Rita_graph.getUndirected().run(new SingleSourceShortestTemporalPathEATJustPaths<>("CLE", maxiterations))
                .first(10).print();

    }
}
