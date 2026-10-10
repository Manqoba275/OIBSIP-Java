import java.math.BigDecimal;
import java.lang.management.ManagementFactory;
import java.util.List;

public class HistoryBenchmark {
    private static volatile long sink;
    public static void main(String[] args) {
        Bank bank = new Bank();
        Account account = new Account("1001", "1234", BigDecimal.ZERO);
        bank.addAccount(account);
        for (int i = 0; i < 2000; i++) bank.deposit(account, BigDecimal.ONE);
        for (int i = 0; i < 20; i++) render(account);
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        long id = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(id);
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) render(account);
        long elapsed = System.nanoTime() - start;
        long allocated = bean.getThreadAllocatedBytes(id) - before;
        System.out.printf("2000 transactions x 100 history renders: %.2f ms; %.2f MiB allocated; checksum=%d%n", elapsed / 1e6, allocated / 1048576.0, sink);
    }
    private static void render(Account account) {
        if (!account.getTransactions().isEmpty()) {
            for (Transaction transaction : account.getTransactions()) sink += transaction.toString().length();
        }
    }
}
