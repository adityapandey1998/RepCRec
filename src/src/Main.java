import Transactions.TransactionManager;

import java.io.File;

public class Main {

  public static void main(String[] args) {
    String fileName = args[0];
    String filePath = new File(fileName).getAbsolutePath();

    TransactionManager transactionManager = new TransactionManager();
    try {
      transactionManager.processInput(filePath);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}