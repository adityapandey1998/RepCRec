package Data;

import Transactions.Constants;

import java.util.*;

import static Transactions.Constants.NUM_VARIABLES;

/**
 * Translates Represents a single site. Maintains site state and is responsible for
 * managing variables and locks.
 * @author Shubham Jha
 */
public class DataManager {

  int siteId;
  boolean isUp;
  Map<String, Variable> dataMap;
  Map<String, LockManager> lockMap;
  List<Integer> failureTimestamps, recoveryTimestamps;

  /**
   * Initialize Class Members and the data
   */
  public DataManager(int siteId) {
    this.siteId = siteId;
    this.isUp = true;

    this.dataMap = new TreeMap<>();
    this.lockMap = new HashMap<>();
    this.failureTimestamps = new ArrayList<>();
    this.recoveryTimestamps = new ArrayList<>();

    for (int varIdx = 1; varIdx <= NUM_VARIABLES; varIdx++) {

      String varId = "x" + varIdx;

      if (varIdx % 2 == 0) {
        // even, replicate on all sites
        this.dataMap.put(varId, new Variable(varId,
            new CommitValue(varIdx * 10, 0), true));
        this.lockMap.put(varId, new LockManager(varId));
      } else if (varIdx % 10 + 1 == this.siteId) {
        // odd, replicate on some sites
        this.dataMap.put(varId, new Variable(varId, new CommitValue(varIdx * 10, 0), false));
        this.lockMap.put(varId, new LockManager(varId));
      }
    }
  }

  /**
   * @return returns the status of the current site.
   */
  public boolean isUp() {
    return isUp;
  }

  /**
   * @param up Updated the status of the current site.
   */
  public void setUp(boolean up) {
    isUp = up;
  }

  /**
   * @return Returns ID of the current site.
   */
  public int getSiteId() {
    return siteId;
  }

  /**
   * @param variableId ID of the variable to check for.
   * @return Returns whether a variable exists in the dataMap.
   */
  public boolean variableExists(String variableId) {
    return this.dataMap.containsKey(variableId);
  }

  /**
   * @param variableId ID of the variable to read
   * @param timestamp The timestamp before which the variable needs to be read
   * @return Returns the latest value of the variable specified by variableId before the timestamp.
   */
  public Result readVariableSnapshot(String variableId, int timestamp) {
    Variable var = this.dataMap.get(variableId);
    if (var.isReadable) {
      for (CommitValue commitValue : var.committedValues) {
        if (commitValue.getCommitTimestamp() <= timestamp) {
          if (var.isReplicated) {
            for (int failedTimestamp : this.failureTimestamps) {
              if (commitValue.getCommitTimestamp() < failedTimestamp
                  && failedTimestamp <= timestamp) {
                return new Result(false);
              }
            }
          }
          return new Result(true, commitValue.getValue());
        }
      }
    }
    return new Result(false);
  }

  /**
   * @param transactionId the ID of the context transaction
   * @param variableId the ID of the variable to read
   * @return returns the value of a variable in the context of a specific transaction
   */
  public Result readVariable(String transactionId, String variableId) {
    Variable var = this.dataMap.get(variableId);
    if (var.isReadable) {
      LockManager lockManager = lockMap.get(variableId);
      Lock currentLock = lockManager.currentLock;
      if (currentLock != null) {
        if (currentLock.getLockType() == Constants.LockType.READ) {
          if (currentLock.transactionIds.contains(transactionId)) {
            return new Result(true, var.getMostRecentlyCommittedValue().getValue());
          }
          if (!lockManager.checkQueuedWriteLocks(transactionId)) {
            lockManager.shareReadLock(transactionId);
            return new Result(true, var.getMostRecentlyCommittedValue().getValue());
          }
          lockManager.addToLockQueue(
              new QueuedLock(variableId, Constants.LockType.READ, transactionId));
          return new Result(false);
        }
        if (currentLock.transactionId.equals(transactionId)) {
          return new Result(true, var.getTempValue().getValue());
        }
        lockManager.addToLockQueue(new QueuedLock(variableId, Constants.LockType.READ, transactionId));
        return new Result(false);
      }
      lockManager.setCurrentLock(new ReadLock(variableId, transactionId));
      return new Result(true, var.getMostRecentlyCommittedValue().getValue());
    }
    return new Result(false);
  }

