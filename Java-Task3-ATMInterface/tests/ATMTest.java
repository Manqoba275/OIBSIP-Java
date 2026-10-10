import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ATMTest {
    private static int checks;
    private static String session(String input) {
        Bank bank = new Bank();
        bank.addAccount(new Account("1001", "1234", new BigDecimal("1000.00")));
        bank.addAccount(new Account("1002", "5678", new BigDecimal("500.00")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (Scanner scanner = new Scanner(input);
             PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            new ATM(bank, scanner).run();
        } finally { System.setOut(original); }
        return output.toString(StandardCharsets.UTF_8);
    }
    private static void contains(String output, String expected) {
        checks++;
        if (!output.contains(expected)) throw new AssertionError("Missing: " + expected + "\n" + output);
    }
    public static void main(String[] args) {
        String result = session("1001\n1234\n1\n3\n100.50\n2\n20\n4\n1002\n50\n1\n5\n");
        for (String expected : new String[]{"No transactions", "Deposit successful", "Withdrawal successful",
                "Transfer successful", "R1030.50", "DEPOSIT", "WITHDRAWAL", "TRANSFER OUT", "Goodbye!"}) {
            contains(result, expected);
        }
        result = session("1001\n0000\n1001\n0000\n1001\n0000\n1001\n1234\n");
        contains(result, "Access denied after 3 incorrect attempts.");
        if (result.contains("Login successful")) throw new AssertionError("Lockout bypass");
        checks++;
        result = session("1001\n1234\n9\n3\nabc\n3\n-1\n3\n0\n3\n1.001\n2\n1001\n4\nmissing\n1\n4\n1001\n1\n5\n");
        for (String expected : new String[]{"choose an option from 1 to 5", "positive amount",
                "greater than zero", "Insufficient Funds", "Recipient account does not exist",
                "Cannot transfer to the same account", "Balance: R1000.00", "Goodbye!"}) {
            contains(result, expected);
        }
        for (String input : new String[]{"", "1001\n", "1001\n1234\n", "1001\n1234\n3\n", "1001\n1234\n4\n"}) {
            contains(session(input), "Input closed. Goodbye!");
        }
        System.out.println("ATM interaction checks passed: " + checks);
    }
}
