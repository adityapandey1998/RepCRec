import Transactions.Constants;

import java.util.ArrayList;
import java.util.List;

public class LockManager {
    String variableId;
    Lock currentLock;
    List<QueuedLock> queue;

    public LockManager(String variableId, Lock currentLock, List<QueuedLock> queue) {
        this.variableId = variableId;
        this.currentLock = currentLock;
        this.queue = queue;
    }

    public void setCurrentLock(Lock currentLock) {
        this.currentLock = currentLock;
    }

    void clear() {
        this.currentLock = null;
        this.queue = new ArrayList<>();
    }

    void promoteCurrentLock(WriteLock writeLock) {
        if(this.currentLock != null &&
                this.currentLock.lockType == Constants.LockType.READ &&
                this.currentLock.transactionIds.size() == 1 &&
                this.currentLock.transactionIds.contains(writeLock.transactionIds)
        ) {
            setCurrentLock(writeLock);
        }
    }

    void shareReadLock(String transactionId) {
        if(currentLock.lockType == Constants.LockType.READ) {
            this.currentLock.transactionIds.add(transactionId);
        }
    }
}
