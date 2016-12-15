package Tgraphs.Creatingtemporalgraphs;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextOutputFormat;
import org.apache.flink.api.java.operators.DataSink;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.hadoop.ipc.Server;

import java.io.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.sql.*;

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
        boolean runonetime = true;


//        cleangraphuniform(env,"C:\\Dropbox\\tgraphInstances\\facebook_messages_uncleaned.txt","C:\\Dropbox\\tgraphInstances\\facebook_messages_cleaned_uniform.txt");

//        cleanupFacebookMessageGraphGetResultsquery();

//        cleanupFacebookMessageGraph();
//        cleanWikiEditGraph();
        cleanfacebookgraph(env,"C:\\Dropbox\\tgraphInstances\\facebookfriends_uncleaned.txt","C:\\Dropbox\\tgraphInstances\\tgraph_real_facebookfriends.txt");

        if(runonetime) { return; }
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
    /*
    * Cleans up facebook friendship graph, removes zero values and removes a value to make the set smaller and adding a 4th value, a +1
    * */
    public static void cleanfacebookgraph(ExecutionEnvironment env, String inputfile, String outputfile) throws Exception {
        DataSet<Tuple3<Integer, Integer, Integer>> temporalsetdoubles = env.readCsvFile(inputfile)
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("1101")
                .types(Integer.class, Integer.class, Integer.class); // read the node IDs as Longs

        DataSet<Tuple3<Integer, Integer, Integer>> tempa = temporalsetdoubles.filter(new FilterFunction<Tuple3<Integer, Integer, Integer>>() {
            @Override
            public boolean filter(Tuple3<Integer, Integer, Integer> value) throws Exception {
                if (value.f2 == 0) {
                    return false;
                }
                return true;
            }
        });

//        tempa.minBy(2).print();
        DataSet<Tuple3<Integer, Integer, Integer>> tempb = tempa.map(new MapFunction<Tuple3<Integer, Integer, Integer>, Tuple3<Integer, Integer, Integer>>() {
             @Override
             public Tuple3<Integer, Integer, Integer> map(Tuple3<Integer, Integer, Integer> value) throws Exception {
                 return new Tuple3<Integer, Integer, Integer>(value.f0,value.f1,value.f2 - 1157454928);
             }
         });


        tempb.writeAsFormattedText(outputfile, new TextOutputFormat.TextFormatter<Tuple3<Integer, Integer, Integer>>() {
            @Override
            public String format(Tuple3<Integer, Integer, Integer> value) {
                return value.f0 + " " + value.f1 + " " + value.f2 + " " + (value.f2 + 1);
            }
        });
        env.execute();

    }

    /*
    * this class was created to clean up the wikiedits page, it had edges labels indicating if an edge was removed or added, i transformed
    * it into a temporal graph with time windows of the edge existince.
    * Some of the code is a bit hacky since i reached the upper limits of my systems heap space
    * */
    public static void cleanWikiEditGraph() {



        try {
//            System.out.print(inputfile);

//            File file = new File(input.nextLine());

//            input = new Scanner(file);

//            hasmap with source target as key, and time as result
            HashMap<String,String> allentries = new HashMap<>();




            int i = 0;
            int faultrecords = 0;
            int succesfullwrites = 0;
            int startingminus = 0;
            for (int a = 11; a < 20; a++) {
                String inputfile = "C:\\Dropbox\\tgraphInstances\\wikieditsraw.txt";
                String outputfile = "C:\\Dropbox\\tgraphInstances\\wikieditsnotraw_" + a + ".txt";

                BufferedWriter writer = new BufferedWriter(new FileWriter(outputfile));
                Scanner input = new Scanner(new File(inputfile));
                System.out.println("removing entries: " + allentries.size());
                allentries.clear();
                while (input.hasNextLine()) {
                    String line = input.nextLine();

                    String[] lineelements = line.split(" ");
                    if (Objects.equals(lineelements[0], "%")) {
                        continue;
                    } else {
                        if (lineelements.length < 4) {
                            continue;
                        }
//                    int source = lineelements[0]
//                    int i = source + target % 100;
                        // put in file_i

                        String sourcetarget = lineelements[0] + "_" + lineelements[1];


                        if (sourcetarget.length() == a) {


                            String evalue = lineelements[2];
                            String time = lineelements[3];
//                    if the entry is available
//                    System.out.println("starting line: " + line);
                            if (allentries.containsKey(sourcetarget)) {
                                if (evalue.equals("-1")) {
//                            write it away
//                            System.out.println("match found, writing it to file");
                                    writer.append(sourcetarget.replaceAll("_", " ") + " " + allentries.get(sourcetarget) + " " + time + System.lineSeparator());
                                    allentries.remove(sourcetarget);
                                    succesfullwrites++;
                                } else {
//                            overwrite it if it exists
                                    allentries.put(sourcetarget, time);
                                    faultrecords++;
                                    System.out.println("this shouldnt happen, but overwriting +1");
                                }
                            } else {
//                        it doesnt exist,
                                if (Objects.equals(evalue, "-1")) {
//                            we do nothing since we ignore this case
                                    startingminus++;
                                } else {
//                            create the instance
                                    allentries.put(sourcetarget, time);
                                }
                            }
                            i++;
                            if (i % 50000 == 0) {
                                writer.flush();
                                System.out.println("processing line: " + line);
                            }
                        }

                    }

//                System.out.println(line);

                }
                input.close();
                writer.close();
            }
            System.out.println("Records that were left untouched: " + allentries.size());
            System.out.println("Records that were duplicate: " + faultrecords);
            System.out.println("Records that were succesfully processed: " + succesfullwrites);
            System.out.println("Records that had a removal before adding: " + startingminus);



        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /*
    * This class is used to combine all the different subssets created by the cleanfacebookgraph() class
    * */
    public static void unionWikiEditGraph(ExecutionEnvironment env) throws Exception {

        String inputfilestart = "C:\\Dropbox\\tgraphInstances\\wikieditsnotraw_3.txt";
        String outputfile = "C:\\Dropbox\\tgraphInstances\\wikieditsraw_combined.txt";

        DataSet<Tuple4<Integer, Integer, Integer, Integer>> temporalsetdoubles = env.readCsvFile(inputfilestart)
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("1111")
                .types(Integer.class, Integer.class, Integer.class, Integer.class); // read the node IDs as Longs

        for (int a = 4; a < 20; a++) {
            String inputfile = "C:\\Dropbox\\tgraphInstances\\wikieditsnotraw_" + a + ".txt";


            DataSet<Tuple4<Integer, Integer, Integer, Integer>> tempset = env.readCsvFile(inputfile)
                    .fieldDelimiter(" ")  // node IDs are separated by spaces
                    .ignoreComments("%")  // comments start with "%"
                    .includeFields("1111")
                    .types(Integer.class, Integer.class, Integer.class, Integer.class); // read the node IDs as Longs

            temporalsetdoubles = temporalsetdoubles.union(tempset);
            System.out.println("count of iteration: " + a + " " + temporalsetdoubles.count());

        }
        temporalsetdoubles.writeAsFormattedText(outputfile, new TextOutputFormat.TextFormatter<Tuple4<Integer, Integer, Integer, Integer>>() {
            @Override
            public String format(Tuple4<Integer, Integer, Integer, Integer> value) {
                return value.f0 + " " + value.f1 + " " + value.f2 + " " + value.f3;
            }
        });
        env.execute();
    }
    /*
    * This class reads the file and adds everything to a mysql database to be grouped
    * */
    public static void cleanupFacebookMessageGraph() throws SQLException, ClassNotFoundException {

        String inputfile = "C:\\Dropbox\\tgraphInstances\\facebook_messages_uncleaned.txt";

        try{
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection=DriverManager.getConnection(
                    "jdbc:mysql://localhost:3307/datasets","root","usbw");
//here sonoo is database name, root is username and password
//            Statement stmt=connection.createStatement();
            ArrayList<String> queries = new ArrayList<>();
            Scanner input = new Scanner(new File(inputfile));
            Statement statement = connection.createStatement();
            String appender = "INSERT into wikimsg (src,tar,sta,end) VALUES";
            int i = 0;

            while (input.hasNextLine()) {

                String line = input.nextLine();

                String[] lineelements = line.split(" ");
                if (Objects.equals(lineelements[0], "%")) {
                    continue;
                }
                if (lineelements.length < 4) {
                    continue;
                }

                String src = lineelements[0];
                String tar = lineelements[1];
                String sta = lineelements[3];
                String end = "0";

//                statement.addBatch("INSERT into wikimsg (src,tar,sta,end) VALUES ");
                i++;
                if(i % 5000 == 0) {
                    appender = appender + "(" + src + "," + tar + "," + sta + "," + end + ")";
                    System.out.println("currently at: " + i);
                    statement.addBatch(appender);
                    statement.executeBatch();
                    appender = "INSERT into wikimsg (src,tar,sta,end) VALUES";
                } else {
                    appender = appender + "(" + src + "," + tar + "," + sta + "," + end + "),";
                }

            }
            System.out.println("starting execute");

            statement.executeBatch();
            statement.close();
            connection.close();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static void cleanupFacebookMessageGraphGetResultsquery() throws ClassNotFoundException, SQLException, IOException {

        String outputfile = "C:\\Dropbox\\tgraphInstances\\facebook_messages_cleaned.txt";
        Class.forName("com.mysql.jdbc.Driver");
        Connection connection=DriverManager.getConnection(
                "jdbc:mysql://localhost:3307/datasets","root","usbw");
//here sonoo is database name, root is username and password
//            Statement stmt=connection.createStatement();
        ArrayList<String> queries = new ArrayList<>();
        Statement statement = connection.createStatement();
        String appender = "INSERT into wikimsg (src,tar,sta,end) VALUES";
        int i = 0;

        ArrayList<Tuple4<String,String,Integer,Integer>> resulttuples = new ArrayList<>();
        ResultSet results = statement.executeQuery("SELECT * FROM  `wikimsg` WHERE 1 ORDER BY src, tar, sta");
        String lastsource = "";
        String lasttarget = "";
        Integer lasttime = 0;
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputfile));
        while (results.next()) {
            i++;
            String source = results.getString(1);
            String target = results.getString(2);
            Integer start = results.getInt(3);
//            check if we have the same element as before
            if(source.equals(lastsource) && target.equals(lasttarget)) {
//                both messages happend within 24hours
                if(lasttime + 86400 > start) {
                    writer.append(source + " " + target + " " + lasttime + " " + start + System.lineSeparator());
//                    resulttuples.add(new Tuple4<>(source,target,lasttime,start));
                }
                lasttime = start;
            } else {
                lastsource = source;
                lasttarget = target;
            }


        }
        writer.close();

    }

    /*
    * Transforms a graph with time instances to a uniformly distributed time windowed graph
    * */
    public static void cleangraphuniform(ExecutionEnvironment env, String inputfile, String outputfile) throws Exception {
        DataSet<Tuple3<Integer, Integer, Integer>> temporalsetdoubles = env.readCsvFile(inputfile)
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("1101")
                .types(Integer.class, Integer.class, Integer.class); // read the node IDs as Longs

        temporalsetdoubles.writeAsFormattedText(outputfile, new TextOutputFormat.TextFormatter<Tuple3<Integer, Integer, Integer>>() {
            @Override
            public String format(Tuple3<Integer, Integer, Integer> value) {
                return value.f0 + " " + value.f1 + " " + value.f2 + " " + (value.f2 + 1);
            }
        });
        env.execute();

    }
}
