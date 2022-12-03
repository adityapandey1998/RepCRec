package Data;

import Transactions.Constants;

/**
 * Represents a write lock.
 * @author Shubham Jha
 */
public class WriteLock extends Lock {


  /**
   * Write Lock Constructor
   * @param variableId variable ID
   * @param transactionId transaction ID
   */
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
