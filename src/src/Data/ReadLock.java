package Data;

import Transactions.Constants;
import java.util.HashSet;
import java.util.Set;

public class ReadLock extends Lock {

  final Constants.LockType lockType = Constants.LockType.READ;

  public ReadLock(String variableId, String transactionId) {
    this.variableId = variableId;
    this.transactionIds = new HashSet<>();
    this.transactionIds.add(transactionId);
    this.transactionId = transactionId;
  }

  public String getVariableId() {
    return variableId;
  }

  public Set<String> getTransactionIds() {
    return transactionIds;
  }

}
