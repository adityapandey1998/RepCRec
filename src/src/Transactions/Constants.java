package Transactions;

public class Constants {

  public final static int NUM_SITES = 10;
  public final static int NUM_VARIABLES = 20;

  public enum TransactionType {
    RW, RO
  }

  ;

  public enum OperationType {
    READ, WRITE, COMMIT
  }

  ;

  public enum LockType {
    UNLOCK, READ, WRITE
  }

  ;
}
