public class CommitValue {
    private int value;
    private int commitTimestamp;

    public CommitValue(int value, int commitTimestamp) {
        this.value = value;
        this.commitTimestamp = commitTimestamp;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getCommitTimestamp() {
        return commitTimestamp;
    }

    public void setCommitTimestamp(int commitTimestamp) {
        this.commitTimestamp = commitTimestamp;
    }
}