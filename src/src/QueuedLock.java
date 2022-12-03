import Transactions.Constants;

import java.util.Set;

public class QueuedLock {
    final Constants.LockType lockType;
    final String variableId;
    final Set<String> transactionIds;

    public QueuedLock(String variableId, Constants.LockType lockType, Set<String> transactionIds) {
        this.variableId = variableId;
        this.lockType = lockType;
        this.transactionIds = transactionIds;
    }

    public String getVariableId() {
        return variableId;
    }

    public Set<String> getTransactionIds() {
        return transactionIds;
    }
}
