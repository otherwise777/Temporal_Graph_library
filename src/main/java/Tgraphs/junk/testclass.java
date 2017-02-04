package Tgraphs.junk;

import Tgraphs.*;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.*;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.graph.library.GSAPageRank;
import org.apache.flink.graph.library.LabelPropagation;
import org.apache.flink.graph.library.PageRank;
import org.apache.flink.graph.library.SingleSourceShortestPaths;
import org.apache.flink.graph.spargel.MessageIterator;
import org.apache.flink.graph.spargel.GatherFunction;
import org.apache.flink.graph.spargel.ScatterFunction;
import org.apache.flink.types.NullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by s133781 on 24-Oct-16.
 */
public class testclass {
    private static String[] arguments;

    public testclass() throws Exception {
    }


    public static void main(String[] args) throws Exception {
        arguments = args;
        System.out.println("and so the testing begins");
        test13();
    }
    private static void ssstpeatExample() throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataSet<Tuple4<Integer, Integer, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph2")
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Integer.class, Integer.class, Double.class, Double.class); // read the node IDs as Longs

        Tgraph<Integer, NullValue, NullValue, Double> temporalGraph = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles, env);
        temporalGraph.run(new SingleSourceShortestTemporalPathEAT<>(1, 30)).first(20).print();
    }

    public static final class InitVerticesforInteger implements MapFunction<Integer, Double> {

        @Override
        public Double map(Integer vertexId) {
            return 0D;
        }
    }

    private static void test8() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);

        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);


        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);

        Graph<Long, NullValue, Tuple3<Double, Integer, Integer>> gellygraph = testgraph.getGellyGraph();
        DataSet<Edge<Long,Tuple3<Double, Integer, Integer>>> testedge = gellygraph.getEdges().first(1);
        testedge.print();
//        System.out.println(testedge.toString());
    }
    /**
     * Initializes the vertex values with the vertex ID
     */

    private static void test9() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);
        Tgraph<Long, NullValue, Double, Integer> slicedgraph;
//        testgraph.getTemporalEdges().print();
//        slicedgraph.getTemporalEdges().print();
//        slicedgraph.getVertices().print();
        testgraph.getGraphSlice(6,7).getTemporalEdges().print();
        testgraph.getGraphSlice2(6,7).getTemporalEdges().print();

        testgraph.getGraphSlice(6,7).getVertices().print();
        testgraph.getGraphSlice2(6,7).getVertices().print();
    }
    private static void test10() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long,Long, Double,Integer, Integer>> temporalset = env.readCsvFile("./datasets/testdata")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Double.class,Integer.class,Integer.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, Double, Integer> testgraph = Tgraph.From5TupleNoVertexes(temporalset,env);
//        Tgraph<Long, NullValue, Double, Integer> slicedgraph;
        Graph<Long, NullValue, Tuple3<Double, Integer, Integer>> gellygraph = testgraph.getGellyGraph();
//gellygraph.run(new LabelPropagation<>())
//        testgraph.getGraphSlice2(2, 7).getEdges().print();
//        testgraph.getEdges().print();

//        testgraph.getVertices().join(testgraph.getGraphSlice(2, 7).getEdges()).where(0).equalTo(0).with(new
//                ProjectEdge<Long, NullValue, Tuple3<Double, Integer, Integer>> );

        DataSet<Tuple2<Vertex<Long, NullValue>, Edge<Long, Double>>> test = testgraph.getVertices().join(testgraph.getGraphSlice(2, 7).getEdges()).where(0).equalTo(0);
        DataSet<Tuple1<Vertex<Long, NullValue>>> test2 = test.project(0);
        DataSet<Vertex<Long, NullValue>> test3 = test2.map(new MapFunction<Tuple1<Vertex<Long, NullValue>>, Vertex<Long, NullValue>>() {
            @Override
            public Vertex<Long, NullValue> map(Tuple1<Vertex<Long, NullValue>> value) throws Exception {
                return value.getField(0);
            }
        });
        test3.distinct().print();
    }

    private static void test11() throws Exception {
        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple4<Long,Long, Long, Long>> temporalset = env.readCsvFile("./datasets/Testgraph")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class,Long.class,Long.class,Long.class); // read the node IDs as Longs
        Tgraph<Long, NullValue, NullValue, Long> tempgraph = Tgraph.From4TupleNoEdgesNoVertexes(temporalset,env);


        DataSet<Vertex<Long, NullValue>> startnodeset = tempgraph.getVertices().first(1);
        Long test = tempgraph.ShortestPathsEAT(1L,Long.MIN_VALUE,Long.MAX_VALUE).collect().get(0).getId();
