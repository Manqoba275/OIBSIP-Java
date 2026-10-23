import java.nio.file.*;
import java.sql.*;
import java.util.Arrays;
public final class DatabaseTest {
    public static void main(String[] args) throws Exception {
        Path path=Files.createTempDirectory("library-db-test").resolve("test.db");
        try {
            Database db=new Database(path);
            new Database(path); // Reopening must not recreate or discard existing tables.
            try(Connection c=db.open(); Statement s=c.createStatement()) {
                try(ResultSet r=s.executeQuery("PRAGMA foreign_keys")) { if(!r.next() || r.getInt(1)!=1)throw new AssertionError("Foreign keys disabled"); }
                try {s.executeUpdate("INSERT INTO loans(user_id,book_id,issued,due) VALUES(99,99,'2026-01-01','2026-01-15')");throw new AssertionError("Orphan accepted");}catch(SQLException expected){}
                try {s.executeUpdate("INSERT INTO books(title,author,isbn,category,quantity) VALUES('x','y','z','a',-1)");throw new AssertionError("Negative quantity accepted");}catch(SQLException expected){}
            }
            byte[] salt=Passwords.salt(),hash=Passwords.hash("Demo123!".toCharArray(),salt);
            if(!Arrays.equals(hash,Passwords.hash("Demo123!".toCharArray(),salt)))throw new AssertionError("Unstable hash");
            if(Arrays.equals(hash,Passwords.hash("wrong".toCharArray(),salt)))throw new AssertionError("Password collision");
            if(Arrays.equals(salt,Passwords.salt()))throw new AssertionError("Repeated salt");
            System.out.println("DatabaseTest: 6 checks passed.");
        } finally {Files.deleteIfExists(path);Files.deleteIfExists(path.getParent());}
    }
}
