package Transactions;

/**
 * Constants
 * @author Aditya Pandey, Shubham Jha
 */
public class Constants {

  public final static int NUM_SITES = 10;
  public final static int NUM_VARIABLES = 20;

  /**
   * RW - Read-Write RO - Read-Only
   */
  public enum TransactionType {
    RW, RO
  }

  ;

  public enum OperationType {
    READ, WRITE
  }

  ;

  public enum LockType {
    UNLOCK, READ, WRITE
  }

  ;
}