//        Long test2 = tempgraph.ShortestPathsEAT(1L,Long.MIN_VALUE,Long.MAX_VALUE).collect().get(0).getId();
//        Long test3 = tempgraph.ShortestPathsEAT(1L,Long.MIN_VALUE,Long.MAX_VALUE).collect().get(0).getId();
//        System.out.println(test + test2 + test3);
        System.out.println(test);

//        tempgraph.getTemporalEdges().print();
//        tempgraph.ShortestPathsEAT(test)
//        tempgraph.ShortestPathsEAT(test).print();



    }
    public static void test12() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);

        // a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple5<Long, Long, Long, Long, Long>> temporalset = env.readCsvFile("./datasets/Testgraph")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class, Long.class, Long.class, Long.class, Long.class); // read the node IDs as Longs

        Tgraph<Long, Double, Long, Long> tempgraph = Tgraph.From5TuplewithEdgesandVertices(temporalset,new InitVertices(),env);
//        tempgraph.getTemporalEdges().print();
//        tempgraph.getVertices().print();
//        Graph<Long, Double, Tuple3<Long, Long, Long>> temporalgraph = tempgraph.getGellyGraph();


        // read the input graph


// define the maximum number of iterations
        int maxIterations = 5;

// Execute the vertex-centric iteration
//        Graph<Long, NullValue, Tuple3<NullValue, Long, Long>> result = graph.runVertexCentricIteration(
//                new MinDistanceMessenger(), new VertexDistanceUpdater(), maxIterations);

// a temporal set created with Flink, now we need to make it into a temporal set into gelly
        DataSet<Tuple3<Long, Long, Double>> egetuples = env.readCsvFile("./datasets/Testgraph")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class, Long.class, Double.class); // read the node IDs as Longs

//        DataSet<Tuple2<Long,Double>> Vertextuples = env.readCsvFile("./datasets/Testgraph")
//                .fieldDelimiter(",")  // node IDs are separated by spaces
//                .ignoreComments("%")  // comments start with "%"
//                .types(Long.class, Double.class); // read the node IDs as Longs

        Graph<Long, Double, Double> graph2 = Graph.fromTupleDataSet(egetuples,new InitVertices(),env);
        Graph<Long, Double, Tuple3<Long,Long,Long>> graph3 = tempgraph.getGellyGraph();
        Graph<Long, Double, Tuple3<Long,Long,Long>> graph4 = graph3.runScatterGatherIteration(
                new MinDistanceMessengerforTuple(), new VertexDistanceUpdater(), maxIterations);

//        graph4.getVertices().print();
        Long sourcevertex = 1L;
        Integer maxit = 5;
        graph2.run(new SingleSourceShortestPaths<Long>(sourcevertex,maxit)).print();

//        graph2.getEdges().print();
//        graph2.getVertices().print();
//        graph2.getEdges().print();
//        graph2.getVertices().print();

//        Graph<Long, Double, Long> result = graph2.runScatterGatherIteration(
//                new MinDistanceMessenger(), new VertexDistanceUpdater(), maxIterations);
//        result.getVertices().print();

// Extract the vertices as the result
//        result.getVertices().print();

        Graph<Long, Tuple2<Double,ArrayList<Long>>, Tuple3<Long,Long,Long>> graph6 = tempgraph.getGellyGraph2();

        Graph<Long, Tuple2<Double,ArrayList<Long>>, Tuple3<Long,Long,Long>> graph7 = graph6.runScatterGatherIteration(
                new MinDistanceMessengerforTuplewithpath(), new VertexDistanceUpdaterwithpath(), maxIterations);

        Graph<Long, Double, Tuple3<Long,Long,Long>> graph8 = tempgraph.getGellyGraph();


        DataSet<Tuple5<Long, Long, Double, Double, Long>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(Long.class, Long.class, Double.class, Double.class, Long.class); // read the node IDs as Longs

        Tgraph<Long, Double, Long, Double> tempgraphdoubles = Tgraph.From5TuplewithEdgesandVertices(temporalsetdoubles,new InitVertices(),env);


        DataSet<Vertex<Long,Tuple2<Double,ArrayList<Long>>>> verticess = tempgraphdoubles.run(new SingleSourceShortestTemporalPathEATWithPaths<>(1L,maxIterations));

        verticess.print();

