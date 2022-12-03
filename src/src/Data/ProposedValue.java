package Data;

/**
 * This class represents a variable that hasn't been committed yet.
 * @author Shubham Jha
 */
public class ProposedValue {

  int value;
  String transactionId;

  public ProposedValue(int value, String transactionId) {
    this.value = value;
    this.transactionId = transactionId;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }
}
