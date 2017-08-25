package Tgraphs.BenchmarkingClasses;

import Tgraphs.SingleSourceShortestTemporalPathEAT;
import Tgraphs.Tgraph;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Vertex;
import org.apache.flink.types.NullValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class for doing some benchmarking tests with the EAT algorithm
 * The class iterates over the different graphs indicated by @param graphs
 * the class then appends the results in the results.txt file for the results
 *
 */
public class analysistest {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env;
        if(args.length < 4) {
            Configuration conf = new Configuration();
            conf.setString("fs.overwrite-files","true");
            env = ExecutionEnvironment.createLocalEnvironment(conf);
        } else {
            env = ExecutionEnvironment.getExecutionEnvironment();
        }



        int paralelInstances = 1;
        int testsPerLoop = 1;
        int maxiterations = 30;
        String resultfile = "results.txt";
        String fileprefix = "C:\\Dropbox\\tgraphInstances\\graph1m\\";
        String junkoutputfile = "junk.txt";
        String[] graphs = {
                "tgraph1m_constant_0.txt",
        };
        env.setParallelism(paralelInstances);

//        logger
        FileWriter writer = new FileWriter(resultfile,true);
//        writer.append("test results" + System.lineSeparator());
        writer.append("direction graph iteration  paralel running_time" + System.lineSeparator());
        writer.close();
        Random R = new Random();
        for(String graph : graphs) {
            DataSet<Tuple4<Integer, Integer, Double, Double>> temporalsetdoubles = env.readCsvFile(fileprefix + graph)
                    .fieldDelimiter(" ")  // node IDs are separated by spaces
                    .ignoreComments("%")  // comments start with "%"
                    .includeFields("1111")
                    .types(Integer.class, Integer.class, Double.class, Double.class); // read the node IDs as Longs

            Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles, env);
            for(int i = 1; i <= testsPerLoop; i++) {
                temporalGraphfullset.getUndirected().run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations))
                        .first(1).writeAsText("junk\\di" + i + graph + junkoutputfile);;
                Long runningtime = env.execute().getNetRuntime();

                FileWriter writer2 = new FileWriter(resultfile,true);
                writer2.append("undirected " + graph + " " + i + " " + paralelInstances + " " + runningtime + System.lineSeparator());
                writer2.close();
            }
            for(int i = 1; i <= testsPerLoop; i++) {
                temporalGraphfullset.run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations)).first(1).writeAsText("junk\\ud" + i + graph + junkoutputfile);;
                Long runningtime = env.execute().getNetRuntime();
//                env.getRuntimeContext().getMetricGroup()

                FileWriter writer2 = new FileWriter(resultfile,true);
                writer2.append("directed " + graph + " " + i + " " + paralelInstances + " " + runningtime + System.lineSeparator());
                writer2.close();
            }
        }


    }
}
