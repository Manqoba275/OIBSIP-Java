import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public final class DBConnection {
    private final String url;
    public DBConnection(Path database) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path absolute = database.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        url = "jdbc:sqlite:" + absolute;
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, salt BLOB NOT NULL, password_hash BLOB NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS bookings (pnr TEXT PRIMARY KEY, username TEXT NOT NULL REFERENCES users(username), passenger TEXT NOT NULL, train_number TEXT NOT NULL, train_name TEXT NOT NULL, travel_class TEXT NOT NULL, journey_date TEXT NOT NULL, source TEXT NOT NULL, destination TEXT NOT NULL)");
        }
    }
    public Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException ex) { c.close(); throw ex; }
        return c;
    }
}
