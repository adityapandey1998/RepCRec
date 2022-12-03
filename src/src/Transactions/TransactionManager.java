package Transactions;

import Sites.Site;
import OperationHandlers.dumpHandlers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TransactionManager {
    static public Integer siteCount = 10;
    static public Integer varCount = 20;
    private List<Site> siteList;
    private List<Transaction> transactionList;
    private Map<String, HashSet<Site>> varSiteMap;


    public TransactionManager() {
        this.siteList = new ArrayList<Site>();
        this.transactionList = new ArrayList<Transaction>();
        this.varSiteMap = new HashMap<String, HashSet<Site>>();

        this.createLists();
    }

    public void addTransaction(Transaction transaction) {
        transactionList.add(transaction);
    }


    public void createLists() {
        for (int siteNo = 0; siteNo<siteCount; siteNo++ ) {
            siteList.add(new Site(siteNo+1));
        }

        for (int varNo = 0; varNo < varCount; varNo++ ) {
            String varName = "x" + (varNo+1);
            if (varNo%2 == 1) {
                Site site = sites.get((varNo+1) % 10);
                site.addDataMap(varName, 0, 10 * (varNo+1));

                HashSet<Site> set = varSiteMap.getOrDefault(varName, new HashSet<Site>());
                set.add(site);
                varSiteMap.put(varName, set);

                site.addStartEndTimeMap(0, Integer.MAX_VALUE);
                site.getVariableStaleStateMap().put(varName, false);
            } else {
                for (int siteNo = 0; siteNo<siteCount; siteNo++ ) {
                    Site site = sites.get((siteNo));
                    site.addDataMap(varName, 0, 10 * (varNo+1));

                    HashSet<Site> set = varSiteMap.getOrDefault(varName, new HashSet<Site>());
                    set.add(site);
                    varSiteMap.put(varName, set);

                    site.addStartEndTimeMap(0, Integer.MAX_VALUE);
                    site.getVariableStaleStateMap().put(varName, false);
                }
            }
        }
    }

    public void processInputFile(String filePath) throws IOException {
        int time=1;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException fileNotFoundException) {
            System.err.println("FileNotFoundException Raised");
            return;
        }
        String line = "";
        while((line = reader.readLine()) !=null) {
//            Check Deadlock
            if (line.startsWith("dump()")) {
                dumpHandlers.dumpAction(siteList);
            } else if (line.startsWith("beginRO")) {
                String transactionId = line.substring(8, line.length() - 1);
                Transaction transaction = new Transaction(transactionId, Constants.TransactionType.RO, time);
                transactionList.add(transaction);
            } else if (line.startsWith("begin")) {
                String transactionId = line.substring(6, line.length() - 1);
                Transaction transaction = new Transaction(transactionId, Constants.TransactionType.RW, time);
                transactionList.add(transaction);
            } else if (line.startsWith("R")) {
                String inputLine = line.substring(2, line.length() - 1);
                String[] inputLineSplit = inputLine.split(",");
                String transactionId = inputLineSplit[0].trim();
                String variableName = inputLineSplit[1].trim();

                Transaction transactionToRead = null;
                for (Transaction transaction : transactionList) {
                    if (transaction.transactionId.equals(transactionId)) {
                        transactionToRead = transaction;
                        break;
                    }
                }

            } else if (line.startsWith("W")) {
                String inputLine = line.substring(2, line.length() - 1);
                String[] inputLineSplit = inputLine.split(",");
                String transactionId = inputLineSplit[0].trim();
                String variableName = inputLineSplit[1].trim();
                int value = Integer.parseInt(inputLineSplit[2].trim());

                Transaction transactionToWrite = null;
                for (Transaction transaction : transactionList) {
                    if (transaction.transactionId.equals(transactionId)) {
                        transactionToWrite = transaction;
                        break;
                    }
                }
            } else if (line.startsWith("fail")) {
                int siteId = Integer.parseInt(line.substring(5, line.length() - 1).trim());
                Site siteToFail = null;
                for (Site site : siteList) {
                    if (site.siteId == siteId) {
                        siteToFail = site;
                        OperationHandlers.siteHandlers.failSite(siteToFail, time);
                        break;
                    }
                }
                if (siteToFail==null) {
                    System.out.println("Site doesn't exist");
                }

            } else if (line.startsWith("recover")) {
                int siteId = Integer.parseInt(line.substring(8, line.length() - 1).trim());
                Site siteToRecover = null;
                for (Site site : siteList) {
                    if (site.siteId == siteId) {
                        siteToRecover = site;
                        OperationHandlers.siteHandlers.recoverSite(siteToRecover, time);
                        break;
                    }
                }
                if (siteToRecover==null) {
                    System.out.println("Site doesn't exist");
                }
            } else if (line.startsWith("end")) {
                //endTransaction(op.substring(4, op.length() - 1));
                String transactionId = line.substring(4, line.length() - 1).trim();
                Transaction transactionToEnd = null;
                for (Transaction transaction : transactionList) {
                    if (transaction.transactionId.equals(transactionId)) {
                        transactionToEnd = transaction;
                        OperationHandlers.endHandler.endTransaction(transactionToEnd, siteList);
                        break;
                    }
                }
                if (transactionToEnd == null) {
                    System.out.println("Nothing to end here!");
                }
//                endTransactionList.add(line.substring(4, line.length() - 1));
            }


        }

    }
}
