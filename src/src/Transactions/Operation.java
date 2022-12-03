package Transactions;

public class Operation {
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
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

    public String command;
    public String transactionId;
    public String variableId;
    public int value;
}
