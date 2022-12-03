import Transactions.Constants;

import java.util.Set;

public abstract class Lock {
    Constants.LockType lockType;
    String variableId;
    Set<String> transactionIds;
}
