package OperationHandlers;
import Sites.Site;
import Transactions.Transaction;
import java.util.HashMap;
import java.util.TreeMap;

public class siteHandlers {
    public static void failSite(Site site, int endTime) {
        if (!site.up) {
            System.out.println("This site is down right now! Can't fail it!");
            return;
        }
        site.up = false;
        site.lockMap = new HashMap<>();
        site.endTime = endTime;
        for (Transaction t : site.getVisitedTransactions()) {
            t.setLive(false);
        }
    }

    public static void recoverSite(Site site, int endTime) {
        if (site.up) {
            System.out.println("This site is up right now! Can't recover it!");
            return;
        }
        site.up = true;
        site.lockMap = new HashMap<>();
        TreeMap<Integer, Integer> treeMap = site.getStartEndTimeMap();
        treeMap.put(endTime, Integer.MAX_VALUE);
        site.setStartEndTimeMap(treeMap);
        HashMap<String, Boolean> staleStateMap = site.getVariableStaleStateMap();

        for (String entry : staleStateMap.keySet()) {
            if (variableSiteMap.get(entry).size() > 1)
                staleStateMap.put(entry, true);
        }
    }
}