// - - -  UDFs - - - //
    }

/*
* Test with testgraph2, single shortset path EAT with paths
* */
    public static void test13() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        int maxIterations = 10;

        DataSet<Tuple4<String, String, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph2")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(String.class, String.class, Double.class, Double.class); // read the node IDs as Longs
        Tgraph<String, Double, NullValue, Double> tempgraphdoubles = Tgraph.From4TupleNoEdgesWithVertices(temporalsetdoubles,new InitVerticesfordoubles(),env);

        Graph<String, Double, Double> tempgraph = tempgraphdoubles.getGellyGraph().mapVertices(new InitVerticeNullfordoublesstring()).mapEdges(new MapFunction<Edge<String, Tuple3<NullValue, Double, Double>>, Double>() {
            @Override
            public Double map(Edge<String, Tuple3<NullValue, Double, Double>> stringTuple3Edge) throws Exception {
                return 1D;
            }
        });
        tempgraph.run(new PageRank<>(0.85,30)).print();
//        tempgraph.run(new LabelPropagation<>())

//        DataSet<Vertex<String,Tuple2<Double,ArrayList<String>>>> verticess = tempgraphdoubles.run(new SingleSourceShortestTemporalPathEATWithPaths<>("A",maxIterations));

//        verticess.print();
    }
    /*
* Test with testgraph2, single shortset path EAT without paths
* */
    public static void test14() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        int maxIterations = 5;

        DataSet<Tuple4<String, String, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph2")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(String.class, String.class, Double.class, Double.class); // read the node IDs as Longs
        Tgraph<String, NullValue, NullValue, Double> tempgraphdoubles = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles,env);

        tempgraphdoubles.run(new SSSTPBetweenness<>(maxIterations)).print();

//        verticess.print();
    }
    public static void test15() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 16000);
//        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_SEGMENT_SIZE_KEY, 64000);
//        conf.setFloat(ConfigConstants.Tas, 64000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        int maxIterations = 5;

        DataSet<Tuple4<String, String, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph2")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(String.class, String.class, Double.class, Double.class); // read the node IDs as Longs
        Tgraph<String, NullValue, NullValue, Double> TemporalGraph = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles,env);
//        tempgraphdoubles.run(new SingleSourceShortestTemporalPathSTTJustPaths<>("A",maxIterations)).print();
//        DataSet<Vertex<String, ArrayList<String>>> temp = tempgraphdoubles.run(new SingleSourceShortestTemporalPathSTTJustPaths<>("A", maxIterations));
//        DataSet<Vertex<String, ArrayList<String>>> test = temp.reduce(new ReduceFunction<Vertex<String, ArrayList<String>>>() {
//            @Override
//            public Vertex<String, ArrayList<String>>
//            reduce(Vertex<String, ArrayList<String>> value1,
//                   Vertex<String, ArrayList<String>> value2) throws Exception {
//                ArrayList<String> stringlist = new ArrayList<>(value1.getValue());
//                stringlist.addAll(value2.getValue());
//                return new Vertex<>("", stringlist);
//            }
//        });
//        determining the betweenness

//        String testasda = "ts";
//        MapOperator<Vertex<String, NullValue>, Object> map = tempgraphdoubles.getVertices().map(new MapFunction<Vertex<String, NullValue>, Object>(tempgraphdoubles) {
//            @Override
//            public Object map(Vertex<String, NullValue> value) throws Exception {
//                return null;
//            }
//        });

//        Graph<String,DataSet<Vertex<String,ArrayList<String>>>,Tuple3<NullValue,Double,Double>> te = tempgraphdoubles.getGellyGraph().mapVertices(new vertextestmapper<String>(tempgraphdoubles));

        DataSet<String> setforlist = TemporalGraph.getVertices().map(new MapFunction<Vertex<String, NullValue>, String>() {

            @Override
            public String map(Vertex<String, NullValue> value) throws Exception {
                return value.getId();
            }
        });
        List<String> vertexcollection = setforlist.collect();
        DataSet<ArrayList<String>> collectionDataSet;
