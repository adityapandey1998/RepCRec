package Sites;

import Transactions.Lock;

import java.util.*;

public class Site {
    public int siteId;
    public boolean up;
    public Map<String, List<Lock>> lockMap;
    public Map<String, TreeMap<Integer, Integer>> dataMap;
    public Site(int siteId) {
        this.siteId = siteId;
        this.dataMap = new HashMap<>();
        this.lockMap = new HashMap<>();
        this.startEndTimeMap = new TreeMap<>();
        this.up = true;
        this.visitedTransactions = new HashSet<>();
        this.variableStaleStateMap = new HashMap<>();
        this.datacache = new HashMap<>();
    }

    public void addDataMap(String key, int time, int val) {
        TreeMap<Integer, Integer> mapValue = dataMap.getOrDefault(key, new TreeMap<>());
        mapValue.put(time, val);
        dataMap.put(key, mapValue);
    }
}
