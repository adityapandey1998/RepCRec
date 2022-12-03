package Data;

import Transactions.Constants;

/**
 * QueuedLock Class
 *
 * @author Shubham Jha
 */
public class QueuedLock extends Lock {

  public QueuedLock(String variableId, Constants.LockType lockType, String transactionId) {
    this.variableId = variableId;
    this.lockType = lockType;
    this.transactionId = transactionId;
  }

  public String getVariableId() {
    return variableId;
  }

  public String getTransactionId() {
    return transactionId;
  }
}