//        List<ArrayList<String>> collection = null;
        ArrayList<String> temparlist = new ArrayList<>();


//        collectionDataSet = TemporalGraph.run(new SingleSourceShortestTemporalPathSTTJustPaths<>(vertexcollection.get(0), maxIterations)).map(new vertextestmapperdataset());

        int timer = 0;

        for (String vertexid : vertexcollection) {
            timer++;
            List<ArrayList<String>> temp = TemporalGraph.run(new SingleSourceShortestTemporalPathSTTJustPaths<>(vertexid, maxIterations)).map(new vertextestmapperdataset()).collect();
            for (ArrayList<String> t : temp) {
                temparlist.addAll(t);
            }
            System.out.println(timer);

        }
        System.out.println(temparlist.toString());

//        System.out.println(something.toString());


    }
    public static void test16() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 16000);
//        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_SEGMENT_SIZE_KEY, 64000);
//        conf.setFloat(ConfigConstants.Tas, 64000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        int maxIterations = 5;

        DataSet<Tuple4<String, String, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph2")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(String.class, String.class, Double.class, Double.class); // read the node IDs as Longs
        Tgraph<String, NullValue, NullValue, Double> TemporalGraph = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles, env);
        System.out.println(TemporalGraph.run(new SSSTPClosenessSingleNode<>("B",30,1,false)));
//        TemporalGraph.run(new SingleSourceShortestTemporalPathEAT<>("A",30)).print();
    }
    public static void test18() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        int maxIterations = 5;

        DataSet<Tuple4<String, String, Double, Double>> temporalsetdoubles = env.readCsvFile("./datasets/Testgraph2")
                .fieldDelimiter(",")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .types(String.class, String.class, Double.class, Double.class); // read the node IDs as Longs
        Tgraph<String, NullValue, NullValue, Double> tempgraphdoubles = Tgraph.From4TupleNoEdgesNoVertexes(temporalsetdoubles,env);



        tempgraphdoubles.run(new SSSTPCloseness<>(maxIterations,1,false)).print();

//        verticess.print();
    }

    /*
    * Test with wilco's dataset running different algorithms
    * */
    public static void test19() throws Exception {
        final ExecutionEnvironment env;

        // sets can be 10M, 1M, 100k, 10k
        String currentset = "C:\\Dropbox\\graphInstances\\" + arguments[0] + ".txt";
        int maxiterations = Integer.parseInt(arguments[1]);
        // method can be: closeness, testsssp, ssstp
        String method = arguments[2];
        // boolean for usage of undirected graph
        String undirected;
        if(arguments.length > 3) {
            undirected = arguments[3];
        } else {
            undirected = "false";
        }

        if(arguments.length > 4) {
            Configuration conf = new Configuration();
            conf.setString("fs.overwrite-files","true");
            env = ExecutionEnvironment.createLocalEnvironment(conf);
        } else {
            env = ExecutionEnvironment.getExecutionEnvironment();

        }


//        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
//        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY, 0.4F);
//        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_SIZE_KEY,100000);
//        conf.setString(ConfigConstants.TASK_MANAGER_MEMORY_OFF_HEAP_KEY, "2000");
//        System.out.println(arguments[0]);
//        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_SEGMENT_SIZE_KEY, 64000);
//        conf.setFloat(ConfigConstants.Tas, 64000);
//





        DataSet<Tuple2<Integer, Integer>> temporalsetdoubles = env.readCsvFile(currentset)
                .fieldDelimiter(" ")  // node IDs are separated by spaces
                .ignoreComments("%")  // comments start with "%"
                .includeFields("101")
                .types(Integer.class, Integer.class); // read the node IDs as Longs


        long time = System.nanoTime();

        System.out.println("done loading file");

        if(method.equals("testsssp")) {

            DataSet<Edge<Integer, Double>> newset = temporalsetdoubles.map(new MapFunction<Tuple2<Integer, Integer>, Edge<Integer, Double>>() {
                @Override
                public Edge<Integer, Double> map(Tuple2<Integer, Integer> value) throws Exception {
                    return new Edge<Integer, Double>(value.f0, value.f1, 1D);
                }
            });

            Graph<Integer, Double, Double> testgraph = Graph.fromDataSet(newset, new InitVerticesFromIntegerToDouble(), env);
            testgraph.getUndirected().run(new testSSSP<>(1, maxiterations)).first(10).print();
        } else if(method.equals("ssstp")) {

//            creating the dataset of edges with random temporal edges
            DataSet<Tuple4<Integer, Integer, Double, Double>> newset = temporalsetdoubles.map(
                    new MapFunction<Tuple2<Integer, Integer>, Tuple4<Integer, Integer, Double, Double>>() {
                        @Override
                        public Tuple4<Integer, Integer, Double, Double> map(Tuple2<Integer, Integer> value) throws Exception {
                            Double time =  ThreadLocalRandom.current().nextDouble(1000);
                            return new Tuple4<>(value.f0, value.f1, time, time + 5);
                        }
                    });
            Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(newset, env);
//            temporalGraphfullset.getTemporalEdges().print();
            DataSet<Vertex<Integer, Double>> resultset;

            if(undirected.equals("true")) {
                System.out.println("works");
                resultset = temporalGraphfullset.getUndirected().run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations));
//                testvar.first(10).print();
            } else {
                resultset = temporalGraphfullset.run(new SingleSourceShortestTemporalPathEAT<>(1, maxiterations));
            }
