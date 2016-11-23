package Tgraphs;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

import java.util.*;

/**
 * Created by s133781 on 21-Nov-16.
 */
public class BetVertexValue<K> {
    private Map<K,Tuple3<Double,ArrayList<K>,Boolean>> TheMap;

    public BetVertexValue() {
        Map<K,Tuple3<Double,ArrayList<K>,Boolean>> temp = new HashMap();
        this.TheMap = temp;
    }

    public BetVertexValue(K value) {
        Map<K,Tuple3<Double,ArrayList<K>,Boolean>> temp = new HashMap();
        temp.put(value, new Tuple3<>(0D, new ArrayList<K>(),true));
        this.TheMap = temp;
    }
    public Double getdist(K value) {
        return TheMap.get(value).f0;
    }
    public ArrayList<K> getpath(K value) {
        return TheMap.get(value).f1;
    }
    public Boolean getUpdate(K value) {
        return TheMap.get(value).f2;
    }
    public void setDistandPath(K value, Double dist,ArrayList<K> path) {
        TheMap.put(value, new Tuple3<>(dist,path,true));
    }
    public Iterator<Map.Entry<K, Tuple3<Double, ArrayList<K>,Boolean>>> getIterator() {
        return TheMap.entrySet().iterator();
    }
    public Set<Map.Entry<K, Tuple3<Double, ArrayList<K>,Boolean>>> getentrySet() {
        return TheMap.entrySet();
    }
    public boolean valueExists(K value) {
        return TheMap.containsKey(value);
    }
    public void addEntry(K value, Double dist,ArrayList<K> path) {
        this.TheMap.put(value, new Tuple3<>(dist, path, true));
    }
    public boolean addIfNotExist(K value, Double dist,ArrayList<K> path) {
        if(!TheMap.containsKey(value)) {
            this.TheMap.put(value, new Tuple3<>(dist, path, true));
            return true;
        }
        return false;
    }
    public void clearUpdates() {
        for (Tuple3<Double,ArrayList<K>,Boolean> value : TheMap.values()) {
            value.f2 = false;
        }
    }

}
