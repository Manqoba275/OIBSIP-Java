import java.math.BigDecimal;
import java.util.Scanner;

public final class ATM {
    private final Bank bank;
    private final Scanner input;

    public ATM(Bank bank, Scanner input) {
        this.bank = bank;
        this.input = input;
    }

    public void run() {
        System.out.println("=== ATM Interface ===");
        Account account = login();
        if (account == null) return;
        while (true) {
            System.out.println("\nAccount: " + account.getId() + " | Balance: R" + account.getBalance());
            System.out.println("1. Transaction History\n2. Withdraw\n3. Deposit\n4. Transfer\n5. Quit");
            String choice = read("Choose an option: ");
            if (choice == null || choice.equals("5")) {
                System.out.println("Goodbye!");
                return;
            }
            try {
                switch (choice) {
                    case "1" -> showHistory(account);
                    case "2" -> {
                        BigDecimal amount = readAmount();
                        if (amount == null) return;
                        bank.withdraw(account, amount);
                        System.out.println("Withdrawal successful.");
                    }
                    case "3" -> {
                        BigDecimal amount = readAmount();
                        if (amount == null) return;
                        bank.deposit(account, amount);
                        System.out.println("Deposit successful.");
                    }
                    case "4" -> {
                        String recipient = read("Recipient account ID: ");
                        if (recipient == null) return;
                        BigDecimal amount = readAmount();
                        if (amount == null) return;
                        bank.transfer(account, recipient, amount);
                        System.out.println("Transfer successful.");
                    }
                    default -> System.out.println("Please choose an option from 1 to 5.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private Account login() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String id = read("User ID: ");
            if (id == null) return null;
            String pin = read("PIN: ");
            if (pin == null) return null;
            Account account = bank.authenticate(id, pin);
            if (account != null) {
                System.out.println("Login successful.");
                return account;
            }
            System.out.println("Invalid User ID or PIN. Attempts remaining: " + (3 - attempt));
        }
        System.out.println("Access denied after 3 incorrect attempts.");
        return null;
    }

    private void showHistory(Account account) {
        if (account.getTransactions().isEmpty()) {
            System.out.println("No transactions in this session.");
        } else {
            account.getTransactions().forEach(System.out::println);
        }
    }

    private BigDecimal readAmount() {
        String value = read("Amount (R): ");
        if (value == null) return null;
        if (!value.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Enter a positive amount with up to two decimal places.");
        }
        return new BigDecimal(value);
    }

    private String read(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            System.out.println("\nInput closed. Goodbye!");
            return null;
        }
        return input.nextLine().trim();
    }
}
