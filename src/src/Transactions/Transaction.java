package Transactions;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction Class
 * @author Aditya Pandey
 */
public class Transaction {

  public String transactionId;
  public Boolean isLive;
  public int startTime;
  public Constants.TransactionType transactionType;
  public List<Integer> sitesAccessed;

  /**
   * @param transactionId   Transaction ID
   * @param transactionType Transaction Type - Read-Only/Read-Write
   * @param startTime       Start Time of the Transaction
   */
  public Transaction(String transactionId, Constants.TransactionType transactionType,
      int startTime) {
    this.transactionId = transactionId;
    this.isLive = true;
    this.transactionType = transactionType;
    this.startTime = startTime;
    this.sitesAccessed = new ArrayList<>();
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public Boolean getLive() {
    return isLive;
  }

  public void setLive(Boolean live) {
    isLive = live;
  }

  public int getStartTime() {
    return startTime;
  }

  public void setStartTime(int startTime) {
    this.startTime = startTime;
  }

  public Constants.TransactionType getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(Constants.TransactionType transactionType) {
    this.transactionType = transactionType;
  }

  public List<Integer> getSitesAccessed() {
    return sitesAccessed;
  }

  public void setSitesAccessed(List<Integer> sitesAccessed) {
    this.sitesAccessed = sitesAccessed;
  }

  @Override
  public String toString() {
    return "Transaction{" +
        "transactionId='" + transactionId + '\'' +
        ", isLive=" + isLive +
        ", startTime=" + startTime +
        ", transactionType=" + transactionType +
        ", sitesAccessed=" + sitesAccessed +
        '}';
  }

}