//            resultset.print();
            resultset.writeAsText("file:///C:\\Dropbox\\graphInstancesresult\\result_ssstp_" + arguments[0] + ".txt");
            System.out.println(env.execute().getNetRuntime());

        } else if(method.equals("closeness")) {
            //creating the dataset of edges with random temporal edges
            DataSet<Tuple4<Integer, Integer, Double, Double>> newset = temporalsetdoubles.map(
                    new MapFunction<Tuple2<Integer, Integer>, Tuple4<Integer, Integer, Double, Double>>() {
                        @Override
                        public Tuple4<Integer, Integer, Double, Double> map(Tuple2<Integer, Integer> value) throws Exception {
                            Double time =  ThreadLocalRandom.current().nextDouble(1000);
                            return new Tuple4<>(value.f0, value.f1, time, time + 5);
                        }
                    });
            Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(newset, env);
//            temporalGraphfullset.getTemporalEdges().print();
            temporalGraphfullset.run(new SSSTPCloseness<>(maxiterations,1,false)).sortPartition(1, Order.DESCENDING).first(200).print();
        }

//        System.out.println((System.nanoTime() - time) / 1000000000 + " seconds");
    }

    public static void test20() throws Exception {

        Configuration conf = new Configuration();
        conf.setFloat(ConfigConstants.TASK_MANAGER_NETWORK_NUM_BUFFERS_KEY, 2000);
        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_FRACTION_KEY,0.4F);
//        conf.setFloat(ConfigConstants.TASK_MANAGER_MEMORY_SEGMENT_SIZE_KEY, 64000);
//        conf.setFloat(ConfigConstants.Tas, 64000);
        final ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment(conf);
        int maxIterations = 1;

        DataSet<Tuple4<Integer, Integer, Double, Double>> temporalsetdoubles = env.readCsvFile("C:\\git\\Temporal_Graph_library\\datasets\\Testgraph2")
            .fieldDelimiter(" ")  // node IDs are separated by spaces
            .ignoreComments("%")  // comments start with "%"
            .types(Integer.class, Integer.class, Double.class, Double.class); // read the node IDs as Longs

        DataSet<Tuple4<Integer, Integer, Double, Double>> newset = temporalsetdoubles.map(
                new MapFunction<Tuple4<Integer, Integer, Double, Double>, Tuple4<Integer, Integer, Double, Double>>() {
            @Override
            public Tuple4<Integer, Integer, Double, Double> map(Tuple4<Integer, Integer, Double, Double> value) throws Exception {
                return new Tuple4<>(value.f0,value.f1,value.f3 - 1165708800,value.f3 + 1 - 1165708800);
            }
        });
        Tgraph<Integer, NullValue, NullValue, Double> temporalGraphfullset = Tgraph.From4TupleNoEdgesNoVertexes(newset.first(50000), env);
        Graph<Integer, Double, Double> tempgraph = temporalGraphfullset.getGellyGraph().mapVertices(new InitVerticeNullfordoubles()).mapEdges(new MapFunction<Edge<Integer, Tuple3<NullValue, Double, Double>>, Double>() {
            @Override
            public Double map(Edge<Integer, Tuple3<NullValue, Double, Double>> integerTuple3Edge) throws Exception {
                return 1D;
            }
        });
        tempgraph.run(new PageRank<>(0.85,1)).first(50).print();

