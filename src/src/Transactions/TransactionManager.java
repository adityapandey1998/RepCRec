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

    private boolean handleDeadlock() {
        Map<String, Set<String>> blockingGraph = new HashMap<>();
        Map<String, Set<String>> graph;
        for (DataManager dataManager: dataManagerList) {
            if(dataManager.isUp()) {
                graph = dataManager.generateBlockingGraph();
                graph.forEach((node, adjList) -> {
                    Set<String> tempSet = blockingGraph.getOrDefault(node, new HashSet<>());
                    tempSet.addAll(adjList);
                    blockingGraph.put(node, tempSet);
                });
            }
        }

        String youngestTransId = null;
        int youngestTransTime = -1;

        for (String node: blockingGraph.keySet()) {
            Set<String> visited =  new HashSet<>();
            if (hasCycle(node, node, visited, blockingGraph)) {
                if (transactionTable.get(node).getStartTime() > youngestTransTime) {
                    youngestTransTime = transactionTable.get(node).getStartTime();
                    youngestTransId = node;
                }
            }
        }
        if (youngestTransId!=null) {
            System.out.println("Deadlock detected: aborting transaction " + youngestTransId);
            this.abort(youngestTransId, false);
            return true;
        }
        return false;
    }

    private boolean hasCycle(String current, String root, Set<String> visited, Map<String, Set<String>> blockingGraph) {
        visited.add(current);
        for (String neighbour : blockingGraph.get(current)) {
            if (neighbour.equals(root)) {
                return true;
            }
            if (!visited.contains(neighbour)) {
                if (hasCycle(neighbour, root, visited, blockingGraph))
                    return true;
            }
        }
        return false;
    }
    public void abort(String transactionId, boolean dueToSite) {
        for (DataManager dataManager: dataManagerList) {
            dataManager.abort(transactionId);
            transactionTable.remove(transactionId);
            if(dueToSite) {
                System.out.println("Aborted "+ transactionId + " due to site failure!");
            } else {
                System.out.println("Aborted "+ transactionId + " due to deadlock!");
            }
        }
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
            if(handleDeadlock()) {

            }
        }
    }

    private void executeOp() {
        Deque<Operation> operationQueueCopy = new ArrayDeque<>(operationQueue);
        for (Operation operation: operationQueueCopy) {
            if (!transactionTable.containsKey(operation.getTransactionId())) {
                operationQueue.remove(operation);
            } else {
                boolean opSuccess = false;
                if (operation.getCommand().equals("R")) {
                    if (transactionTable.get(operation.getTransactionId()).getTransactionType() == Constants.TransactionType.RO) {
                        opSuccess = readOnlyOp(operation.getTransactionId(), operation.getVariableId());
                    } else {
                        opSuccess = readOp(operation.getTransactionId(), operation.getVariableId());
                    }
                } else if (operation.getCommand().equals("W")) {
                    opSuccess = writeOp(operation.getTransactionId(), operation.getVariableId(), operation.getValue());
                } else {
                    System.out.println("Invalid Operation");
                }
                if (opSuccess)
                    operationQueue.remove(operation);
            }
        }
    }

    private boolean readOp(String transactionId, String variableId) {
        if (!transactionTable.containsKey(transactionId)) {
            System.out.println("Transaction "+ transactionId+ " doesn't exist in the transaction table!");
        } else {
            for (DataManager dataManager: dataManagerList) {
                if (dataManager.isUp() && dataManager.hasVariable(variableId)) {
                    boolean result = dataManager.read(transactionId, variableId);
                    if (result) {
                        Transaction transaction = transactionTable.get(transactionId);
                        transaction.sitesAccessed.add(dataManager.getSiteId());
                        transactionTable.put(transactionId, transaction);
                        System.out.println(String.format("%s reads %s.%s: %d".format(
                                transactionId, variableId, dataManager.getSiteId(), result.value)));
                        return true;
                    }

                }
            }
        }
        return false;
    }

    private boolean readOnlyOp(String transactionId, String variableId) {
        if (!transactionTable.containsKey(transactionId)) {
            System.out.println("Transaction "+ transactionId+ " doesn't exist in the transaction table!");
        } else {
            for (DataManager dataManager: dataManagerList) {
                if (dataManager.isUp() && dataManager.hasVariable(variableId)) {
                    boolean result = dataManager.readSnapshot(transactionId, variableId);
                    if (result) {
                        System.out.println(String.format("%s (RO) reads %s.%s: %d".format(
                                transactionId, variableId, dataManager.getSiteId(), result.value)));
                        return true;
                    }

                }
            }
        }
        return false;
    }

    private boolean writeOp(String transactionId, String variableId, int value) {
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
