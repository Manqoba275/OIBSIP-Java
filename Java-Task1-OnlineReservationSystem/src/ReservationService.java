import java.security.MessageDigest;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ReservationService {
    public static final List<String> CLASSES = List.of("Economy", "Business", "Sleeper");
    private final DBConnection database;
    private final Clock clock;
    private String user;

    public ReservationService(DBConnection database, Clock clock) throws SQLException {
        this.database = database; this.clock = clock;
        // Create the demonstration account once; existing password records are never overwritten.
        try (Connection c = database.open(); PreparedStatement query = c.prepareStatement("SELECT 1 FROM users WHERE username=?")) {
            query.setString(1, "traveler");
            try (ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    byte[] salt = Passwords.salt();
                    char[] password = "Travel123!".toCharArray();
                    byte[] hash;
                    try { hash = Passwords.hash(password, salt); } finally { Arrays.fill(password, '\0'); }
                    try (PreparedStatement insert = c.prepareStatement("INSERT OR IGNORE INTO users VALUES (?,?,?)")) {
                        insert.setString(1, "traveler"); insert.setBytes(2, salt); insert.setBytes(3, hash); insert.executeUpdate();
                    }
                }
            }
        }
    }
    public LocalDate today() { return LocalDate.now(clock); }
    public boolean login(String username, char[] password) throws SQLException {
        user = null;
        if (username == null || password == null) return false;
        try (Connection c = database.open(); PreparedStatement q = c.prepareStatement("SELECT salt,password_hash FROM users WHERE username=?")) {
            q.setString(1, username.trim());
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) return false;
                byte[] candidate = Passwords.hash(password, r.getBytes(1));
                try {
                    if (!MessageDigest.isEqual(candidate, r.getBytes(2))) return false;
                    user = username.trim(); return true;
                } finally { Arrays.fill(candidate, (byte) 0); }
            }
        }
    }
    public void logout() { user = null; }
    private String requireUser() {
        if (user == null) throw new IllegalStateException("Sign in before managing reservations.");
        return user;
    }
    public Booking book(String passenger, String number, String travelClass, String date, String source, String destination) throws SQLException {
        String owner = requireUser();
        if (passenger == null || passenger.isBlank() || passenger.trim().length() > 80) {
            throw new IllegalArgumentException("Enter a passenger name of 1 to 80 characters.");
        }
        if (number == null || !number.matches("[0-9]+")) throw new IllegalArgumentException("Train number must be numeric.");
        Train train = Train.find(number);
        if (!CLASSES.contains(travelClass)) throw new IllegalArgumentException("Choose a valid travel class.");
        LocalDate journey;
        try {
            if (date == null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new DateTimeParseException("", "", 0);
            journey = LocalDate.parse(date);
        } catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter a real date in YYYY-MM-DD format."); }
        if (journey.isBefore(today())) throw new IllegalArgumentException("Journey date cannot be in the past.");
        int from = train.stops().indexOf(source), to = train.stops().indexOf(destination);
        if (from < 0 || to <= from) throw new IllegalArgumentException("Choose stations on this train's route, with destination after source.");
        Booking booking = new Booking(UUID.randomUUID().toString().toUpperCase(Locale.ROOT), passenger.trim(), number,
                train.name(), travelClass, journey, source, destination);
        try (Connection c = database.open(); PreparedStatement insert = c.prepareStatement("INSERT INTO bookings VALUES (?,?,?,?,?,?,?,?,?)")) {
            insert.setString(1, booking.pnr()); insert.setString(2, owner); insert.setString(3, booking.passenger());
            insert.setString(4, number); insert.setString(5, train.name()); insert.setString(6, travelClass);
            insert.setString(7, journey.toString()); insert.setString(8, source); insert.setString(9, destination);
            insert.executeUpdate();
        }
        return booking;
    }
    public Optional<Booking> fetch(String pnr) throws SQLException {
        String owner = requireUser();
        try (Connection c = database.open(); PreparedStatement q = c.prepareStatement("SELECT * FROM bookings WHERE pnr=? AND username=?")) {
            q.setString(1, normalizedPnr(pnr)); q.setString(2, owner);
            try (ResultSet r = q.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new Booking(r.getString("pnr"), r.getString("passenger"), r.getString("train_number"),
                        r.getString("train_name"), r.getString("travel_class"), LocalDate.parse(r.getString("journey_date")),
                        r.getString("source"), r.getString("destination")));
            }
        }
    }
    public boolean cancel(String pnr) throws SQLException {
        String owner = requireUser();
        // Scope every operation to the signed-in account, even when the PNR is known.
        try (Connection c = database.open(); PreparedStatement q = c.prepareStatement("DELETE FROM bookings WHERE pnr=? AND username=?")) {
            q.setString(1, normalizedPnr(pnr)); q.setString(2, owner); return q.executeUpdate() == 1;
        }
    }
    private static String normalizedPnr(String pnr) {
        if (pnr == null || pnr.isBlank()) throw new IllegalArgumentException("Enter a PNR.");
        return pnr.trim().toUpperCase(Locale.ROOT);
    }
}
