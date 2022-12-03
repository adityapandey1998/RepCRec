import java.util.ArrayList;
import java.util.List;

public class Variable {

    String variableIdx;
    boolean isReplicated;
    Integer tempValue;
    boolean isReadable;

    List<Integer> committedValues;

    Variable(String variableIdx, int initialValue, boolean isReplicated) {
        this.variableIdx = variableIdx;
        this.committedValues = new ArrayList<>(List.of(initialValue));
        this.tempValue = null;
        this.isReplicated = isReplicated;
        this.isReadable = true;
    }

    Integer getLastCommittedValue() {
        return this.committedValues.get(0);
    }

    Integer getTempValue() {
        return this.tempValue;
    }

    void addCommitValue(int commitValue) {
        this.committedValues.add(commitValue);
    }
}
