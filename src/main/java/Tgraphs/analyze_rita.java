package Tgraphs;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.MapOperator;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple6;
import org.apache.flink.graph.Vertex;
import org.apache.flink.types.NullValue;

import java.util.ArrayList;
import java.util.Date;

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
        env.setParallelism(1);

        int maxiterations = 30;

        String output = "results.txt";
        String input = "    \\datasets\\rita_data.csv";
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
                Double starttime = v.f0 + ((v.f3 / 100) * 60  + v.f3 % 100) * 60;
                Double endtime = v.f0 + ((v.f4 / 100) * 60  + v.f4 % 100) * 60;
                return new Tuple4<>(v.f1,v.f2,starttime ,endtime);
            }
        });

        /*
        * Creating the temporal graph
        * */
        Tgraph<String, NullValue, NullValue, Double> Rita_graph = Tgraph.From4TupleNoEdgesNoVertexes(flightSetMapped, env);

        /*
        *
        * */
        DataSet<Vertex<String, Tuple2<Double, ArrayList<String>>>> result = Rita_graph.getUndirected().run(new SingleSourceShortestTemporalPathEATWithPaths<>("CLE", maxiterations));

        /*
        * Transform unix time to readable date string
        * */
        MapOperator<Vertex<String, Tuple2<Double, ArrayList<String>>>, Vertex<String, Tuple2<String, ArrayList<String>>>> readableResult = result.map(new MapFunction<Vertex<String, Tuple2<Double, ArrayList<String>>>, Vertex<String, Tuple2<String, ArrayList<String>>>>() {
            @Override
            public Vertex<String, Tuple2<String, ArrayList<String>>> map(Vertex<String, Tuple2<Double, ArrayList<String>>> v) throws Exception {
                Date date = new Date(v.f1.f0.longValue()*1000);

                return new Vertex<>(v.f0, new Tuple2<>(date.toString(), new ArrayList<>(v.f1.f1)));
            }
        });

        /*
        * Print first 10 results
        * */
        readableResult.first(10).print();


    }
}
