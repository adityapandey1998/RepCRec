package Data;

import Transactions.Constants;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a read lock.
 * @author Shubham Jha
 */
public class ReadLock extends Lock {

  public ReadLock(String variableId, String transactionId) {
    this.variableId = variableId;
    this.transactionIds = new HashSet<>();
    this.transactionIds.add(transactionId);
    this.transactionId = transactionId;
    this.lockType = Constants.LockType.READ;
  }

  public String getVariableId() {
    return variableId;
  }

  public Set<String> getTransactionIds() {
    return transactionIds;
  }

}
