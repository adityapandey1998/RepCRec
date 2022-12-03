package Data;

import Transactions.Constants;

public class WriteLock extends Lock {

  final Constants.LockType lockType = Constants.LockType.WRITE;

  public WriteLock(String variableId, String transactionId) {
    this.variableId = variableId;
    this.transactionId = transactionId;
  }

  public String getVariableId() {
    return variableId;
  }

  public String getTransactionId() {
    return transactionId;
  }
}