//        Tgraph<Integer, NullValue, NullValue, Double> temporalGraph = Tgraph.From4TupleNoEdgesNoVertexes(newset.first(500000), env);
//        DataSet<Vertex<Integer, Double>> result = temporalGraph.run(new SingleSourceShortestTemporalPathEAT<>(2, maxIterations));
//        result = result.filter(new FilterFunction<Vertex<Integer, Double>>() {
//            @Override
//            public boolean filter(Vertex<Integer, Double> integerDoubleVertex) throws Exception {
//                if(integerDoubleVertex.getValue() == Double.MAX_VALUE) {
//                    return false;
//                } else {
//                    return true;
//                }
//            }
//        });
//        result.first(50).print();
//        System.out.println(result.collect().size());

//        temporalGraph.getTemporalEdges().first(50).print();
//        System.out.println(TemporalGraph.run(new SSSTPClosenessSingleNode<>("B",30,1,false)));
//        TemporalGraph.run(new SingleSourceShortestTemporalPathEAT<>("A",30)).print();
    }
    /*
* Test with testgraph2, single shortset path EAT without paths
* */

    public static final class vertextestmapperdataset<K>	implements MapFunction<Vertex<String, ArrayList<String>>, ArrayList<String>> {
        @Override
        public ArrayList<String> map(Vertex<String, ArrayList<String>> value) throws Exception {
            return value.getValue();
        }
    }

