package Tgraphs;

/**
 * Created by s133781 on 03-Nov-16.
 */

import org.apache.flink.graph.Graph;

/**
 * @param <K> key type
 * @param <VV> vertex value type
 * @param <EV> edge value type
 * @param <T> the return type
 */
public interface TGraphAlgorithm<K, VV, EV, N, T> {

    public T run(Tgraph<K, VV, EV, N> input) throws Exception;
}
