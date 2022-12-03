package Data;

import Transactions.Constants;
import java.util.Set;

/**
 * @author Shubham Jha
 */
public abstract class Lock {

  public Set<String> transactionIds;
  public String transactionId;
  Constants.LockType lockType;
  String variableId;

  public Constants.LockType getLockType() {
    return lockType;
  }

  public String getVariableId() {
    return variableId;
  }
}
