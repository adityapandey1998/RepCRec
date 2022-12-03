public class Result {

  boolean success;
  int value;

  public Result(boolean success) {
    this.success = success;
    this.value = 0;
  }

  public Result(boolean success, int value) {
    this.success = success;
    this.value = value;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }
}
