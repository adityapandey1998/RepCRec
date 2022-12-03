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
        return !this.lockArray.stream()
                .anyMatch(lock -> lock.getTransactionId().equals(transactionID) &&
                        lock.getVariableID() == variableID &&
                        lock.getLockType()== Constants.LockType.WRITE);
    }

    public void acquireReadLock(int variableID, String transactionID) {
        if (this.canAcquireReadLock(variableID, transactionID)) {
            this.addLock(variableID, transactionID, Constants.LockType.READ);
        }
    }

    public void promoteReadLockToWriteLock(int variableID, String transactionID) {
        for (int i = 0; i < this.lockArray.size(); i++) {
            if (this.lockArray.get(i).getVariableID() == variableID &&
                    this.lockArray.get(i).getTransactionId().equals(transactionID) &&
                    this.lockArray.get(i).getLockType() == Constants.LockType.READ
            ) {
                this.lockArray.get(i).setLockType(Constants.LockType.WRITE);
            }
        }
    }

    public boolean lockExistsForTransaction(String transactionID) {
        return this.lockArray.stream().anyMatch(lock -> lock.getTransactionId().equals(transactionID));
    }

    public boolean lockExistsOnVariable(int variableID) {
        return this.lockArray.stream().anyMatch(lock -> lock.getVariableID() == variableID);
    }

    public boolean lockExists(int variableID, String transactionID, Constants.LockType type) {
        return this.lockArray.stream()
                .anyMatch(lock -> lock.getVariableID() == variableID &&
                        lock.getTransactionId().equals(transactionID) &&
                        lock.getLockType() == type
                );
    }

    public ArrayList<Lock> getLocks(int variableID) {
        return new ArrayList<>(
                this.lockArray.stream()
                .filter(lock -> lock.getVariableID() == variableID)
                .collect(Collectors.toCollection(ArrayList::new))
        );
    }

    public void releaseLock(int variableID, String transactionID, Constants.LockType lockType) {
        this.lockArray = this.lockArray.stream()
                .filter(lock -> (lock.getVariableID() != variableID &&
                                !lock.getTransactionId().equals(transactionID) &&
                                lock.getLockType() != lockType))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void releaseLock(String transactionID) {
        this.lockArray = this.lockArray.stream()
                .filter(lock -> lock.getTransactionId() != transactionID)
                .collect(Collectors.toCollection(ArrayList::new));
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