  /**
   * @param transactionId the ID of the transaction on which the lock needs to be acquired.
   * @param variableId the ID of the variable on which the lock needs to be acquired.
   * @return Returns true if write lock successfully acquired, false otherwise.
   */
  public boolean acquireWriteLock(String transactionId, String variableId) {
    LockManager lockManager = this.lockMap.get(variableId);
    Lock currentLock = lockManager.currentLock;
    if (currentLock != null) {
      if (currentLock.getLockType() == Constants.LockType.READ) {
        if (currentLock.transactionIds.size() != 1) {
          lockManager.addToLockQueue(
              new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
          return false;
        }
        if (currentLock.transactionIds.contains(transactionId)) {
          if (lockManager.checkQueuedWriteLocks(transactionId)) {
            lockManager.addToLockQueue(
                new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
            return false;
          }
          return true;
        }
        lockManager.addToLockQueue(new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
        return false;
      }
      if (transactionId.equals(currentLock.transactionId)) {
        return true;
      }
      lockManager.addToLockQueue(new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
      return false;
    }
    return true;
  }

  public void writeVariable(String transactionId, String variableId, int value) {
    Variable var = this.dataMap.get(variableId);
    LockManager lockManager = this.lockMap.get(variableId);
    Lock currentLock = lockManager.currentLock;

    if (currentLock != null) {
      if (currentLock.getLockType() == Constants.LockType.READ) {
        if (currentLock.transactionIds.size() == 1) {
          if (currentLock.transactionIds.contains(transactionId)) {
            if (!lockManager.checkQueuedWriteLocks(transactionId)) {
              lockManager.promoteCurrentLock(new WriteLock(variableId, transactionId));
              var.proposedValue = new ProposedValue(value, transactionId);
              return;
            }
            return;
          }
        } else {
          return;
        }
      }
      if (transactionId.equals(currentLock.transactionId)) {
        var.proposedValue = new ProposedValue(value, transactionId);
        return;
      }
      return;
    }
    lockManager.setCurrentLock(new WriteLock(variableId, transactionId));
    var.proposedValue = new ProposedValue(value, transactionId);
  }

  /**
   * Dumps the site status and the values of the variables to console.
   */
  public void dump() {
    String siteStatus = this.isUp ? "UP" : "DOWN";
    StringBuilder result = new StringBuilder(
        String.format("site %d [%s] - ", this.siteId, siteStatus));
    for (Variable v : this.dataMap.values()) {
      String varStr = String.format("%s: %d, ", v.variableId, v.getMostRecentlyCommittedValue().getValue());
      result.append(varStr);
    }
    System.out.println(result);
  }

  /**
   * @param transactionId The ID of the transaction to abort.
   */
  public void abortTransaction(String transactionId) {
    for(LockManager lockManager : this.lockMap.values()) {
      lockManager.releaseTransactionLock(transactionId);
      List<QueuedLock> tempQ = new ArrayList<>(lockManager.queue);
      for(QueuedLock queuedLock : tempQ) {
        if(queuedLock.transactionId.equals(transactionId)) {
          lockManager.queue.remove(queuedLock);
        }
      }
    }
    resolveLockMap();
  }

  /**
   * @param transactionId The ID of the transaction to be committed.
   * @param commitTimestamp The timestamp of the commit.
   */
  public void commitTransaction(String transactionId, int commitTimestamp) {
    for (LockManager lockManager : this.lockMap.values()) {
      lockManager.releaseTransactionLock(transactionId);
      for (QueuedLock queuedLock : lockManager.queue) {
        if(queuedLock.transactionId.equals(transactionId)) {
          return;
        }
      }
    }
    for (Variable val : this.dataMap.values()) {
      if (val.proposedValue != null && val.proposedValue.getTransactionId().equals(transactionId)) {
        val.addCommitValue(new CommitValue(val.proposedValue.getValue(), commitTimestamp));
        val.isReadable = true;
      }
    }
    resolveLockMap();
  }

  /**
   * Analyze the lockMap and move specific queued locks to the front.
   */
  public void resolveLockMap() {
    for (Map.Entry<String, LockManager> entry : this.lockMap.entrySet()) {
      String variableId = entry.getKey();
      LockManager lockManager = entry.getValue();

      if (lockManager.queue != null && !lockManager.queue.isEmpty()) {
        if (lockManager.currentLock == null) {
          Lock firstQueueLock = lockManager.queue.get(0);
          lockManager.queue.remove(0);

          if (firstQueueLock.getLockType() == Constants.LockType.READ) {
            lockManager.setCurrentLock(
                new ReadLock(firstQueueLock.getVariableId(), firstQueueLock.transactionId));
          } else {
            lockManager.setCurrentLock(
                new WriteLock(firstQueueLock.getVariableId(), firstQueueLock.transactionId));
          }
        }

        if (lockManager.currentLock.getLockType() == Constants.LockType.READ) {
          for (QueuedLock queuedLock : lockManager.queue) {
            if (queuedLock.getLockType() == Constants.LockType.WRITE) {
              if (lockManager.currentLock.transactionIds.size() == 1 &&
                  lockManager.currentLock.transactionIds.contains(queuedLock.getTransactionId())) {
                lockManager.promoteCurrentLock(
                    new WriteLock(queuedLock.getVariableId(), queuedLock.getTransactionId()));
                lockManager.queue.remove(queuedLock);
              }
              break;
            }
            lockManager.shareReadLock(queuedLock.getTransactionId());
            lockManager.queue.remove(queuedLock);
          }
        }
      }
    }
  }

  /**
   * @param timestamp timestamp at which to fail the current site.
   */
  public void failSite(int timestamp) {
    this.isUp = false;
    this.failureTimestamps.add(timestamp);
    for (LockManager lockManager : this.lockMap.values()) {
      lockManager.clearLocks();
    }
  }

  /**
   * @param timestamp timestamp at which the current site recovers
   */
  public void recoverSite(int timestamp) {
    this.isUp = true;
    this.recoveryTimestamps.add(timestamp);
    for (Variable var : this.dataMap.values()) {
      if (var.isReplicated) {
        var.isReadable = false;
      }
    }
  }

  /**
   * @param currentLock the current lock
   * @param queuedLock a queued lock
   * @return returns true if the current lock is blocking an already queued lock.
   */
  boolean currentLockBlocksQueuedLock(Lock currentLock, Lock queuedLock) {
    if (currentLock.lockType == Constants.LockType.READ) {
      if (queuedLock.lockType == Constants.LockType.READ || (currentLock.transactionIds.size() == 1 && currentLock.transactionIds.contains(queuedLock.transactionId))
      ) {
        return false;
      }
      return true;
    }
    return !currentLock.transactionId.equals(queuedLock.transactionId);
  }

  /**
   * @param queuedLockFirst a queued lock
   * @param queuedLockSecond another queued lock
   * @return returns true if a queued lock is blocking another queued lock
   */
  boolean queuedLockBlocksQueuedLock(Lock queuedLockFirst, Lock queuedLockSecond) {
    if (queuedLockFirst.lockType == Constants.LockType.READ
        && queuedLockSecond.lockType == Constants.LockType.READ) {
      return false;
    }
    return !queuedLockFirst.transactionId.equals(queuedLockSecond.transactionId);
  }

  /**
   * @return returns the generated waitsFor graph for the current site
   */
  public Map<String, HashSet<String>> generateWaitsforGraph() {
    HashMap<String, HashSet<String>> waitsforGraph = new HashMap<>();
    for (Map.Entry<String, LockManager> entry : this.lockMap.entrySet()) {
      LockManager lockManager = entry.getValue();

      if (lockManager.currentLock == null || lockManager.queue == null || lockManager.queue.isEmpty()) {
        continue;
      }

      for (QueuedLock queuedLock : lockManager.queue) {
        if (currentLockBlocksQueuedLock(lockManager.currentLock, queuedLock)) {
          if (lockManager.currentLock.lockType == Constants.LockType.READ) {
            for (String transactionId : lockManager.currentLock.transactionIds) {
              if (!transactionId.equals(queuedLock.transactionId)) {
                if (!waitsforGraph.containsKey(queuedLock.transactionId)) {
                  waitsforGraph.put(queuedLock.transactionId, new HashSet<>());
                }
                HashSet<String> tempSet = waitsforGraph.get(queuedLock.transactionId);
                tempSet.add(transactionId);
                waitsforGraph.put(queuedLock.transactionId, tempSet);
              }
            }
          } else {
            if (!lockManager.currentLock.transactionId.equals(queuedLock.transactionId)) {
              if (!waitsforGraph.containsKey(queuedLock.transactionId)) {
                waitsforGraph.put(queuedLock.transactionId, new HashSet<>());
              }
              HashSet<String> tempSet = waitsforGraph.get(queuedLock.transactionId);
              tempSet.add(lockManager.currentLock.transactionId);
              waitsforGraph.put(queuedLock.transactionId, tempSet);
            }
          }
        }
      }

      for (int i = 0; i < lockManager.queue.size(); i++) {
        for (int j = 0; j < i; j++) {
          if (queuedLockBlocksQueuedLock(lockManager.queue.get(j), lockManager.queue.get(i))) {
            String key = lockManager.queue.get(i).transactionId;
            if (!waitsforGraph.containsKey(key)) {
              waitsforGraph.put(key, new HashSet<>());
            }
            HashSet<String> tempSet = waitsforGraph.get(key);
            tempSet.add(lockManager.queue.get(j).transactionId);
            waitsforGraph.put(key, tempSet);
          }
        }
      }
    }
    return waitsforGraph;
  }
}
