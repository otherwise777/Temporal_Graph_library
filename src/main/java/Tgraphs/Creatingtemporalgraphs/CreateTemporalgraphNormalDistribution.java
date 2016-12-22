package Tgraphs.Creatingtemporalgraphs;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextOutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.NullValue;

import java.io.FileWriter;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by s133781 on 07-Dec-16.
 */
public class CreateTemporalgraphNormalDistribution {
    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env;
        Configuration conf = new Configuration();
        conf.setString("fs.overwrite-files","true");
        env = ExecutionEnvironment.createLocalEnvironment(conf);
        env.setParallelism(1);
        String fileprefix = "C:\\Dropbox\\tgraphInstances\\realgraph\\";
        String graph = "tgraph_real_facebookmsges_uniform";
        NormalDistribution d;
        Integer setlength = 1000000;
        Integer height = 100000;
        Random R = new Random();
        Integer variancedivider = 24;
        String outputfile = "C:\\Dropbox\\tgraphInstances\\realgraph\\tgraph1m_normal_mean_1m_sd_" + variancedivider + ".txt";

//        distrbution, first number is the n / 2,
        d = new NormalDistribution(setlength / 2,setlength / variancedivider);
        Double multiplier = height / d.density(setlength / 2);


//
        DataSet<Tuple3<Integer, Integer, Integer>> temporalsetdoubles = env.readCsvFile(fileprefix + graph + ".txt")
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("111")
                .types(Integer.class, Integer.class, Integer.class); // read the node IDs as Longs

        DataSet<Tuple4<Integer, Integer, Integer, Integer>> newset = temporalsetdoubles.map(
                new MapFunction<Tuple3<Integer, Integer, Integer>, Tuple4<Integer, Integer, Integer, Integer>>() {
                    @Override
                    public Tuple4<Integer, Integer, Integer, Integer> map(Tuple3<Integer, Integer, Integer> value) throws Exception {
//                        for the normal distribution
                        Integer timestart = value.f2;
                        Integer randomtimestart = R.nextInt(1000000);
                        Double time = d.density(randomtimestart) * multiplier + timestart;

                        return new Tuple4<>(value.f0, value.f1, timestart, time.intValue());
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
