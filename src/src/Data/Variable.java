package Data;

import java.util.ArrayList;
import java.util.List;

public class Variable {

  String variableIdx;
  public boolean isReplicated;
  Integer tempValue;
  public boolean isReadable;

  public List<CommitValue> committedValues;

  public Variable(String variableIdx, CommitValue initialValue, boolean isReplicated) {
    this.variableIdx = variableIdx;
    this.committedValues = new ArrayList<>(List.of(initialValue));
    this.tempValue = null;
    this.isReplicated = isReplicated;
    this.isReadable = true;
  }

  public CommitValue getLastCommittedValue() {
    return this.committedValues.get(0);
  }

  public Integer getTempValue() {
    return this.tempValue;
  }

  void addCommitValue(CommitValue commitValue) {
    this.committedValues.add(commitValue);
  }
}
