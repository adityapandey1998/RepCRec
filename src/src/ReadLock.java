import Transactions.Constants;

import java.util.Set;

public class ReadLock {
    String variableId;
    Constants.LockType lockType;
    Set<String> transactionIds;

    public ReadLock(String variableId, Constants.LockType lockType, Set<String> transactionIds) {
        this.variableId = variableId;
        this.lockType = lockType;
        this.transactionIds = transactionIds;
    }

    public String getVariableId() {
        return variableId;
    }

    public void setVariableId(String variableId) {
        this.variableId = variableId;
    }

    public Constants.LockType getLockType() {
        return lockType;
    }

    public void setLockType(Constants.LockType lockType) {
        this.lockType = lockType;
    }

    public Set<String> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(Set<String> transactionIds) {
        this.transactionIds = transactionIds;
    }
}
