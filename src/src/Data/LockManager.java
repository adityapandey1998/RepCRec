package Data;

import Transactions.Constants;

import java.util.ArrayList;
import java.util.List;

public class LockManager {

  String variableId;
  Lock currentLock;
  List<QueuedLock> queue;

  public LockManager(String variableId) {
    this.variableId = variableId;
    this.currentLock = null;
    this.queue = new ArrayList<>();
  }

  public void setCurrentLock(Lock currentLock) {
    this.currentLock = currentLock;
  }

  void clear() {
    this.currentLock = null;
    this.queue = new ArrayList<>();
  }

  void promoteCurrentLock(WriteLock writeLock) {
    if (this.currentLock != null &&
        this.currentLock.lockType == Constants.LockType.READ &&
        this.currentLock.transactionIds.size() == 1 &&
        this.currentLock.transactionIds.contains(writeLock.transactionId)
    ) {
      setCurrentLock(writeLock);
    }
  }

  void shareReadLock(String transactionId) {
    if (currentLock.lockType == Constants.LockType.READ) {
      this.currentLock.transactionIds.add(transactionId);
    }
  }

  void addToQueue(QueuedLock newLock) {
    for (QueuedLock queuedLock : this.queue) {
      if (
          queuedLock.transactionId.equals(newLock.transactionId) &&
              (queuedLock.lockType == newLock.lockType
                  || newLock.lockType == Constants.LockType.READ)
      ) {
        return;
      }
    }
    this.queue.add(newLock);
  }

  boolean hasOtherQueuedWriteLock(String transactionId) {
    for (QueuedLock queuedLock : this.queue) {
      if (queuedLock.lockType == Constants.LockType.WRITE) {
        if (!transactionId.isEmpty() && queuedLock.transactionId.equals(transactionId)) {
          continue;
        }
        return true;
      }
    }
    return false;
  }

  void releaseCurrentLockByTransaction(String transactionId) {
    if (this.currentLock != null) {
      if (this.currentLock.lockType == Constants.LockType.READ) {
        this.currentLock.transactionIds.remove(transactionId);
        if (this.currentLock.transactionIds.isEmpty()) {
          this.currentLock = null;
        }
      } else {
        if (currentLock.transactionId.equals(transactionId)) {
          this.currentLock = null;
        }
      }
    }
  }
}
