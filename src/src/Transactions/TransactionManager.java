package Transactions;

import static Transactions.Constants.NUM_SITES;

import Data.DataManager;
import Data.Result;
import Transactions.Constants.OperationType;
import Transactions.Constants.TransactionType;
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
 *
 * @author Aditya Pandey
 */
public class TransactionManager {

  private final List<DataManager> sites;
  private final Map<String, Transaction> transactionMap;
  private final Deque<Operation> operationQueue;
  private int currentTime = 0;

  /**
   * Initialize Class Members and create Data Managers
   */
  public TransactionManager() {
    transactionMap = new HashMap<>();
    operationQueue = new ArrayDeque<>();
    sites = new ArrayList<>();
    for (int siteNo = 0; siteNo < NUM_SITES; siteNo++) {
      sites.add(new DataManager(siteNo + 1));
    }
  }

  /**
   * Identify and handle deadlocks in the Waitsfor/Blocking Graph.
   *
   * @return Boolean value representing the detection of a deadlock.
   */
  private boolean checkAndHandleDeadlock() {
    Map<String, Set<String>> blockingGraph = new HashMap<>();
    Map<String, HashSet<String>> graph;
    for (DataManager dataManager : sites) {
      if (dataManager.isUp()) {
        graph = dataManager.generateWaitsForGraph();
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
        if (transactionMap.get(node).getStartTime() > youngestTransTime) {
          youngestTransTime = transactionMap.get(node).getStartTime();
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
    for (String adj : blockingGraph.getOrDefault(current, new HashSet<>())) {
      if (adj.equals(root)) {
        return true;
      }
      if (!visited.contains(adj)) {
        if (hasCycle(adj, root, visited, blockingGraph)) {
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
    for (DataManager dataManager : sites) {
      dataManager.abortTransaction(transactionId);
      transactionMap.remove(transactionId);
    }
    if (dueToSiteFailure) {
      System.out.println("Aborted " + transactionId + " due to site failure!");
    } else {
      System.out.println("Aborted " + transactionId + " due to deadlock!");
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
      // Skip Comments
      if (line.startsWith("//")) {
        continue;
      }
      if (checkAndHandleDeadlock()) {
        executeOperations();
      }
      processInputLine(line);
      executeOperations();
      currentTime += 1;
    }
  }

  /**
   * Execute Read/Write Operation
   */
  private void executeOperations() {
    Deque<Operation> operationQueueCopy = new ArrayDeque<>(operationQueue);
    for (Operation operation : operationQueueCopy) {
      if (!transactionMap.containsKey(operation.getTransactionId())) {
        operationQueue.remove(operation);
      } else {
        boolean opSuccess = false;
        switch (operation.getOperationType()) {
          case READ -> {
            if (transactionMap.get(operation.getTransactionId()).getTransactionType()
                == TransactionType.RO) {
              opSuccess = readOnlyOp(operation.getTransactionId(), operation.getVariableId());
            } else {
              opSuccess = readOp(operation.getTransactionId(), operation.getVariableId());
            }
          }
          case WRITE -> opSuccess = writeOp(operation.getTransactionId(), operation.getVariableId(),
              operation.getValue());
          default -> System.out.println("Invalid Operation");
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
    if (transactionMap.containsKey(transactionId)) {
      for (DataManager dataManager : sites) {
        if (dataManager.isUp() && dataManager.variableExists(variableId)) {
          Result result = dataManager.readVariable(transactionId, variableId);
          if (result.isSuccess()) {

            Transaction transaction = transactionMap.get(transactionId);
            transaction.sitesAccessed.add(dataManager.getSiteId());
            transactionMap.put(transactionId, transaction);

            System.out.printf("%s reads %s.%s: %d%n",
                transactionId, variableId, dataManager.getSiteId(), result.getValue());
            return true;
          }
        }
      }
    } else {
      System.out.println(
          "Transaction " + transactionId + " doesn't exist in the transaction table!");
    }
    return false;
  }

  /**
   * @param transactionId Transaction corresponding to Read-Only Read Operation
   * @param variableId    Variable corresponding to Read-Only Read Operation
   * @return Success of Read-Only Read Operation
   */
  private boolean readOnlyOp(String transactionId, String variableId) {
    if (transactionMap.containsKey(transactionId)) {
      int ts = transactionMap.get(transactionId).getStartTime();
      for (DataManager dataManager : sites) {
        if (dataManager.isUp() && dataManager.variableExists(variableId)) {
          Result result = dataManager.readVariableSnapshot(variableId, ts);
          if (result.isSuccess()) {
            System.out.printf("%s (RO) reads %s.%s: %d%n",
                transactionId, variableId, dataManager.getSiteId(), result.getValue());
            return true;
          }
        }
      }
    } else {
      System.out.println(
          "Transaction " + transactionId + " doesn't exist in the transaction table!");
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
    if (transactionMap.containsKey(transactionId)) {
      boolean allSitesDown = true;
      boolean canGetAllLocks = true;
      for (DataManager dataManager : sites) {
        if (dataManager.isUp() && dataManager.variableExists(variableId)) {
          allSitesDown = false;
          boolean result = dataManager.acquireWriteLock(transactionId, variableId);
          if (!result) {
            canGetAllLocks = false;
          }
        }
      }
      if (canGetAllLocks && !allSitesDown) {
        List<Integer> writeSiteList = new ArrayList<>();
        for (DataManager dataManager : sites) {
          if (dataManager.isUp() && dataManager.variableExists(variableId)) {
            dataManager.writeVariable(transactionId, variableId, value);

            Transaction transaction = transactionMap.get(transactionId);
            transaction.sitesAccessed.add(dataManager.getSiteId());
            transactionMap.put(transactionId, transaction);

            writeSiteList.add(dataManager.getSiteId());
          }
        }
        System.out.printf("%s writes %s with value %s to sites %s%n",
            transactionId, variableId, value, writeSiteList);
        return true;
      }
    } else {
      System.out.println(
          "Transaction " + transactionId + " doesn't exist in the transaction table!");
    }
    return false;
  }

  /**
   * Function to trigger the output for each DataManager/Site.
   */
  private void dump() {
    for (DataManager dataManager : sites) {
      dataManager.dump();
    }
  }

  /**
   * Identify the Operation, get the operands and pass the parameters to the corresponding
   * functions.
   *
   * @param line Input Line containing the commands
   */
  private void processInputLine(String line) {
    int length = line.length();
    if (line.startsWith("dump")) {
      System.out.println("Dump:");
      dump();
    } else if (line.startsWith("fail")) {
      int siteId = Integer.parseInt(line.substring(5, length - 1).trim());
      fail(siteId);
    } else if (line.startsWith("recover")) {
      int siteId = Integer.parseInt(line.substring(8, length - 1).trim());
      recover(siteId);
    } else if (line.startsWith("end")) {
      String transactionId = line.substring(4, length - 1).trim();
      end(transactionId);
    } else if (line.startsWith("beginRO")) {
      String transactionId = line.substring(8, length - 1);
      beginTransaction(transactionId, TransactionType.RO, currentTime);
    } else if (line.startsWith("begin")) {
      String transactionId = line.substring(6, length - 1);
      beginTransaction(transactionId, TransactionType.RW, currentTime);
    } else if (line.startsWith("R")) {
      String inputLine = line.substring(2, length - 1);
      String[] inputLineSplit = inputLine.split(",");
      String transactionId = inputLineSplit[0].trim();
      String variableName = inputLineSplit[1].trim();

      if (transactionMap.containsKey(transactionId)) {
        operationQueue.add(new Operation(OperationType.READ, transactionId, variableName));
      } else {
        System.out.println("Transaction " + transactionId + " doesn't exists!");
      }
    } else if (line.startsWith("W")) {
      String inputLine = line.substring(2, line.length() - 1);
      String[] inputLineSplit = inputLine.split(",");
      String transactionId = inputLineSplit[0].trim();
      String variableName = inputLineSplit[1].trim();
      int value = Integer.parseInt(inputLineSplit[2].trim());

      if (transactionMap.containsKey(transactionId)) {
        operationQueue.add(new Operation(OperationType.WRITE, transactionId, variableName, value));
      } else {
        System.out.println("Transaction " + transactionId + " doesn't exists!");
      }
    }
  }

  /**
   * @param transactionId   ID of the new Transaction
   * @param transactionType Type of the new Transaction
   * @param time            Start time of the new Transaction
   */
  private void beginTransaction(String transactionId, TransactionType transactionType,
      int time) {
    if (transactionMap.containsKey(transactionId)) {
      System.out.println("Transaction " + transactionId + " already exists!");
    } else {
      Transaction transaction = new Transaction(transactionId, transactionType, time);
      transactionMap.put(transactionId, transaction);
      switch (transactionType) {
        case RO -> System.out.println("Transaction " + transactionId + " begins and is read-only.");
        case RW -> System.out.println("Transaction " + transactionId + " begins.");
      }
    }
  }

  /**
   * Function to trigger failure of given site.
   *
   * @param siteId ID of the site to fail.
   */
  private void fail(int siteId) {
    DataManager dataManager = sites.get(siteId - 1);
    if (dataManager.isUp()) {
      System.out.println("Failing Site:" + siteId);
      dataManager.failSite(currentTime);
      for (Transaction transaction : transactionMap.values()) {
        if (
            !(transaction.getTransactionType() == TransactionType.RO) &&
                transaction.isLive &&
                transaction.getSitesAccessed().contains(siteId)
        ) {
          transaction.isLive = false;
        }
      }
    } else {
      System.out.println("Site " + siteId + " is down!");
    }
  }

  /**
   * Function to trigger recovery of given site.
   *
   * @param siteId ID of the site to recover.
   */
  private void recover(int siteId) {
    DataManager dataManager = sites.get(siteId - 1);
    if (dataManager.isUp()) {
      System.out.println("Site " + siteId + " is already up!");
    } else {
      System.out.println("Failing Site:" + siteId);
      dataManager.recoverSite(currentTime);
    }
  }

  /**
   * Function to trigger end of given transaction.
   *
   * @param transactionId ID of the transaction to end.
   */
  private void end(String transactionId) {
    if (transactionMap.containsKey(transactionId)) {
      if (transactionMap.get(transactionId).isLive) {
        commit(transactionId, currentTime);
      } else {
        abort(transactionId, true);
      }
    } else {
      System.out.println("Transaction " + transactionId + " doesn't exist!");
    }
  }

  /**
   * Function to commit given transaction.
   *
   * @param transactionId ID of the transaction to commit.
   * @param time          commit time.
   */
  private void commit(String transactionId, int time) {
    for (DataManager dataManager : sites) {
      dataManager.commitTransaction(transactionId, time);
    }
    transactionMap.remove(transactionId);
    System.out.println("Transaction " + transactionId + " committed!");
  }
}
