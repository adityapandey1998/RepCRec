package Transactions;

public class Lock {
    private int variableID;
    private String transactionID;
    private Constants.LockType lockType;

    public Lock(int v, String t, Constants.LockType type) {
        this.variableID = v;
        this.transactionID = t;
        this.lockType = type;
    }

    public Constants.LockType getLockType() {
        return this.lockType;
    }

    public void setLockType(Constants.LockType lockType) {
        this.lockType = lockType;
    }

    public String getTransactionId() {
        return this.transactionID;
    }

    public void setTransactionID(String tId) {
        this.transactionID = tId;
    }

    public int getVariableID() {
        return this.variableID;
    }

    public void setVariableID(int varId) {
        this.variableID = varId;
    }

    @Override
    public String toString() {
        return this.variableID + " " + this.transactionID + " " + this.lockType;
    }
}
