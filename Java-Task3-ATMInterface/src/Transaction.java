import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Transaction(String type, BigDecimal amount, String otherAccount,
                          BigDecimal balanceAfter, LocalDateTime occurredAt) {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String toString() {
        String counterparty = otherAccount == null ? "" : " | Account: " + otherAccount;
        return occurredAt.format(DISPLAY_TIME)
                + " | " + type + " | R" + amount.toPlainString() + counterparty
                + " | Balance: R" + balanceAfter.toPlainString();
    }
}
