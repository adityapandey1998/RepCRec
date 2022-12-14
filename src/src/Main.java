import Transactions.TransactionManager;
import java.io.File;
import java.io.IOException;

/**
 * This class is the main entry point to process the transactions. The input file needs to be
 * provided as a command line argument.
 *
 * @author Aditya Pandey, Shubham Jha
 */
public class Main {

  /**
   * @param args - Input File for the program
   */
  public static void main(String[] args) {
    String fileName = args[0];
    TransactionManager transactionManager = new TransactionManager();
    try {
      String filePath = new File(fileName).getAbsolutePath();
      transactionManager.processInput(filePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}