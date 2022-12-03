import Transactions.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    int siteId;
    boolean isUp;
    Map<String, Variable> data;
    Map<String, LockManager> lockTable;
    List<Transaction> failedTransactionList;
    List<Transaction> recoveredTransactionList;

    DataManager(int siteId) {
        this.siteId = siteId;
        this.isUp = true;
        this.data = new HashMap<>();
        this.lockTable = new HashMap<>();
        this.failedTransactionList = new ArrayList<>();
        this.recoveredTransactionList = new ArrayList<>();

        for(int varIdx = 1; varIdx <= 20; varIdx++) {
            String varId = "x" + varIdx;
            if(varIdx % 2 == 0) {
                // replicate on all sites
                this.data.put(varId, new Variable(varId,
                                new CommitValue(varIdx * 10, 0), true));
                this.lockTable.put(varId, new LockManager(varId));
            } else if (varIdx % 10 + 1 == this.siteId) {
                this.data.put(varId, new Variable(varId, new CommitValue(varIdx * 10, 0), false));
                this.lockTable.put(varId, new LockManager(varId));
            }
        }
    }
}
