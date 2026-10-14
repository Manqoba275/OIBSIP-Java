import java.nio.file.*;
import java.sql.*;
import java.time.*;

public class ReservationServiceTest {
    private static int checks;
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("Reservation check " + checks); }
    private static void rejects(Action action) throws Exception {
        try { action.run(); } catch (IllegalArgumentException | IllegalStateException expected) { checks++; return; }
        throw new AssertionError("Expected validation failure");
    }
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempDirectory("reservation-service-test").resolve("test.db");
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);
        try {
            DBConnection db = new DBConnection(file); ReservationService s = new ReservationService(db, clock);
            rejects(() -> s.fetch("missing"));
            rejects(() -> s.book("A", "101", "Economy", "2026-10-15", "Johannesburg", "Durban"));
            check(!s.login("traveler", "wrong".toCharArray()));
            check(!s.login("' OR 1=1 --", "Travel123!".toCharArray()));
            check(s.login("traveler", "Travel123!".toCharArray()));
            Booking b = s.book("O'Brien", "101", "Economy", "2026-10-15", "Johannesburg", "Durban");
            check(b.trainName().equals("Highveld Express"));
            check(s.fetch(b.pnr().toLowerCase()).orElseThrow().equals(b));
            Booking other = s.book("Second", "102", "Business", "2026-10-14", "Cape Town", "George");
            check(!other.pnr().equals(b.pnr()));
            rejects(() -> s.book("", "101", "Economy", "2026-10-15", "Johannesburg", "Durban"));
            rejects(() -> s.book("A", "abc", "Economy", "2026-10-15", "Johannesburg", "Durban"));
            rejects(() -> s.book("A", "999", "Economy", "2026-10-15", "Johannesburg", "Durban"));
            rejects(() -> s.book("A", "101", "Invalid", "2026-10-15", "Johannesburg", "Durban"));
            for (String date : new String[]{"", "2026-02-30", "14/10/2026", "2026-10-13"}) {
                rejects(() -> s.book("A", "101", "Economy", date, "Johannesburg", "Durban"));
            }
            rejects(() -> s.book("A", "101", "Economy", "2026-10-15", "Durban", "Johannesburg"));
            rejects(() -> s.book("A", "101", "Economy", "2026-10-15", "Durban", "Durban"));
            rejects(() -> s.book("A", "101", "Economy", "2026-10-15", "Cape Town", "Durban"));
            check(s.fetch("missing").isEmpty()); rejects(() -> s.fetch(""));
            ReservationService reopened = new ReservationService(new DBConnection(file), clock);
            check(reopened.login("traveler", "Travel123!".toCharArray()));
            check(reopened.fetch(b.pnr()).orElseThrow().equals(b));
            try (Connection c = db.open(); PreparedStatement insert = c.prepareStatement("INSERT INTO users VALUES (?,?,?)")) {
                byte[] salt = Passwords.salt(); insert.setString(1, "other"); insert.setBytes(2, salt);
                insert.setBytes(3, Passwords.hash("Other123!".toCharArray(), salt)); insert.executeUpdate();
            }
            check(reopened.login("other", "Other123!".toCharArray()));
            check(reopened.fetch(b.pnr()).isEmpty()); check(!reopened.cancel(b.pnr()));
            check(s.cancel(b.pnr())); check(s.fetch(b.pnr()).isEmpty()); check(!s.cancel(b.pnr()));
            check(s.fetch(other.pnr()).isPresent());
            s.logout(); rejects(() -> s.cancel(other.pnr()));
            check(!s.login("traveler", "wrong".toCharArray())); rejects(() -> s.fetch(other.pnr()));
            try (Connection c = db.open(); Statement query = c.createStatement(); ResultSet r = query.executeQuery("SELECT count(*) FROM bookings")) {
                check(r.next() && r.getInt(1) == 1);
            }
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.getParent()); }
        System.out.println("Reservation service checks passed: " + checks);
    }
}
