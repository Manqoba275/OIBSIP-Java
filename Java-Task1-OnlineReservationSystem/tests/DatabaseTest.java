import java.nio.file.*;
import java.sql.*;
import java.util.Arrays;

public class DatabaseTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Database check " + checks); }
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempDirectory("reservation-db-test").resolve("test.db");
        DBConnection db = new DBConnection(file);
        try {
            check(Files.exists(file));
            try (Connection c = db.open(); Statement s = c.createStatement()) {
                try (ResultSet r = s.executeQuery("PRAGMA foreign_keys")) { check(r.next() && r.getInt(1) == 1); }
                try (ResultSet r = s.executeQuery("SELECT count(*) FROM bookings")) { check(r.next() && r.getInt(1) == 0); }
            }
            new DBConnection(file);
            check(Files.exists(file));
            byte[] salt = Passwords.salt();
            check(salt.length == 16);
            byte[] hash = Passwords.hash("secret-pass".toCharArray(), salt);
            check(hash.length == 32);
            check(Arrays.equals(hash, Passwords.hash("secret-pass".toCharArray(), salt)));
            check(!Arrays.equals(hash, Passwords.hash("wrong-pass".toCharArray(), salt)));
            check(!Arrays.equals(hash, Passwords.hash("secret-pass".toCharArray(), Passwords.salt())));
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.getParent()); }
        System.out.println("Database and password checks passed: " + checks);
    }
}
