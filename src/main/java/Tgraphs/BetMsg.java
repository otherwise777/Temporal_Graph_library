package Tgraphs;

import org.apache.flink.api.java.tuple.Tuple3;

import java.util.ArrayList;

/**
 * Created by s133781 on 21-Nov-16.
 */
public class BetMsg<K> {


    private K value;
    private Double dist;
    private ArrayList<K> path;


    public BetMsg(K value,Double distance, ArrayList<K> path) {
        this.value = value;
        this.dist = distance;
        this.path = path;
    }
    public K getValue() {
        return value;
    }

    public Double getDist() {
        return dist;
    }

    public ArrayList<K> getPath() {
        return path;
    }
}
