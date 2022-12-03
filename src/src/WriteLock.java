import Transactions.Constants;

import java.util.Set;

public class WriteLock {
    final Constants.LockType lockType = Constants.LockType.WRITE;
    final String variableId;
    final Set<String> transactionIds;

    public WriteLock(String variableId, Set<String> transactionIds) {
        this.variableId = variableId;
        this.transactionIds = transactionIds;
    }

    public String getVariableId() {
        return variableId;
    }

    public Set<String> getTransactionIds() {
        return transactionIds;
    }
}
