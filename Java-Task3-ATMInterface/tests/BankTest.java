import java.math.BigDecimal;

public class BankTest {
    private static int checks;
    private static BigDecimal money(String value) { return new BigDecimal(value); }
    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { checks++; return; }
        throw new AssertionError("Expected operation to be rejected");
    }
    public static void main(String[] args) {
        Bank bank = new Bank();
        Account a = new Account("1001", "1234", money("100.00"));
        Account b = new Account("1002", "5678", money("50.00"));
        bank.addAccount(a); bank.addAccount(b);
        check(bank.authenticate("1001", "1234") == a, "Valid login");
        check(bank.authenticate("1001", "0000") == null, "Invalid PIN");
        check(bank.authenticate("missing", "1234") == null, "Unknown account");
        bank.deposit(a, money("0.10")); bank.deposit(a, money("0.20"));
        check(a.getBalance().equals(money("100.30")), "Exact decimal arithmetic");
        bank.withdraw(a, money("10.30"));
        check(a.getBalance().equals(money("90.00")), "Withdrawal balance");
        bank.transfer(a, "1002", money("40.00"));
        check(a.getBalance().equals(money("50.00")), "Sender balance");
        check(b.getBalance().equals(money("90.00")), "Recipient balance");
        check(a.getTransactions().size() == 4 && b.getTransactions().size() == 1, "Both histories");
        rejects(() -> bank.withdraw(a, money("50.01")));
        rejects(() -> bank.transfer(a, "1002", money("50.01")));
        rejects(() -> bank.transfer(a, "missing", money("1")));
        rejects(() -> bank.transfer(a, "1001", money("1")));
        rejects(() -> bank.deposit(a, money("0")));
        rejects(() -> bank.withdraw(a, money("-1")));
        rejects(() -> bank.deposit(a, money("0.001")));
        rejects(() -> bank.deposit(a, null));
        rejects(() -> bank.addAccount(new Account("1001", "9999", money("1"))));
        rejects(() -> bank.deposit(new Account("1001", "1234", money("1")), money("1")));
        check(a.getBalance().equals(money("50.00")) && b.getBalance().equals(money("90.00")), "Rejected operations preserve balances");
        check(a.getTransactions().size() == 4 && b.getTransactions().size() == 1, "Rejected operations preserve history");
        try { a.getTransactions().clear(); throw new AssertionError("History is mutable"); }
        catch (UnsupportedOperationException expected) { checks++; }
        bank.withdraw(a, money("50.00"));
        check(a.getBalance().equals(money("0.00")), "Withdraw entire balance");
        System.out.println("Bank checks passed: " + checks);
    }
}
