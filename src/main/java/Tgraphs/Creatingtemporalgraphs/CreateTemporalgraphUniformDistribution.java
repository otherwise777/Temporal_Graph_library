package Tgraphs.Creatingtemporalgraphs;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextOutputFormat;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;

import java.util.Random;

/**
 * Created by s133781 on 07-Dec-16.
 */
public class CreateTemporalgraphUniformDistribution {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env;
        Configuration conf = new Configuration();
        conf.setString("fs.overwrite-files","true");
        env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(1);
        Random R = new Random();
        String fileprefix = "C:\\Dropbox\\tgraphInstances\\realgraph\\";
        String graph = "tgraph_real_facebookmsges_uniform";
        Integer distributionamount = 100000;
        String outputfile = "C:\\Dropbox\\tgraphInstances\\realgraph\\tgraph1m_uniform_" + distributionamount + ".txt";

//
        DataSet<Tuple3<Integer, Integer, Integer>> temporalsetdoubles = env.readCsvFile(fileprefix + graph + ".txt")
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("1110")
                .types(Integer.class, Integer.class, Integer.class); // read the node IDs as Longs

        DataSet<Tuple4<Integer, Integer, Integer, Integer>> newset = temporalsetdoubles.map(
                new MapFunction<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Integer, Integer, Integer>>() {
                    @Override
                    public Tuple4<Integer, Integer, Integer, Integer> map(Tuple3<Integer, Integer, Integer> value) throws Exception {
                        Integer timestart = value.f2;
                        Integer timeend = R.nextInt(distributionamount) + timestart;
                        return new Tuple4<>(value.f0, value.f1, timestart, timeend);
                    }
                });
        newset.writeAsFormattedText(outputfile, new TextOutputFormat.TextFormatter<Tuple4<Integer, Integer, Integer, Integer>>() {
            @Override
            public String format(Tuple4<Integer, Integer, Integer, Integer> value) {
                return value.f0 + " " + value.f1 + " " + value.f2 + " " + value.f3;
            }
        });
        env.execute();
    }
}
