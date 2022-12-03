package Transactions;

import Transactions.Constants.OperationType;

/**
 * Read/Write Operation Class
 *
 * @author Aditya Pandey
 */
public class Operation {

  public OperationType operationType;
  public String transactionId;
  public String variableId;
  public int value;

  /**
   * @param operationType This is a Read Constructor
   * @param transactionId Transaction ID
   * @param variableId    Variable ID
   */
  public Operation(OperationType operationType, String transactionId, String variableId) {
    this.operationType = operationType;
    this.transactionId = transactionId;
    this.variableId = variableId;
  }

  /**
   * @param operationType This is a Write Constructor
   * @param transactionId Transaction ID
   * @param variableId    Variable ID
   * @param value         Value to assign
   */
  public Operation(OperationType operationType, String transactionId, String variableId,
      int value) {
    this.operationType = operationType;
    this.transactionId = transactionId;
    this.variableId = variableId;
    this.value = value;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
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
        "operationType='" + operationType + '\'' +
        ", transactionId='" + transactionId + '\'' +
        ", variableId='" + variableId + '\'' +
        ", value=" + value +
        '}';
  }
}
