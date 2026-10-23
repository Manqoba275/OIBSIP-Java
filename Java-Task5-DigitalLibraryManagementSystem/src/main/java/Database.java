import java.nio.file.*;
import java.sql.*;

/** Opens short-lived connections; foreign keys are enabled on every connection. */
public final class Database {
    private final String url;
    public Database(Path file) throws Exception {
        Path absolute = file.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        url = "jdbc:sqlite:" + absolute;
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                  id INTEGER PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                  salt BLOB NOT NULL, password BLOB NOT NULL,
                  role TEXT NOT NULL CHECK(role IN ('ADMIN','MEMBER')),
                  active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)))
                """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS books (
                  id INTEGER PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL,
                  isbn TEXT NOT NULL UNIQUE, category TEXT NOT NULL,
                  quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 1000),
                  archived INTEGER NOT NULL DEFAULT 0 CHECK(archived IN (0,1)))
                """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS loans (
                  id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id),
                  book_id INTEGER NOT NULL REFERENCES books(id), issued TEXT NOT NULL,
                  due TEXT NOT NULL, returned TEXT, paid INTEGER NOT NULL DEFAULT 0 CHECK(paid IN (0,1)))
                """);
            s.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS one_active_loan ON loans(user_id,book_id) WHERE returned IS NULL");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS active_books ON loans(book_id,returned)");
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reservations (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER NOT NULL REFERENCES users(id),
                  book_id INTEGER NOT NULL REFERENCES books(id), UNIQUE(user_id,book_id))
                """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS messages (
                  id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL REFERENCES users(id),
                  subject TEXT NOT NULL, body TEXT NOT NULL, created TEXT NOT NULL)
                """);
        }
    }
    public Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException e) { c.close(); throw e; }
        return c;
    }
}
