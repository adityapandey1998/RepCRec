import Transactions.Constants;

import java.util.Set;

public abstract class Lock {

  protected Set<String> transactionIds;
  Constants.LockType lockType;
  String variableId;
  String transactionId;

  public Constants.LockType getLockType() {
    return lockType;
  }

  public String getVariableId() {
    return variableId;
  }
}
