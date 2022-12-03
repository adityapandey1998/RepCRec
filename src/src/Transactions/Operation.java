package Transactions;

/**
 * Read/Write Operation
 */
public class Operation {

  public Constants.OperationType command;
  public String transactionId;
  public String variableId;
  public int value;

  /**
   * @param command This is a Read Constructor
   * @param transactionId Transaction ID
   * @param variableId Variable ID
   */
  public Operation(Constants.OperationType command, String transactionId, String variableId) {
    this.command = command;
    this.transactionId = transactionId;
    this.variableId = variableId;
  }

  /**
   * @param command This is a Write Constructor
   * @param transactionId Transaction ID
   * @param variableId Variable ID
   * @param value Value to assign
   */
  public Operation(Constants.OperationType command, String transactionId, String variableId, int value) {
    this.command = command;
    this.transactionId = transactionId;
    this.variableId = variableId;
    this.value = value;
  }

  public Constants.OperationType getCommand() {
    return command;
  }

  public void setCommand(Constants.OperationType command) {
    this.command = command;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getVariableId() {
    return variableId;
  }

  public void setVariableId(String variableId) {
    this.variableId = variableId;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "Operations{" +
        "command='" + command + '\'' +
        ", transactionId='" + transactionId + '\'' +
        ", variableId='" + variableId + '\'' +
        ", value=" + value +
        '}';
  }
}
