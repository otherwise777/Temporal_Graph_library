package Tgraphs;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextOutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;

import java.util.Random;

/**
 * Created by s133781 on 07-Dec-16.
 */
public class CreateTemporalgraph {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env;
        Configuration conf = new Configuration();
        conf.setString("fs.overwrite-files","true");
        env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(1);
        String fileprefix = "C:\\Dropbox\\graphInstances\\";
        String graph = "graph100k";
        Random R = new Random();
        Integer height = 10000;
        String outputfile = "C:\\Dropbox\\tgraphInstances\\tgraph100k.txt";




//
        DataSet<Tuple2<Integer, Integer>> temporalsetdoubles = env.readCsvFile(fileprefix + graph + ".txt")
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("101")
                .types(Integer.class, Integer.class); // read the node IDs as Longs

        DataSet<Tuple3<Integer, Integer, Integer>> newset = temporalsetdoubles.map(
                new MapFunction<Tuple2<Integer, Integer>,Tuple3<Integer, Integer, Integer>>() {
                    @Override
                    public Tuple3<Integer, Integer, Integer> map(Tuple2<Integer, Integer> value) throws Exception {
//                        for the normal distribution
                        Integer timestart = R.nextInt(height);
                        return new Tuple3<>(value.f0, value.f1, timestart);
                    }
                });
        newset.writeAsFormattedText(outputfile, new TextOutputFormat.TextFormatter<Tuple3<Integer, Integer, Integer>>() {
            @Override
            public String format(Tuple3<Integer, Integer, Integer> value) {
                return value.f0 + " " + value.f1 + " " + value.f2;
            }
        });
        env.execute();
    }
}
