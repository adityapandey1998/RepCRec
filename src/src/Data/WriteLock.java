package Data;

import Transactions.Constants;

public class WriteLock extends Lock {


  public WriteLock(String variableId, String transactionId) {
    this.variableId = variableId;
    this.transactionId = transactionId;
    this.lockType = Constants.LockType.WRITE;
  }

  public String getVariableId() {
    return variableId;
  }

  public String getTransactionId() {
    return transactionId;
  }
}
