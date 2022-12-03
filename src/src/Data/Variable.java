package Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Variable Class holds the values of different versions of the dame variable.
 */
public class Variable {

  public String variableId;
  public boolean isReplicated;
  public TempValue tempValue;
  public boolean isReadable;

  public List<CommitValue> committedValues;

  public Variable(String variableId, CommitValue initialValue, boolean isReplicated) {
    this.variableId = variableId;
    this.committedValues = new ArrayList<>(List.of(initialValue));
    this.tempValue = null;
    this.isReplicated = isReplicated;
    this.isReadable = true;
  }

  /**
   * @return Gets the latest committed value
   */
  public CommitValue getLastCommittedValue() {
    return this.committedValues.get(0);
  }

  public TempValue getTempValue() {
    return this.tempValue;
  }

  /**
   * Adds the latest value to the front of the list
   */
  public void addCommitValue(CommitValue commitValue) {
    this.committedValues.add(0, commitValue);
  }
}
