package Transactions;
public class Transaction {

    public String transactionId;
    public Boolean isLive;

    private Constants.TransactionType transactionType;

    private int startTime;

    public Transaction(String transactionId, Constants.TransactionType transactionType, int startTime) {
        this.transactionId = transactionId;
        this.isLive = true;
        this.transactionType = transactionType;
        this.startTime = startTime;
    }

}
