import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Bank {
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public void addAccount(Account account) {
        if (account == null || accounts.putIfAbsent(account.getId(), account) != null) {
            throw new IllegalArgumentException("Account is missing or already exists.");
        }
    }

    public Account authenticate(String id, String pin) {
        Account account = accounts.get(id);
        return account != null && account.matchesPin(pin) ? account : null;
    }

    public void deposit(Account account, BigDecimal amount) {
        requireMember(account);
        amount = validAmount(amount);
        record(account, "DEPOSIT", amount, null, account.getBalance().add(amount));
    }

    public void withdraw(Account account, BigDecimal amount) {
        requireMember(account);
        amount = validAmount(amount);
        requireFunds(account, amount);
        record(account, "WITHDRAWAL", amount, null, account.getBalance().subtract(amount));
    }

    public void transfer(Account sender, String recipientId, BigDecimal amount) {
        requireMember(sender);
        amount = validAmount(amount);
        Account recipient = accounts.get(recipientId);
        if (recipient == null) throw new IllegalArgumentException("Recipient account does not exist.");
        if (sender == recipient) throw new IllegalArgumentException("Cannot transfer to the same account.");
        requireFunds(sender, amount);
        record(sender, "TRANSFER OUT", amount, recipientId, sender.getBalance().subtract(amount));
        record(recipient, "TRANSFER IN", amount, sender.getId(), recipient.getBalance().add(amount));
    }

    private void requireMember(Account account) {
        if (account == null || accounts.get(account.getId()) != account) {
            throw new IllegalArgumentException("Account does not belong to this bank.");
        }
    }

    private static BigDecimal validAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Use no more than two decimal places.");
        }
    }

    private static void requireFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient Funds.");
        }
    }

    private static void record(Account account, String type, BigDecimal amount,
                               String otherAccount, BigDecimal balance) {
        account.record(new Transaction(type, amount, otherAccount, balance, LocalDateTime.now()));
    }
}
