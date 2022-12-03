package OperationHandlers;

import Sites.Site;
import Transactions.Transaction;

import java.util.*;

public class endHandler {
    public static void endTransaction(Transaction transaction, List<Site> siteList) {
        if (transaction.isLive) {
            for (Site site : siteList) {
                Map<String, Set<Cache>> dataCache = site.getDatacache();
                Set<Cache> transactionCache = dataCache.getOrDefault(action.getTransaction().getTransactionId(), new HashSet<>());
                for (Cache c : transactionCache) {
                    String variable = c.getVariable();
                    for (int timeOfCache : c.getPair().keySet()) {
                        int valueOfCache = c.getPair().get(timeOfCache);
                        site.getDataMap().get(variable).put(timeOfCache, valueOfCache);
                        System.out.println("Variable " + variable + " is updated on Site " + site.getSiteId() + " with value " + valueOfCache);
                        site.getVariableStaleStateMap().put(variable, false);
                    }
                }
            }
            action.getTransaction().setLive(false);
            cleanUpTransaction(action.getTransaction(), false, false);
            System.out.println(action.getTransaction().getTransactionId() + " commits because it was not aborted.");
        } else {
            cleanUpTransaction(action.getTransaction(), true, true);
        }
    }

    private void cleanUpTransaction(Transaction transaction, boolean isAborted, boolean printReason) {
        if (isAborted) {
            if(printReason)
                System.out.print("This transaction accessed a site which failed and hence ");
            System.out.println(transaction.getTransactionId() + " aborts");
        }
        for (Site s : sites) {
            for (String variable : s.getLockMap().keySet()) {
                List<Lock> locksToRemove = new ArrayList<>();
                for (Lock lock : s.getLockMap().get(variable)) {
                    if (Objects.equals(lock.getTransaction().getTransactionId(), transaction.getTransactionId())) {
                        locksToRemove.add(lock);
                    }
                }
                s.getLockMap().get(variable).removeAll(locksToRemove);
            }
        }
        List<Action> actionsToRemove = new ArrayList<>();
        for (Action action : waitQueue) {
            if (action.getTransaction().getTransactionId().equals(transaction.getTransactionId())) {
                actionsToRemove.add(action);
            }
        }
        waitQueue.removeAll(actionsToRemove);
        transactions.remove(transaction);
    }
}
