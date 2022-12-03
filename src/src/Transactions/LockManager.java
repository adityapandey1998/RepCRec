package Transactions;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class LockManager {
    public ArrayList<Lock> lockArray;

    public LockManager() {
        lockArray = new ArrayList<Lock>();
    }

    public void addLock(int variable, String transactionID, Constants.LockType lockType) {
        this.lockArray.add(
            new Lock(variable, transactionID, lockType)
        );
    }

    public boolean canAcquireReadLock(int variableID, String transactionID) {
        ArrayList<Lock> locks = this.getLocks(variableID);
        if (locks.isEmpty()) {
            return true;
        }
        boolean hasWriteLock = false;
        for (int i = 0; i < locks.size(); i++) {
            if (locks.get(i).getLockType() == Constants.LockType.WRITE
                    && !locks.get(i).getTransactionId().equals(transactionID)) {
                hasWriteLock = true;
            }
        }
        if (hasWriteLock) {
            return false;
        } else {
            return true;
        }
    }

    public void acquireReadLock(int variableID, String transactionID) {
        if (this.canAcquireReadLock(variableID, transactionID)) {
            this.addLock(variableID, transactionID, Constants.LockType.READ);
        } else {
            return;
        }
    }

    public void promoteReadLockToWriteLock(int variableID, String transactionID) {
        for (int i = 0; i < this.lockArray.size(); i++) {
            if (this.lockArray.get(i).getTransactionId().equals(transactionID) &&
                    this.lockArray.get(i).getVariableID() == variableID &&
                    this.lockArray.get(i).getLockType() == Constants.LockType.READ
            ) {
                this.lockArray.get(i).setLockType(Constants.LockType.WRITE);
            }
        }
    }

    // TODO: remove if unused
    public boolean lockExistsForTransaction(String transactionID) {
        for (int i = 0; i < this.lockArray.size(); i++) {
            if (this.lockArray.get(i).getTransactionId().equals(transactionID)) {
                return true;
            }
        }
        return false;
    }

    // TODO: remove if unused
    public boolean lockExistsOnVariable(int variableID) {
        for (int i = 0; i < this.lockArray.size(); i++) {
            if (this.lockArray.get(i).getVariableID() == variableID) {
                return true;
            }
        }
        return false;
    }

    public boolean lockExists(int variableID, String transactionID, Constants.LockType type) {
        for (int i = 0; i < this.lockArray.size(); i++) {
            if (this.lockArray.get(i).getVariableID() == variableID
                    && this.lockArray.get(i).getTransactionId().equals(transactionID)
                    && this.lockArray.get(i).getLockType() == type) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Lock> getLocks(int variableID) {
        return new ArrayList<>(
                this.lockArray.stream()
                .filter(lock -> lock.getVariableID() == variableID)
                .collect(Collectors.toList())
        );
    }

    public void releaseLock(int variableID, String transactionID, Constants.LockType lockType) {
        int index = -1;
        for (int i = 0; i < this.lockArray.size(); i++) {
            if (this.lockArray.get(i).getVariableID() == variableID
                    && this.lockArray.get(i).getTransactionId().equals(transactionID)
                    && this.lockArray.get(i).getLockType() == lockType) {
                index = i;
            }
        }
        if (index != -1) {
            this.lockArray.remove(index);
        } else {
        }
    }


    public void releaseLock(String transactionID) {
        int i = 0;
        while (i < this.lockArray.size()) {
            if (this.lockArray.get(i).getTransactionId().equals(transactionID)) {
                this.lockArray.remove(i);
            } else {
                i++;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < this.lockArray.size(); i++) {
            answer.append(this.lockArray.get(i) + "\n");
        }

        return answer.toString();
    }

}
