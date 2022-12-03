import Transactions.Constants;

import java.util.Set;

public class QueuedLock extends Lock {

  String transactionId;

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