//    public static final class testmapper<K>	implements MapFunction<Vertex<String, NullValue>,>
    public static final class vertextestmapper<K>	implements MapFunction<Vertex<String, NullValue>, DataSet<Vertex<String, ArrayList<String>>>> {

        private Tgraph<String, NullValue, NullValue, Double> tempgraphdoubles;
        public vertextestmapper(Tgraph<String, NullValue, NullValue, Double> tempgraphdoubles) {
            this.tempgraphdoubles = tempgraphdoubles;
        }


        @Override
        public DataSet<Vertex<String, ArrayList<String>>> map(Vertex<String, NullValue> value) throws Exception {
            DataSet<Vertex<String, ArrayList<String>>> tempa = tempgraphdoubles.run(new SingleSourceShortestTemporalPathSTTJustPaths<>((String) value.getId(), 30));

            return tempa;
        }
    }
        /**
     * Distributes the minimum distance associated with a given vertex among all
     * the target vertices summed up with the edge's value.
     */
    @SuppressWarnings("serial")
    private static final class MinDistanceMessenger extends ScatterFunction<Long, Double, Double, Long> {

        @Override
        public void sendMessages(Vertex<Long, Double> vertex) {
            if (vertex.getValue() < Double.POSITIVE_INFINITY) {

                for (Edge<Long, Long> edge : getEdges()) {
//                    System.out.println(edge);
                    sendMessageTo(edge.getTarget(), vertex.getValue() + edge.getValue());
                }
            }
        }
    }
    @SuppressWarnings("serial")
    private static final class MinDistanceMessengerforTuple extends ScatterFunction<Long, Double, Double, Tuple3<Long,Long,Long>> {

        @Override
        public void sendMessages(Vertex<Long, Double> vertex) {
            if (vertex.getValue() < Double.POSITIVE_INFINITY) {
                for (Edge<Long, Tuple3<Long,Long,Long>> edge : getEdges()) {
                    if (edge.getValue().f1 >= vertex.getValue()) {
                        sendMessageTo(edge.getTarget(), edge.getValue().f2.doubleValue());
                    }
                }
            }
        }
    }
    @SuppressWarnings("serial")
    private static final class MinDistanceMessengerforTuplewithpath2 extends ScatterFunction<Long, Double, Tuple2<Double,Long[]>, Tuple3<Long,Long,Long>> {

        @Override
        public void sendMessages(Vertex<Long, Double> vertex) {
            if (vertex.getValue() < Double.POSITIVE_INFINITY) {
                for (Edge<Long, Tuple3<Long,Long,Long>> edge : getEdges()) {
                    if (edge.getValue().f1 >= vertex.getValue().doubleValue()) {
                        Long[] test = {4L};
                        sendMessageTo(edge.getTarget(), new Tuple2<>(edge.getValue().f2.doubleValue(), test) );
                    }
                }
            }
        }
    }
    @SuppressWarnings("serial")
    private static final class MinDistanceMessengerforTuplewithpath extends ScatterFunction<Long, Tuple2<Double,ArrayList<Long>>, Tuple2<Double,ArrayList<Long>>, Tuple3<Long,Long,Long>> {

        @Override
        public void sendMessages(Vertex<Long, Tuple2<Double,ArrayList<Long>>> vertex) {
            if ((Double) vertex.getValue().getField(0) < Double.POSITIVE_INFINITY) {
                for (Edge<Long, Tuple3<Long,Long,Long>> edge : getEdges()) {
                    if (edge.getValue().f1 >= vertex.getValue().f0) {
                        ArrayList<Long> temp = new ArrayList<>(vertex.getValue().f1);
                        temp.add(vertex.getId());
//                        System.out.println("temparray of " + vertex.getId() + ": " + temp.toString());
                        sendMessageTo(edge.getTarget(), new Tuple2<>(edge.getValue().f2.doubleValue(), temp) );
                    }
                }
            }
        }
    }
    /**
     * Function that updates the value of a vertex by picking the minimum
     * distance from all incoming messages.
     */

    @SuppressWarnings("serial")
    private static final class VertexDistanceUpdaterwithpath extends GatherFunction<Long, Tuple2<Double,ArrayList<Long>>, Tuple2<Double,ArrayList<Long>>> {

        @Override
        public void updateVertex(Vertex<Long, Tuple2<Double,ArrayList<Long>>> vertex, MessageIterator<Tuple2<Double,ArrayList<Long>>> inMessages) {

            Double minDistance = Double.MAX_VALUE;
            ArrayList<Long> minpath = vertex.getValue().f1;
            for (Tuple2<Double,ArrayList<Long>> msg : inMessages) {
                if (msg.f0 < minDistance) {
                    minDistance = msg.getField(0);
                    minpath = msg.f1;
                }
            }

            if (vertex.getValue().f0 > minDistance) {
                setNewVertexValue(new Tuple2<>(minDistance,minpath));
            }
        }
    }
    /**
     * Function that updates the value of a vertex by picking the minimum
     * distance from all incoming messages.
     */
    @SuppressWarnings("serial")
    private static final class VertexDistanceUpdater extends GatherFunction<Long, Double, Double> {

        @Override
        public void updateVertex(Vertex<Long, Double> vertex, MessageIterator<Double> inMessages) {

            Double minDistance = Double.MAX_VALUE;

            for (double msg : inMessages) {
                if (msg < minDistance) {
                    minDistance = msg;
                }
            }

            if (vertex.getValue() > minDistance) {
                setNewVertexValue(minDistance);
            }
        }
    }

    /**
     * Initializes the vertex values with the vertex ID
     */
    public static final class InitVertices implements MapFunction<Long, Double> {

        @Override
        public Double map(Long vertexId) {
//            if (vertexId == 1L) {
                return 0D;
//            } else {
//                return Double.MAX_VALUE;
//            }
        }
    }

    public static final class InitVerticesDoublesDoubles implements MapFunction<Double, Double> {

        @Override
        public Double map(Double aDouble) {
            return 0D;
        }
    }
    public static final class InitVerticesfordoubles implements MapFunction<String, Double> {

        @Override
        public Double map(String vertexId) {
            return 0D;
        }
    }
    public static final class InitVerticesFromNullToDouble implements MapFunction<NullValue, Double> {
        @Override
        public Double map(NullValue nullValue) throws Exception {
            return 0D;
        }
    }
    public static final class InitVerticesFromIntegerToDouble implements MapFunction<Integer, Double> {
        @Override
        public Double map(Integer t) throws Exception {
            return 0D;
        }
    }

    public static final class InitVerticeNullfordoubles implements MapFunction<Vertex<Integer,NullValue>, Double> {

        @Override
        public Double map(Vertex<Integer, NullValue> integerNullValueVertex) throws Exception {
            return 0D;
        }
    }
    public static final class InitVerticeNullfordoublesstring implements MapFunction<Vertex<String,Double>, Double> {

        @Override
        public Double map(Vertex<String, Double> integerNullValueVertex) throws Exception {
            return 0D;
        }
    }



}
