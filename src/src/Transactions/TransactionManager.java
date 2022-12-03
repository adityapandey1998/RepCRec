package Transactions;

import Data.DataManager;
import Data.Result;
import Transactions.Constants.OperationType;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates Read/Write requests to the DB into requests for the Data Managers at all sites.
 */
public class TransactionManager {

  private final List<DataManager> dataManagerList;
  private final Map<String, Transaction> transactionTable;
  private final Deque<Operation> operationQueue;
  private int time = 0;

  /**
   * Initialize Class Members and create Data Managers
   */
  public TransactionManager() {
    transactionTable = new HashMap<>();
    operationQueue = new ArrayDeque<>();

    dataManagerList = new ArrayList<>();
    int siteCount = 10;
    for (int siteNo = 0; siteNo < siteCount; siteNo++) {
      dataManagerList.add(new DataManager(siteNo + 1));
    }
  }

  /**
   * Identify and handle deadlocks in the Blocking Graph.
   *
   * @return Boolean value representing the detection of a deadlock.
   */
  private boolean handleDeadlock() {
    Map<String, Set<String>> blockingGraph = new HashMap<>();
    Map<String, Set<String>> graph;
    for (DataManager dataManager : dataManagerList) {
      if (dataManager.isUp()) {
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

    for (String node : blockingGraph.keySet()) {
      Set<String> visited = new HashSet<>();
      if (hasCycle(node, node, visited, blockingGraph)) {
        if (transactionTable.get(node).getStartTime() > youngestTransTime) {
          youngestTransTime = transactionTable.get(node).getStartTime();
          youngestTransId = node;
        }
      }
    }
    if (youngestTransId != null) {
      System.out.println("Deadlock detected: aborting transaction " + youngestTransId);
      this.abort(youngestTransId, false);
      return true;
    }
    return false;
  }

  /**
   * Simple DFS-Based Cycle detection algorithm
   *
   * @return true if a cycle is found with the given root.
   */
  private boolean hasCycle(String current, String root, Set<String> visited,
      Map<String, Set<String>> blockingGraph) {
    visited.add(current);
    for (String neighbour : blockingGraph.get(current)) {
      if (neighbour.equals(root)) {
        return true;
      }
      if (!visited.contains(neighbour)) {
        if (hasCycle(neighbour, root, visited, blockingGraph)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Function to abort a given transaction.
   *
   * @param transactionId    ID of the transaction to abort.
   * @param dueToSiteFailure If the abort is due to a site failed.
   */
  public void abort(String transactionId, boolean dueToSiteFailure) {
    for (DataManager dataManager : dataManagerList) {
      dataManager.abort(transactionId);
      transactionTable.remove(transactionId);
      if (dueToSiteFailure) {
        System.out.println("Aborted " + transactionId + " due to site failure!");
      } else {
        System.out.println("Aborted " + transactionId + " due to deadlock!");
      }
    }
  }

  /**
   * Entry Point to the Transaction Manager running on given Input File
   *
   * @param filePath Path to input file
   * @throws IOException Error while reading the file
   */
  public void processInput(String filePath) throws IOException {
    BufferedReader reader;
    try {
      reader = new BufferedReader(new FileReader(filePath));
    } catch (FileNotFoundException fileNotFoundException) {
      System.err.println("FileNotFoundException Raised");
      return;
    }
    String line;
    while ((line = reader.readLine()) != null) {
      if (handleDeadlock()) {
        executeOp();
      }
      executeInput(line);
      executeOp();
      time += 1;
    }
  }

  /**
   * Execute Read/Write Operation
   */
  private void executeOp() {
    Deque<Operation> operationQueueCopy = new ArrayDeque<>(operationQueue);
    for (Operation operation : operationQueueCopy) {
      if (!transactionTable.containsKey(operation.getTransactionId())) {
        operationQueue.remove(operation);
      } else {
        boolean opSuccess = false;
        if (operation.getCommand() == OperationType.READ) {
          if (transactionTable.get(operation.getTransactionId()).getTransactionType()
              == Constants.TransactionType.RO) {
            opSuccess = readOnlyOp(operation.getTransactionId(), operation.getVariableId());
          } else {
            opSuccess = readOp(operation.getTransactionId(), operation.getVariableId());
          }
        } else if (operation.getCommand() == OperationType.WRITE) {
          opSuccess = writeOp(operation.getTransactionId(), operation.getVariableId(),
              operation.getValue());
        } else {
          System.out.println("Invalid Operation");
        }
        if (opSuccess) {
          operationQueue.remove(operation);
        }
      }
    }
  }

  /**
   * @param transactionId Transaction corresponding to Read Operation
   * @param variableId    Variable corresponding to Read Operation
   * @return Success of General Read Operation
   */
  private boolean readOp(String transactionId, String variableId) {
    if (!transactionTable.containsKey(transactionId)) {
      System.out.println(
          "Transaction " + transactionId + " doesn't exist in the transaction table!");
    } else {
      for (DataManager dataManager : dataManagerList) {
        if (dataManager.isUp() && dataManager.hasVariable(variableId)) {
          Result result = dataManager.read(transactionId, variableId);
          if (result.isSuccess()) {
            Transaction transaction = transactionTable.get(transactionId);
            transaction.sitesAccessed.add(dataManager.getSiteId());
            transactionTable.put(transactionId, transaction);
            System.out.println(String.format("%s reads %s.%s: %d".format(
                transactionId, variableId, dataManager.getSiteId(), result.getValue())));
            return true;
          }

        }
      }
    }
    return false;
  }

  /**
   * @param transactionId Transaction corresponding to Read-Only Read Operation
   * @param variableId    Variable corresponding to Read-Only Read Operation
   * @return Success of Read-Only Read Operation
   */
  private boolean readOnlyOp(String transactionId, String variableId) {
    if (!transactionTable.containsKey(transactionId)) {
      System.out.println(
          "Transaction " + transactionId + " doesn't exist in the transaction table!");
    } else {
      int ts = transactionTable.get(transactionId).getStartTime();
      for (DataManager dataManager : dataManagerList) {
        if (dataManager.isUp() && dataManager.hasVariable(variableId)) {
          Result result = dataManager.readSnapshot(variableId, ts);
          if (result.isSuccess()) {
            System.out.println(String.format("%s (RO) reads %s.%s: %d".format(
                transactionId, variableId, dataManager.getSiteId(), result.getValue())));
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * @param transactionId Transaction corresponding to Write Operation
   * @param variableId    Variable corresponding to Write Operation
   * @param value         Value to be written
   * @return Success of Write Operation
   */
  private boolean writeOp(String transactionId, String variableId, int value) {
    if (!transactionTable.containsKey(transactionId)) {
      System.out.println(
          "Transaction " + transactionId + " doesn't exist in the transaction table!");
    } else {
      boolean allSitesDown = true, canGetAllLocks = true;
      for (DataManager dataManager : dataManagerList) {
        if (dataManager.isUp() && dataManager.hasVariable(variableId)) {
          allSitesDown = false;
          boolean result = dataManager.getWriteLock(transactionId, variableId);
          if (!result) {
            canGetAllLocks = false;
          }
        }
      }
      if (!(canGetAllLocks && allSitesDown)) {
        List<Integer> sitesWritten = new ArrayList<>();
        for (DataManager dataManager : dataManagerList) {
          if (dataManager.isUp() && dataManager.hasVariable(variableId)) {
            dataManager.write(transactionId, variableId, value);

            Transaction transaction = transactionTable.get(transactionId);
            transaction.sitesAccessed.add(dataManager.getSiteId());
            transactionTable.put(transactionId, transaction);

            sitesWritten.add(dataManager.getSiteId());

          }
        }
        System.out.println(String.format("%s writes %s with value %s to sites %s",
            transactionId, variableId, value, sitesWritten));
        return true;
      }
    }
    return false;
  }

  /**
   * Function to trigger the output for each DataManager/Site.
   */
  private void dump() {
    for (DataManager dataManager : dataManagerList) {
      dataManager.dump();
    }
  }

  /**
   * Identify the Operation, get the operands and pass the parameters to the corresponding
   * functions.
   *
   * @param line Input Line containing the commands
   */
  private void executeInput(String line) {
    if (line.startsWith("dump")) {
      System.out.println("Dump:");
      dump();
    } else if (line.startsWith("fail")) {
      int siteId = Integer.parseInt(line.substring(5, line.length() - 1).trim());
      fail(siteId);
    } else if (line.startsWith("recover")) {
      int siteId = Integer.parseInt(line.substring(8, line.length() - 1).trim());
      recover(siteId);
    } else if (line.startsWith("end")) {
      String transactionId = line.substring(4, line.length() - 1).trim();
      end(transactionId);
    } else if (line.startsWith("beginRO")) {
      String transactionId = line.substring(8, line.length() - 1);
      beginTransaction(transactionId, Constants.TransactionType.RO, time);
    } else if (line.startsWith("begin")) {
      String transactionId = line.substring(6, line.length() - 1);
      beginTransaction(transactionId, Constants.TransactionType.RW, time);
    } else if (line.startsWith("R")) {
      String inputLine = line.substring(2, line.length() - 1);
      String[] inputLineSplit = inputLine.split(",");
      String transactionId = inputLineSplit[0].trim();
      String variableName = inputLineSplit[1].trim();
      if (!transactionTable.containsKey(transactionId)) {
        System.out.println("Transaction " + transactionId + " doesn't exists!");
      } else {
        operationQueue.add(new Operation(OperationType.READ, transactionId, variableName));
      }
    } else if (line.startsWith("W")) {
      String inputLine = line.substring(2, line.length() - 1);
      String[] inputLineSplit = inputLine.split(",");
      String transactionId = inputLineSplit[0].trim();
      String variableName = inputLineSplit[1].trim();
      int value = Integer.parseInt(inputLineSplit[2].trim());

      if (!transactionTable.containsKey(transactionId)) {
        System.out.println("Transaction " + transactionId + " doesn't exists!");
      } else {
        operationQueue.add(new Operation(OperationType.WRITE, transactionId, variableName, value));
      }
    } else {
      System.out.println("Invalid Operation");
    }

  }

  /**
   * @param transactionId   ID of the new Transaction
   * @param transactionType Type of the new Transaction
   * @param time            Start time of the new Transaction
   */
  private void beginTransaction(String transactionId, Constants.TransactionType transactionType,
      int time) {
    if (transactionTable.containsKey(transactionId)) {
      System.out.println("Transaction " + transactionId + " already exists!");
    } else {
      Transaction transaction = new Transaction(transactionId, transactionType, time);
      if (transactionType == Constants.TransactionType.RO) {
        System.out.println("Transaction " + transactionId + " begins and is read-only.");
      } else {
        System.out.println("Transaction " + transactionId + " begins.");
      }
      transactionTable.put(transactionId, transaction);
    }
  }

  /**
   * Function to trigger failure of given site.
   *
   * @param siteId ID of the site to fail.
   */
  private void fail(int siteId) {
    DataManager dataManager = dataManagerList.get(siteId - 1);
    if (dataManager.isUp()) {
      System.out.println("Failing Site:" + siteId);
      dataManager.fail(time);
      for (Transaction transaction : transactionTable.values()) {
        if (
            !(transaction.getTransactionType() == Constants.TransactionType.RO) &&
                (transaction.isLive) &&
                transaction.getSitesAccessed().contains(siteId)
        ) {
          transaction.isLive = false;
        }
      }
    } else {
      System.out.println("Site " + siteId + " is down!!");
    }
  }

  /**
   * Function to trigger recovery of given site.
   *
   * @param siteId ID of the site to recover.
   */
  private void recover(int siteId) {
    DataManager dataManager = dataManagerList.get(siteId - 1);
    if (!dataManager.isUp()) {
      System.out.println("Failing Site:" + siteId);
      dataManager.recover(time);
    } else {
      System.out.println("Site " + siteId + " is already up!");
    }
  }

  /**
   * Function to trigger end of given transaction.
   *
   * @param transactionId ID of the transaction to end.
   */
  private void end(String transactionId) {
    if (!transactionTable.containsKey(transactionId)) {
      System.out.println("Transaction " + transactionId + " doesn't exist!");
    } else {
      if (!transactionTable.get(transactionId).isLive) {
        abort(transactionId, true);
      } else {
        commit(transactionId, time);
      }
    }
  }

  /**
   * Function to commit given transaction.
   *
   * @param transactionId ID of the transaction to commit.
   * @param time          commit time.
   */
  private void commit(String transactionId, int time) {
    for (DataManager dataManager : dataManagerList) {
      dataManager.commit(transactionId, time);
    }
    transactionTable.remove(transactionId);
    System.out.println("Transaction " + transactionId + " committed!");

  }

}
