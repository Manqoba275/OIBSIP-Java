import java.math.BigDecimal;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        bank.addAccount(new Account("1001", "1234", new BigDecimal("1000.00")));
        bank.addAccount(new Account("1002", "5678", new BigDecimal("500.00")));
        try (Scanner input = new Scanner(System.in)) {
            new ATM(bank, input).run();
        }
    }
}
