package Data;

import Transactions.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {

  int siteId;
  boolean isUp;

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

  public Map<String, Variable> getData() {
    return data;
  }

  public void setData(Map<String, Variable> data) {
    this.data = data;
  }

  public Map<String, LockManager> getLockTable() {
    return lockTable;
  }

  public void setLockTable(Map<String, LockManager> lockTable) {
    this.lockTable = lockTable;
  }

  public List<Integer> getFailedTimestampList() {
    return failedTimestampList;
  }

  public void setFailedTimestampList(List<Integer> failedTimestampList) {
    this.failedTimestampList = failedTimestampList;
  }

  public List<Transaction> getRecoveredTransactionList() {
    return recoveredTransactionList;
  }

  public void setRecoveredTransactionList(
      List<Transaction> recoveredTransactionList) {
    this.recoveredTransactionList = recoveredTransactionList;
  }

  Map<String, Variable> data;
  Map<String, LockManager> lockTable;
  List<Integer> failedTimestampList;
  List<Transaction> recoveredTransactionList;

  public DataManager(int siteId) {
    this.siteId = siteId;
    this.isUp = true;
    this.data = new HashMap<>();
    this.lockTable = new HashMap<>();
    this.failedTimestampList = new ArrayList<>();
    this.recoveredTransactionList = new ArrayList<>();

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
  }
}
