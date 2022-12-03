import Data.*;
import Transactions.Constants;
import Transactions.Transaction;

import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
  int siteId;
  boolean isUp;
  Map<String, Variable> data;
  Map<String, LockManager> lockTable;
  List<Integer> failedTimestampList;
  List<Transaction> recoveredTransactionList;

  DataManager(int siteId) {
    this.siteId = siteId;
    this.isUp = true;
    this.data = new HashMap<>();
    this.lockTable = new HashMap<>();
    this.failedTimestampList = new ArrayList<>();
    this.recoveredTransactionList = new ArrayList<>();

    for(int varIdx = 1; varIdx <= 20; varIdx++) {
      String varId = "x" + varIdx;
      if(varIdx % 2 == 0) {
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

  boolean hasVariable(String variableId) {
    return this.data.containsKey(variableId);
  }

  Result readSnapshot(String variableId, int timestamp) {
    Variable var = this.data.get(variableId);
    if(var.isReadable) {
      for (CommitValue commitValue : var.committedValues) {
        if(commitValue.getCommitTimestamp() <= timestamp) {
          if(var.isReplicated) {
            for(int failedTimestamp : this.failedTimestampList) {
              if(commitValue.getCommitTimestamp() < failedTimestamp && failedTimestamp <= timestamp) {
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

  Result read(String transactionId, String variableId) {
    Variable var = this.data.get(variableId);
    if(var.isReadable) {
      LockManager lockManager = lockTable.get(variableId);
      Lock currentLock = lockManager.currentLock;
      if(currentLock != null) {
        if (currentLock.getLockType() == Constants.LockType.READ) {
          if (currentLock.transactionIds.contains(transactionId)) {
            return new Result(true, var.getLastCommittedValue().getValue());
          }
          if (!lockManager.hasOtherQueuedWriteLock(transactionId)) {
            lockManager.shareReadLock(transactionId);
            return new Result(true, var.getLastCommittedValue().getValue());
          }
          lockManager.addToQueue(new QueuedLock(variableId, Constants.LockType.READ, transactionId));
          return new Result(false);
        }
        if (currentLock.transactionId.equals(transactionId)) {
          return new Result(true, var.getTempValue());
        }
        lockManager.addToQueue(new QueuedLock(variableId, Constants.LockType.READ, transactionId));
        return new Result(false);
      }
      lockManager.setCurrentLock(new ReadLock(variableId, transactionId));
      return new Result(true, var.getLastCommittedValue().getValue());
    }
    return new Result(false);
  }

  boolean getWriteLock(String transactionId, String variableId) {
    LockManager lockManager = this.lockTable.get(variableId);
    Lock currentLock = lockManager.currentLock;
    if(currentLock != null) {
      if(currentLock.getLockType() == Constants.LockType.READ) {

      }
    }
    return true;
  }
}
