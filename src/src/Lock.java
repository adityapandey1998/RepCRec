import Transactions.Constants;

import java.util.Set;

public abstract class Lock {
    Constants.LockType lockType;
    String variableId;
    protected Set<String> transactionIds;
    String transactionId;

    public Constants.LockType getLockType() {
        return lockType;
    }

    public String getVariableId() {
        return variableId;
    }
}
