package Tgraphs.BenchmarkingClasses;

import Tgraphs.SingleSourceShortestTemporalPathEAT;
import Tgraphs.Tgraph;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.NullValue;

import java.io.FileWriter;

/**
 * Created by s133781 on 07-Dec-16.
 */
public class anaysistestparalel {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env;

        Configuration conf = new Configuration();
        conf.setString("fs.overwrite-files","true");



        int testsPerLoop = 10;
        Integer[] paralels = {1};
        int maxiterations = 30;
        String resultfile = "results.txt";
        String fileprefix = "C:\\Dropbox\\tgraphInstances\\realgraph\\";
        String junkoutputfile = "junk.txt";
//        String graph = "tgraph1m_uniform_100k";
        String[] graphs = {
                "tgraph_real_wikiedithyperlinks.txt"
//                "tgraph_real_facebookfriends_uniform.txt",
//                "tgraph_real_facebookmsges_intervalgraph.txt",
//                "tgraph_real_facebookmsges_uniform.txt",
//                "tgraph1m_uniform_1000.txt",
//                "tgraph1m_uniform_10000.txt",
//                "tgraph1m_normal_mean_1m_sd_6.txt",
//                "tgraph1m_normal_mean_1m_sd_12.txt",
//                "tgraph1m_normal_mean_1m_sd_24.txt",
//                "tgraph1m_normal_mean_1m_sd_48.txt",
//                "tgraph1m_constant_0.txt",
//                "tgraph1m_constant_1.txt",
//                "tgraph1m_constant_10.txt",
//                "tgraph1m_constant_100.txt",
//                "tgraph1m_constant_1000.txt",
//                "tgraph1m_constant_10000.txt",
//                "tgraph1m_constant_100000.txt",
//                "tgraph1m_zipfian_0.txt",
//                "tgraph1m_zipfian_1.txt"
        };

//        logger
        FileWriter writer = new FileWriter(resultfile,true);
//        writer.append("test round with undirected graphs" + System.lineSeparator());
        writer.append("graph iteration paralel running_time" + System.lineSeparator());
        writer.close();
        for( String graph: graphs) {
            for (Integer paralel : paralels) {
                env = ExecutionEnvironment.createLocalEnvironment(conf);
                env.setParallelism(paralel);
                DataSet<Tuple4<Integer, Integer, Double, Double>> temporalsetdoubles = env.readCsvFile(fileprefix + graph)
                        .fieldDelimiter(" ")  // node IDs are separated by spaces
                        .ignoreComments("%")  // comments start with "%"
                        .includeFields("1111")
                        .types(Integer.class, Integer.class, Double.class, Double.class); // read the node IDs as Longs


                Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles, env);
                System.out.println(temporalGraphfullset.getVertices().count());
                if(testsPerLoop == 10) {return; }
                Long totaltime = 0L;
                for (int i = 0; i <= testsPerLoop; i++) {
                    temporalGraphfullset.getUndirected().run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations)).first(1).writeAsText("junk\\" + graph + paralel + i + junkoutputfile);
//                temporalGraphfullset.getUndirected().run(new SSSTPCloseness<>(maxiterations, 1, true)).first(1).writeAsText(junkoutputfile);
                    Long runningtime = env.execute().getNetRuntime();
                    if (i != 0) {
                        totaltime = totaltime + runningtime;
                    }
                }
//            stores the total running time of 10 tests, excluding the first
                FileWriter writer2 = new FileWriter(resultfile, true);
                writer2.append(graph + " " + paralel + " " + totaltime + System.lineSeparator());
                writer2.close();

            }
        }

    }
}
