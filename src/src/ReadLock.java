import Transactions.Constants;

import java.util.Set;

public class ReadLock {
    final Constants.LockType lockType = Constants.LockType.READ;
    final String variableId;
    final Set<String> transactionIds;

    public ReadLock(String variableId, Set<String> transactionIds) {
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