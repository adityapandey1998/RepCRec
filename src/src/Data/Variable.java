package Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Variable Class holds the values of different versions of the dame variable.
 */
public class Variable {

  public String variableId;
  public boolean isReadable;
  public ProposedValue proposedValue;
  public boolean isReplicated;

  public List<CommitValue> committedValues;

  public Variable(String variableId, CommitValue initialValue, boolean isReplicated) {
    this.variableId = variableId;
    this.isReadable = true;
    this.proposedValue = null;
    this.isReplicated = isReplicated;
    this.committedValues = new ArrayList<>(List.of(initialValue));
  }

  /**
   * @return Gets the latest committed value
   */
  public CommitValue getMostRecentlyCommittedValue() {
    return this.committedValues.get(0);
  }

  public ProposedValue getTempValue() {
    return this.proposedValue;
  }

  /**
   * Adds the latest value to the front of the list
   */
  public void addCommitValue(CommitValue commitValue) {
    this.committedValues.add(0, commitValue);
  }
}
