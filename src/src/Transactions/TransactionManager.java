package Transactions;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TransactionManager {
    private List<DataManager> dataManagerList;
    private static Integer siteCount = 10;
    private Map<String, Transaction> transactionTable;
    private int time = 0;

    private Deque<Operation> operationQueue;
    public TransactionManager() {
        transactionTable = new HashMap<>();
        operationQueue = new ArrayDeque<>();

        dataManagerList = new ArrayList<DataManager>();
        for (int siteNo = 0; siteNo<siteCount; siteNo++ ) {
            dataManagerList.add(new DataManager(siteNo+1));
        }
    }

    private void handleDeadlock() {
        Map<String, Set<String>> blockingGraph = new HashMap<>();
        Map<String, Set<String>> graph;
        for (DataManager dataManager: dataManagerList) {
            if(dataManager.isUp()) {
                graph = dataManager.generateBlockingGraph();
                graph.forEach((node, adjList) -> {
//                    for node, adj_list in graph.items():
                    Set<String> tempSet = blockingGraph.getOrDefault(node, new HashSet<>());
                    tempSet.addAll(adjList);
                    blockingGraph.put(node, tempSet);
                });
            }
        }

        String youngestTransId = null;
        int youngestTransTime = null;
    }
    public void processInput(String filePath) throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException fileNotFoundException) {
            System.err.println("FileNotFoundException Raised");
            return;
        }
        String line = "";
        while ((line = reader.readLine()) != null) {

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
