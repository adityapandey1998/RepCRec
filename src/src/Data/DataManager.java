package Data;

import Transactions.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DataManager {

  int siteId;
  boolean isUp;
  Map<String, Variable> data;
  Map<String, LockManager> lockTable;
  List<Integer> failedTimestampList;
  List<Integer> recoveredTimestampList;
  public DataManager(int siteId) {
    this.siteId = siteId;
    this.isUp = true;
    this.data = new HashMap<>();
    this.lockTable = new HashMap<>();
    this.failedTimestampList = new ArrayList<>();
    this.recoveredTimestampList = new ArrayList<>();

    for (int varIdx = 1; varIdx <= 20; varIdx++) {
      String varId = "x" + varIdx;
      if (varIdx % 2 == 0) {
        // replicate on all sites
        this.data.put(varId, new Variable(varId,
            new CommitValue(varIdx * 10, 0), true));
        this.lockTable.put(varId, new LockManager(varId));
      } else if (varIdx % 10 + 1 == this.siteId) {
        this.data.put(varId, new Variable(varId, new CommitValue(varIdx * 10, 0), false));
        this.lockTable.put(varId, new LockManager(varId));
      }
    }
  }

  public int getSiteId() {
    return siteId;
  }

  public void setSiteId(int siteId) {
    this.siteId = siteId;
  }

  public boolean isUp() {
    return isUp;
  }

  public void setUp(boolean up) {
    isUp = up;
  }

  public boolean hasVariable(String variableId) {
    return this.data.containsKey(variableId);
  }

  public Result readSnapshot(String variableId, int timestamp) {
    Variable var = this.data.get(variableId);
    if (var.isReadable) {
      for (CommitValue commitValue : var.committedValues) {
        if (commitValue.getCommitTimestamp() <= timestamp) {
          if (var.isReplicated) {
            for (int failedTimestamp : this.failedTimestampList) {
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

  public Result read(String transactionId, String variableId) {
    Variable var = this.data.get(variableId);
    if (var.isReadable) {
      LockManager lockManager = lockTable.get(variableId);
      Lock currentLock = lockManager.currentLock;
      if (currentLock != null) {
        if (currentLock.getLockType() == Constants.LockType.READ) {
          if (currentLock.transactionIds.contains(transactionId)) {
            return new Result(true, var.getLastCommittedValue().getValue());
          }
          if (!lockManager.hasOtherQueuedWriteLock(transactionId)) {
            lockManager.shareReadLock(transactionId);
            return new Result(true, var.getLastCommittedValue().getValue());
          }
          lockManager.addToQueue(
              new QueuedLock(variableId, Constants.LockType.READ, transactionId));
          return new Result(false);
        }
        if (currentLock.transactionId.equals(transactionId)) {
          return new Result(true, var.getTempValue().getValue());
        }
        lockManager.addToQueue(new QueuedLock(variableId, Constants.LockType.READ, transactionId));
        return new Result(false);
      }
      lockManager.setCurrentLock(new ReadLock(variableId, transactionId));
      return new Result(true, var.getLastCommittedValue().getValue());
    }
    return new Result(false);
  }

  public boolean getWriteLock(String transactionId, String variableId) {
    LockManager lockManager = this.lockTable.get(variableId);
    Lock currentLock = lockManager.currentLock;
    if (currentLock != null) {
      if (currentLock.getLockType() == Constants.LockType.READ) {
        if (currentLock.transactionIds.size() != 1) {
          lockManager.addToQueue(
              new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
          return false;
        }
        if (currentLock.transactionIds.contains(transactionId)) {
          if (lockManager.hasOtherQueuedWriteLock(transactionId)) {
            lockManager.addToQueue(
                new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
            return false;
          }
          return true;
        }
        lockManager.addToQueue(new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
        return false;
      }
      if (transactionId.equals(currentLock.transactionId)) {
        return true;
      }
      lockManager.addToQueue(new QueuedLock(variableId, Constants.LockType.WRITE, transactionId));
      return false;
    }
    return true;
  }

  public void write(String transactionId, String variableId, int value) {
    Variable var = this.data.get(variableId);
    LockManager lockManager = this.lockTable.get(variableId);
    Lock currentLock = lockManager.currentLock;

    if (currentLock != null) {
      if (currentLock.getLockType() == Constants.LockType.READ) {
        if (currentLock.transactionIds.size() == 1) {
          if (currentLock.transactionIds.contains(transactionId)) {
            if (!lockManager.hasOtherQueuedWriteLock(transactionId)) {
              lockManager.promoteCurrentLock(new WriteLock(variableId, transactionId));
              var.tempValue = new TempValue(value, transactionId);
              return;
            }
          }
        }
      }
      if (transactionId.equals(currentLock.transactionId)) {
        var.tempValue = new TempValue(value, transactionId);
        return;
      }
    }
    lockManager.setCurrentLock(new WriteLock(variableId, transactionId));
    var.tempValue = new TempValue(value, transactionId);
  }

  public void dump() {
    String siteStatus = this.isUp ? "UP" : "DOWN";
    StringBuilder output = new StringBuilder(
        String.format("Site %d [%s] - ", this.siteId, siteStatus));
    for (Variable v : this.data.values()) {
      String varStr = String.format("%s: %d, ", v.variableId, v.getLastCommittedValue().getValue());
      output.append(varStr);
    }
    System.out.println(output);
  }

  public void commit(String transactionId, int commitTimestamp) {
    for (LockManager lockManager : this.lockTable.values()) {
      lockManager.releaseCurrentLockByTransaction(transactionId);
    }
    for (Variable val : this.data.values()) {
      if (val.tempValue != null && val.tempValue.getTransactionId().equals(transactionId)) {
        val.addCommitValue(new CommitValue(val.tempValue.getValue(), commitTimestamp));
        val.isReadable = true;
      }
    }
    resolveLockTable();
  }

  public void resolveLockTable() {
    for (Map.Entry<String, LockManager> entry : this.lockTable.entrySet()) {
      String variableId = entry.getKey();
      LockManager lockManager = entry.getValue();

      if (lockManager.queue != null) {
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
    this.failedTimestampList.add(timestamp);
    for (LockManager lockManager : this.lockTable.values()) {
      lockManager.clear();
    }
  }

  public void recover(int timestamp) {
    this.isUp = true;
    this.recoveredTimestampList.add(timestamp);
    for (Variable var : this.data.values()) {
      if (var.isReplicated) {
        var.isReadable = false;
      }
    }
  }

  boolean currentBlocksQueued(Lock currentLock, Lock queuedLock) {
    if (currentLock.lockType == Constants.LockType.READ) {
      if (queuedLock.lockType == Constants.LockType.READ ||
          (currentLock.transactionIds.size() == 1 && currentLock.transactionIds.contains(
              queuedLock.transactionId))
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
    for (Map.Entry<String, LockManager> entry : this.lockTable.entrySet()) {
      LockManager lockManager = entry.getValue();
      if (lockManager.currentLock == null || lockManager.queue == null) {
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
                graph.get(queuedLock.transactionId).add(transactionId);
              }
            }
          } else {
            if (!lockManager.currentLock.transactionId.equals(queuedLock.transactionId)) {
              if (!graph.containsKey(queuedLock.transactionId)) {
                graph.put(queuedLock.transactionId, new HashSet<>());
              }
              graph.get(queuedLock.transactionId).add(lockManager.currentLock.transactionId);
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
            graph.get(key).add(lockManager.queue.get(j).transactionId);
          }
        }
      }
    }
    return graph;
  }

}
