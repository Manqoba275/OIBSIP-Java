import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Transaction(String type, BigDecimal amount, String otherAccount,
                          BigDecimal balanceAfter, LocalDateTime occurredAt) {
    @Override
    public String toString() {
        String counterparty = otherAccount == null ? "" : " | Account: " + otherAccount;
        return occurredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " | " + type + " | R" + amount.toPlainString() + counterparty
                + " | Balance: R" + balanceAfter.toPlainString();
    }
}
