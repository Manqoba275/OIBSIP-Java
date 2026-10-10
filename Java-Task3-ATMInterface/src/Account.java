import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class Account {
    private final String id;
    private final String pin;
    private BigDecimal balance;
    private final ArrayList<Transaction> transactions = new ArrayList<>();
    private List<Transaction> historySnapshot = List.of();

    public Account(String id, String pin, BigDecimal openingBalance) {
        if (id == null || id.isBlank() || pin == null || !pin.matches("[0-9]{4}")) {
            throw new IllegalArgumentException("An account ID and four-digit PIN are required.");
        }
        if (openingBalance == null || openingBalance.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        this.id = id;
        this.pin = pin;
        this.balance = openingBalance.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    public String getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public List<Transaction> getTransactions() {
        if (historySnapshot == null) historySnapshot = List.copyOf(transactions);
        return historySnapshot;
    }
    boolean matchesPin(String candidate) { return pin.equals(candidate); }

    void record(Transaction transaction) {
        balance = transaction.balanceAfter();
        transactions.add(transaction);
        historySnapshot = null;
    }
}
