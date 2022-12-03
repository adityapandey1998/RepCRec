package OperationHandlers;

import Sites.Site;

import java.util.*;

public class dumpHandlers {
    public static void dumpAction(List<Site> siteList) {
        for (Site site : siteList) {
            System.out.print("site " + site.siteId + " - ");
            Map<String, TreeMap<Integer, Integer>> dataMap = site.dataMap;
            ArrayList<String> sortedKeys = new ArrayList<>(dataMap.keySet());
            sortedKeys.sort(new Comparator<String>() {
                public int compare(String s1, String s2) {
                    return parseInteger(s1) - parseInteger(s2);
                }
                int parseInteger(String s) {
                    String num = s.replaceAll("\\D", "");
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
            for (String key : sortedKeys) {
                TreeMap<Integer, Integer> keyDataMap = dataMap.get(key);
                int latestTime = keyDataMap.lastKey();
                System.out.print(key + ": " + keyDataMap.get(latestTime) + " ");
            }
            System.out.println();
        }
    }
}
