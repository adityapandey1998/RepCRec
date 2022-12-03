package Data;

import Transactions.Constants;
import java.util.ArrayList;
import java.util.List;


/**
 * Lock Manager handles all the setting, promoting, sharing and releasing of transaction and
 * variable locks.
 *
 * @author Shubham Jha
 */
public class LockManager {

  public Lock currentLock;
  public List<QueuedLock> queue;
  String variableId;

  public LockManager(String variableId) {
    this.variableId = variableId;
    this.currentLock = null;
    this.queue = new ArrayList<>();
  }

  public void setCurrentLock(Lock currentLock) {
    this.currentLock = currentLock;
  }

  void clearLocks() {
    this.currentLock = null;
    this.queue = new ArrayList<>();
  }

  /**
   * @param writeLock writeLock to set as current
   */
  public void promoteCurrentLock(WriteLock writeLock) {
    if (this.currentLock != null &&
        this.currentLock.lockType == Constants.LockType.READ &&
        this.currentLock.transactionIds.size() == 1 &&
        this.currentLock.transactionIds.contains(writeLock.transactionId)
    ) {
      setCurrentLock(writeLock);
    }
  }

  /**
   * @param transactionId Transaction ID
   */
  public void shareReadLock(String transactionId) {
    if (currentLock.lockType == Constants.LockType.READ) {
      this.currentLock.transactionIds.add(transactionId);
    }
  }

  /**
   * Add a new lock to the lock queue
   *
   * @param newLock New Queued lock
   */
  public void addToLockQueue(QueuedLock newLock) {
    for (QueuedLock queuedLock : this.queue) {
      if (queuedLock.transactionId.equals(newLock.transactionId)) {
        if (queuedLock.lockType == newLock.lockType
            || newLock.lockType == Constants.LockType.READ) {
          return;
        }
      }
    }
    this.queue.add(newLock);
  }

  /**
   * @param transactionId Transaction ID
   * @return checking other Queue Lock on given transaction.
   */
  public boolean checkQueuedWriteLocks(String transactionId) {
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

  /**
   * releasing transaction lock.
   *
   * @param transactionId Transaction ID
   */
  public void releaseTransactionLock(String transactionId) {
    if (this.currentLock != null) {
      if (this.currentLock.lockType == Constants.LockType.READ) {
        this.currentLock.transactionIds.remove(transactionId);
        if (this.currentLock.transactionIds.isEmpty()) {
          this.currentLock = null;
        }
      } else {
        if (this.currentLock.transactionId.equals(transactionId)) {
          this.currentLock = null;
        }
      }
    }
  }
}
