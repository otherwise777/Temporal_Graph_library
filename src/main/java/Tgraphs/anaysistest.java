package Tgraphs;

import org.apache.flink.api.common.functions.MapFunction;
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
 * Created by s133781 on 07-Dec-16.
 */
public class anaysistest {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env;
        if(args.length > 4) {
            Configuration conf = new Configuration();
            conf.setString("fs.overwrite-files","true");
            env = ExecutionEnvironment.createLocalEnvironment(conf);
        } else {
            env = ExecutionEnvironment.getExecutionEnvironment();
        }

        int paralelInstances = Integer.parseInt(args[2]);
        int testsPerLoop = 10;
        int maxiterations = 30;
        String resultfile = args[1];
        String fileprefix = args[0];
        String junkoutputfile = args[3];
//        String[] graphs = {"graph10k","graph100k","graph1m","graph10m","sparse10k","sparse100k","sparse1m","sparse10m","sparse50m"};
//        String[] graphs = {"sparse10k","sparse100k","sparse1m","sparse10m"};
        String[] graphs = {"graph1m"};
        env.setParallelism(paralelInstances);

//        logger
        FileWriter writer = new FileWriter(resultfile,true);
        writer.append("test round with undirected graphs" + System.lineSeparator());
        writer.append("graph iteration  paralel running_time" + System.lineSeparator());
        writer.close();

        for(String graph : graphs) {
            DataSet<Tuple2<Integer, Integer>> temporalsetdoubles = env.readCsvFile(fileprefix + graph + ".txt")
                    .fieldDelimiter(" ")  // node IDs are separated by spaces
                    .ignoreComments("%")  // comments start with "%"
                    .includeFields("101")
                    .types(Integer.class, Integer.class); // read the node IDs as Longs

            DataSet<Tuple4<Integer, Integer, Double, Double>> newset = temporalsetdoubles.map(
                    new MapFunction<Tuple2<Integer, Integer>, Tuple4<Integer, Integer, Double, Double>>() {
                        @Override
                        public Tuple4<Integer, Integer, Double, Double> map(Tuple2<Integer, Integer> value) throws Exception {
                            Double time =  ThreadLocalRandom.current().nextDouble(1000);
                            return new Tuple4<>(value.f0, value.f1, time, time + 5);
                        }
                    });
            Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(newset, env);
            for(int i = 0; i <= testsPerLoop; i++) {
                temporalGraphfullset.getUndirected().run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations)).first(1).writeAsText(junkoutputfile);;
                Long runningtime = env.execute().getNetRuntime();

                FileWriter writer2 = new FileWriter(resultfile,true);
                writer2.append(graph + " " + i + " " + paralelInstances + " " + runningtime + System.lineSeparator());
                writer2.close();
            }
        }


    }
}
