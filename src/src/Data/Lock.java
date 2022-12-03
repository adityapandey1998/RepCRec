package Data;

import Transactions.Constants;

import java.util.Set;

public abstract class Lock {

  public Set<String> transactionIds;
  Constants.LockType lockType;
  String variableId;
  public String transactionId;

  public Constants.LockType getLockType() {
    return lockType;
  }

  public String getVariableId() {
    return variableId;
  }
}
