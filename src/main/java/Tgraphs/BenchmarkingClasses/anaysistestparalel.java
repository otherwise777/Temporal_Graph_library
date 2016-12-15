package Tgraphs.BenchmarkingClasses;

import Tgraphs.SSSTPCloseness;
import Tgraphs.Tgraph;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.NullValue;

import java.io.FileWriter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by s133781 on 07-Dec-16.
 */
public class anaysistestparalel {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env;

        Configuration conf = new Configuration();
        conf.setString("fs.overwrite-files","true");



        int testsPerLoop = 10;
        Integer[] paralels = {1,2,3,4,5,6,7,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        int maxiterations = 30;
        String resultfile = args[1];
        String fileprefix = args[0];
        String junkoutputfile = args[3];
        String graph = "tgraph1m_uniform_100k";


//        logger
        FileWriter writer = new FileWriter(resultfile,true);
        writer.append("test round with undirected graphs" + System.lineSeparator());
        writer.append("graph iteration  paralel running_time" + System.lineSeparator());
        writer.close();

        for( Integer paralel: paralels) {
            env = ExecutionEnvironment.createLocalEnvironment(conf);
            env.setParallelism(paralel);
            DataSet<Tuple4<Integer, Integer, Double, Double>> temporalsetdoubles = env.readCsvFile(fileprefix + graph + ".txt")
                    .fieldDelimiter(" ")  // node IDs are separated by spaces
                    .ignoreComments("%")  // comments start with "%"
                    .includeFields("1111")
                    .types(Integer.class,Integer.class, Double.class, Double.class); // read the node IDs as Longs


            Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles, env);
            for (int i = 0; i <= testsPerLoop; i++) {
//                temporalGraphfullset.getUndirected().run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations)).first(1).writeAsText(junkoutputfile);
                temporalGraphfullset.getUndirected().run(new SSSTPCloseness<>(maxiterations, 1, true)).first(1).writeAsText(junkoutputfile);
                Long runningtime = env.execute().getNetRuntime();

                FileWriter writer2 = new FileWriter(resultfile, true);
                writer2.append(graph + " " + i + " " + paralel + " " + runningtime + System.lineSeparator());
                writer2.close();
            }
        }

    }
}
