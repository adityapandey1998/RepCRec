import Transactions.TransactionManagerOld;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String fileName = args[0];
        String filePath = new File(fileName).getAbsolutePath();

        TransactionManagerOld transactionManagerOld = new TransactionManagerOld();
        try {
            transactionManagerOld.processInputFile(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}