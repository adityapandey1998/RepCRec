package Data;

import Transactions.Constants;

import java.util.*;

import static Transactions.Constants.NUM_VARIABLES;

public class DataManager {

  int siteId;
  boolean isUp;
  Map<String, Variable> dataMap;
  Map<String, LockManager> lockMap;
  List<Integer> failureTimestamps, recoveryTimestamps;

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

  public boolean isUp() {
    return isUp;
  }

  public void setUp(boolean up) {
    isUp = up;
  }

  public int getSiteId() {
    return siteId;
  }

  public boolean variableExists(String variableId) {
    return this.dataMap.containsKey(variableId);
  }

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
    resolveLockTable();
  }

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
    resolveLockTable();
  }

  public void resolveLockTable() {
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

  public void fail(int timestamp) {
    this.isUp = false;
    this.failureTimestamps.add(timestamp);
    for (LockManager lockManager : this.lockMap.values()) {
      lockManager.clearLocks();
    }
  }

  public void recover(int timestamp) {
    this.isUp = true;
    this.recoveryTimestamps.add(timestamp);
    for (Variable var : this.dataMap.values()) {
      if (var.isReplicated) {
        var.isReadable = false;
      }
    }
  }

  boolean currentBlocksQueued(Lock currentLock, Lock queuedLock) {
    if (currentLock.lockType == Constants.LockType.READ) {
      if (queuedLock.lockType == Constants.LockType.READ || (currentLock.transactionIds.size() == 1 && currentLock.transactionIds.contains(queuedLock.transactionId))
      ) {
        return false;
      }
      return true;
    }
    return !currentLock.transactionId.equals(queuedLock.transactionId);
  }

  boolean queuedBlocksQueued(Lock queuedLockLeft, Lock queuedLockRight) {
    if (queuedLockLeft.lockType == Constants.LockType.READ
        && queuedLockRight.lockType == Constants.LockType.READ) {
      return false;
    }
    return !queuedLockLeft.transactionId.equals(queuedLockRight.transactionId);
  }

  public Map<String, HashSet<String>> generateBlockingGraph() {
    HashMap<String, HashSet<String>> graph = new HashMap<>();
    for (Map.Entry<String, LockManager> entry : this.lockMap.entrySet()) {
      LockManager lockManager = entry.getValue();

      if (lockManager.currentLock == null || lockManager.queue == null || lockManager.queue.isEmpty()) {
        continue;
      }

      for (QueuedLock queuedLock : lockManager.queue) {
        if (currentBlocksQueued(lockManager.currentLock, queuedLock)) {
          if (lockManager.currentLock.lockType == Constants.LockType.READ) {
            for (String transactionId : lockManager.currentLock.transactionIds) {
              if (!transactionId.equals(queuedLock.transactionId)) {
                if (!graph.containsKey(queuedLock.transactionId)) {
                  graph.put(queuedLock.transactionId, new HashSet<>());
                }
                HashSet<String> tempSet = graph.get(queuedLock.transactionId);
                tempSet.add(transactionId);
                graph.put(queuedLock.transactionId, tempSet);
              }
            }
          } else {
            if (!lockManager.currentLock.transactionId.equals(queuedLock.transactionId)) {
              if (!graph.containsKey(queuedLock.transactionId)) {
                graph.put(queuedLock.transactionId, new HashSet<>());
              }
              HashSet<String> tempSet = graph.get(queuedLock.transactionId);
              tempSet.add(lockManager.currentLock.transactionId);
              graph.put(queuedLock.transactionId, tempSet);
            }
          }
        }
      }

      for (int i = 0; i < lockManager.queue.size(); i++) {
        for (int j = 0; j < i; j++) {
          if (queuedBlocksQueued(lockManager.queue.get(j), lockManager.queue.get(i))) {
            String key = lockManager.queue.get(i).transactionId;
            if (!graph.containsKey(key)) {
              graph.put(key, new HashSet<>());
            }
            HashSet<String> tempSet = graph.get(key);
            tempSet.add(lockManager.queue.get(j).transactionId);
            graph.put(key, tempSet);
          }
        }
      }
    }
    return graph;
  }

}
