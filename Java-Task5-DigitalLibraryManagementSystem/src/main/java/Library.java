import java.sql.*;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** All authorization and lending rules live here, including calls outside the web UI. */
public final class Library {
    public record User(long id, String name, String email, String role, boolean active) {}
    public record Book(long id, String title, String author, String isbn, String category, int quantity, int available) {}
    public record Loan(long id, long userId, String member, String title, String issued, String due, String returned, long fine, boolean paid) {}
    public record Reservation(long id, String title, long position) {}
    public record Message(String member, String subject, String body, String created) {}
    private final Database db;
    private final Clock clock;
    public Library(Database db, Clock clock) { this.db = db; this.clock = clock; }
    private LocalDate today() { return LocalDate.now(clock); }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
    private static String text(String value, String field, int max) {
        value = value == null ? "" : value.strip();
        require(!value.isEmpty() && value.length() <= max, field + " must contain 1–" + max + " characters.");
        return value;
    }
    private static PreparedStatement statement(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement p = c.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
        return p;
    }
    private static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = statement(c, sql, args)) { return p.executeUpdate(); }
    }
    private static long count(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = statement(c, sql, args); ResultSet r = p.executeQuery()) { return r.next() ? r.getLong(1) : 0; }
    }
    private static User user(ResultSet r) throws SQLException {
        return new User(r.getLong("id"),r.getString("name"),r.getString("email"),r.getString("role"),r.getBoolean("active"));
    }
    private User actor(Connection c, long id, boolean admin) throws SQLException {
        try (PreparedStatement p = statement(c,"SELECT * FROM users WHERE id=?",id); ResultSet r = p.executeQuery()) {
            if (!r.next() || !r.getBoolean("active") || (admin && !r.getString("role").equals("ADMIN")))
                throw new SecurityException("Sign in with an active " + (admin ? "administrator" : "member") + " account.");
            return user(r);
        }
    }
    public User user(long id) throws SQLException { try (Connection c = db.open()) { return actor(c,id,false); } }
    public User login(String email, String password) throws SQLException {
        String address = text(email,"Email",160).toLowerCase(Locale.ROOT);
        require(password != null && password.length() <= 128,"Invalid email or password.");
        try (Connection c = db.open(); PreparedStatement p = statement(c,"SELECT * FROM users WHERE email=?",address); ResultSet r = p.executeQuery()) {
            require(r.next() && r.getBoolean("active"),"Invalid email or password.");
            require(MessageDigest.isEqual(r.getBytes("password"),Passwords.hash(password.toCharArray(),r.getBytes("salt"))),"Invalid email or password.");
            return user(r);
        }
    }
    private long createUser(Connection c, String name, String email, String password, String role) throws SQLException {
        name = text(name,"Name",80); email = text(email,"Email",160).toLowerCase(Locale.ROOT);
        require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),"Enter a valid email address.");
        require(password != null && password.length() >= 8 && password.length() <= 128,"Password must contain 8–128 characters.");
        require(count(c,"SELECT count(*) FROM users WHERE email=?",email)==0,"This email is already registered.");
        byte[] salt = Passwords.salt();
        update(c,"INSERT INTO users(name,email,salt,password,role) VALUES(?,?,?,?,?)",name,email,salt,Passwords.hash(password.toCharArray(),salt),role);
        return count(c,"SELECT last_insert_rowid()");
    }
    public synchronized long register(String name, String email, String password) throws SQLException {
        try (Connection c = db.open()) { return createUser(c,name,email,password,"MEMBER"); }
    }
    /** Demo accounts are created only in an empty database, never reset on restart. */
    public synchronized void seedDemo() throws SQLException {
        try (Connection c = db.open()) {
            if (count(c,"SELECT count(*) FROM users") != 0) return;
            c.setAutoCommit(false);
            try {
                createUser(c,"Library Administrator","admin@library.test","Admin123!","ADMIN");
                createUser(c,"Demo Reader","reader@library.test","Reader123!","MEMBER");
                String[][] titles = {
                    {"The Hobbit","J. R. R. Tolkien","9780261103344","Fiction"},
                    {"Pride and Prejudice","Jane Austen","9780141439518","Classics"},
                    {"Clean Code","Robert C. Martin","9780132350884","Technology"},
                    {"A Brief History of Time","Stephen Hawking","9780553380163","Science"},
                    {"Things Fall Apart","Chinua Achebe","9780385474542","Classics"},
                    {"The Pragmatic Programmer","David Thomas and Andrew Hunt","9780135957059","Technology"}
                };
                for (String[] b : titles) update(c,"INSERT INTO books(title,author,isbn,category,quantity) VALUES(?,?,?,?,?)",b[0],b[1],b[2],b[3],2);
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }
    public List<Book> books(String query, String category) throws SQLException {
        String search = query == null ? "" : query.strip();
        require(search.length() <= 100,"Search must be 100 characters or fewer.");
        List<Book> list = new ArrayList<>();
        try (Connection c = db.open(); PreparedStatement p = statement(c,"""
            SELECT b.*, b.quantity-(SELECT count(*) FROM loans l WHERE l.book_id=b.id AND l.returned IS NULL) available
            FROM books b WHERE b.archived=0 AND (instr(lower(b.title),lower(?))>0 OR instr(lower(b.author),lower(?))>0)
            AND (?='' OR b.category=?) ORDER BY b.title
            """,search,search,category==null?"":category,category==null?"":category); ResultSet r = p.executeQuery()) {
            while(r.next()) list.add(new Book(r.getLong("id"),r.getString("title"),r.getString("author"),r.getString("isbn"),r.getString("category"),r.getInt("quantity"),r.getInt("available")));
        }
        return list;
    }
    public synchronized void saveBook(long admin, long id, String title, String author, String isbn, String category, int quantity) throws SQLException {
        title=text(title,"Title",160); author=text(author,"Author",120); category=text(category,"Category",50);
        isbn=text(isbn,"ISBN",30).replace("-", "").replace(" ", "");
        require(validIsbn(isbn),"Enter a valid ISBN-10 or ISBN-13, including its check digit.");
        require(quantity>=1 && quantity<=1000,"Quantity must be between 1 and 1000.");
        try (Connection c = db.open()) {
            actor(c,admin,true);
            require(count(c,"SELECT count(*) FROM books WHERE isbn=? AND id<>?",isbn,id)==0,"ISBN already exists in the catalogue.");
            require(quantity>=count(c,"SELECT count(*) FROM loans WHERE book_id=? AND returned IS NULL",id),"Quantity cannot be less than copies on loan.");
            if (id==0) update(c,"INSERT INTO books(title,author,isbn,category,quantity) VALUES(?,?,?,?,?)",title,author,isbn,category,quantity);
            else require(update(c,"UPDATE books SET title=?,author=?,isbn=?,category=?,quantity=? WHERE id=? AND archived=0",title,author,isbn,category,quantity,id)==1,"Book not found.");
        }
    }
    public static boolean validIsbn(String s) {
        if (s.matches("[0-9]{13}")) {
            int sum=0; for(int i=0;i<13;i++) sum+=(s.charAt(i)-'0')*(i%2==0?1:3);
            return sum%10==0;
        }
        if(s.matches("[0-9]{9}[0-9Xx]")) {
            int sum=0; for(int i=0;i<10;i++) sum+=(Character.toUpperCase(s.charAt(i))=='X'?10:s.charAt(i)-'0')*(10-i);
            return sum%11==0;
        }
        return false;
    }
    public synchronized void deleteBook(long admin, long id) throws SQLException {
        try (Connection c = db.open()) {
            actor(c,admin,true); c.setAutoCommit(false);
            try {
                require(count(c,"SELECT count(*) FROM loans WHERE book_id=? AND returned IS NULL",id)==0,"Return all copies before deleting this book.");
                // Archive the catalogue entry so historical loans and fines remain readable.
                require(update(c,"UPDATE books SET archived=1 WHERE id=? AND archived=0",id)==1,"Book not found.");
                update(c,"DELETE FROM reservations WHERE book_id=?",id); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    /** A transaction and one writer per service keep availability and queue updates together. */
    public synchronized void borrow(long member, long book) throws SQLException {
        try (Connection c = db.open()) {
            actor(c,member,false); c.setAutoCommit(false);
            try {
                require(count(c,"SELECT count(*) FROM books WHERE id=? AND archived=0",book)==1,"Book not found.");
                require(count(c,"SELECT count(*) FROM loans WHERE user_id=? AND book_id=? AND returned IS NULL",member,book)==0,"You already have this book on loan.");
                long available=count(c,"SELECT quantity-(SELECT count(*) FROM loans WHERE book_id=? AND returned IS NULL) FROM books WHERE id=?",book,book);
                long own=count(c,"SELECT id FROM reservations WHERE user_id=? AND book_id=?",member,book);
                long ahead=count(c,"SELECT count(*) FROM reservations WHERE book_id=? AND (?=0 OR id<?)",book,own,own);
                require(available>ahead,"No copy is available for you yet. Join or wait in the reservation queue.");
                update(c,"INSERT INTO loans(user_id,book_id,issued,due) VALUES(?,?,?,?)",member,book,today().toString(),today().plusDays(14).toString());
                update(c,"DELETE FROM reservations WHERE user_id=? AND book_id=?",member,book); c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    public synchronized void reserve(long member, long book) throws SQLException {
        try(Connection c=db.open()) {
            actor(c,member,false);
            require(count(c,"SELECT count(*) FROM books WHERE id=? AND archived=0",book)==1,"Book not found.");
            require(count(c,"SELECT count(*) FROM loans WHERE user_id=? AND book_id=? AND returned IS NULL",member,book)==0,"You already have this book on loan.");
            long available=count(c,"SELECT quantity-(SELECT count(*) FROM loans WHERE book_id=? AND returned IS NULL) FROM books WHERE id=?",book,book);
            require(available<=count(c,"SELECT count(*) FROM reservations WHERE book_id=?",book),"A copy is available. Borrow it instead.");
            require(count(c,"SELECT count(*) FROM reservations WHERE user_id=? AND book_id=?",member,book)==0,"You already reserved this book.");
            update(c,"INSERT INTO reservations(user_id,book_id) VALUES(?,?)",member,book);
        }
    }
    public synchronized void cancelReservation(long member,long id) throws SQLException {
        try(Connection c=db.open()) { actor(c,member,false); require(update(c,"DELETE FROM reservations WHERE id=? AND user_id=?",id,member)==1,"Reservation not found."); }
    }
    public synchronized void returnBook(long member,long loan) throws SQLException {
        try(Connection c=db.open()) { actor(c,member,false); require(update(c,"UPDATE loans SET returned=? WHERE id=? AND user_id=? AND returned IS NULL",today().toString(),loan,member)==1,"Active loan not found."); }
    }
    public List<Loan> loans(long member, boolean all) throws SQLException {
        List<Loan> list=new ArrayList<>();
        try(Connection c=db.open()) {
            actor(c,member,all);
            try(PreparedStatement p=statement(c,"SELECT l.*,u.name,b.title FROM loans l JOIN users u ON u.id=l.user_id JOIN books b ON b.id=l.book_id WHERE (?=1 OR l.user_id=?) ORDER BY l.id DESC",all?1:0,member); ResultSet r=p.executeQuery()) {
                while(r.next()) {
                    String returned=r.getString("returned"),due=r.getString("due");
                    long fine=5*Math.max(0,ChronoUnit.DAYS.between(LocalDate.parse(due),returned==null?today():LocalDate.parse(returned)));
                    list.add(new Loan(r.getLong("id"),r.getLong("user_id"),r.getString("name"),r.getString("title"),r.getString("issued"),due,returned,fine,r.getBoolean("paid")));
                }
            }
        }
        return list;
    }
    public List<Reservation> reservations(long member) throws SQLException {
        List<Reservation> list=new ArrayList<>();
        try(Connection c=db.open()) {
            actor(c,member,false);
            try(PreparedStatement p=statement(c,"SELECT r.id,b.title,(SELECT count(*) FROM reservations q WHERE q.book_id=r.book_id AND q.id<=r.id) position FROM reservations r JOIN books b ON b.id=r.book_id WHERE r.user_id=? ORDER BY r.id",member); ResultSet r=p.executeQuery()) {
                while(r.next()) list.add(new Reservation(r.getLong(1),r.getString(2),r.getLong(3)));
            }
        }
        return list;
    }
    public synchronized void payFine(long admin,long loan) throws SQLException {
        try(Connection c=db.open()) {
            actor(c,admin,true);
            require(update(c,"UPDATE loans SET paid=1 WHERE id=? AND returned IS NOT NULL AND returned>due AND paid=0",loan)==1,"Only an unpaid fine on a returned book can be marked paid.");
        }
    }
    public List<User> members(long admin) throws SQLException {
        List<User> list=new ArrayList<>();
        try(Connection c=db.open()) {
            actor(c,admin,true);
            try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM users WHERE role='MEMBER' ORDER BY name")) { while(r.next()) list.add(user(r)); }
        }
        return list;
    }
    public synchronized void setActive(long admin,long member,boolean active) throws SQLException {
        try(Connection c=db.open()) {
            actor(c,admin,true); c.setAutoCommit(false);
            try {
                if(!active) require(count(c,"SELECT count(*) FROM loans WHERE user_id=? AND (returned IS NULL OR (returned>due AND paid=0))",member)==0,"Resolve active loans and unpaid fines before disabling a member.");
                require(update(c,"UPDATE users SET active=? WHERE id=? AND role='MEMBER'",active?1:0,member)==1,"Member not found.");
                if(!active) update(c,"DELETE FROM reservations WHERE user_id=?",member);
                c.commit();
            } catch(Exception e) { c.rollback(); throw e; }
        }
    }
    public void contact(long member,String subject,String body) throws SQLException {
        subject=text(subject,"Subject",120); body=text(body,"Message",2000);
        try(Connection c=db.open()) { actor(c,member,false); update(c,"INSERT INTO messages(user_id,subject,body,created) VALUES(?,?,?,?)",member,subject,body,today().toString()); }
    }
    public List<Message> messages(long admin) throws SQLException {
        List<Message> list=new ArrayList<>();
        try(Connection c=db.open()) {
            actor(c,admin,true);
            try(Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT u.name,m.* FROM messages m JOIN users u ON u.id=m.user_id ORDER BY m.id DESC")) { while(r.next()) list.add(new Message(r.getString("name"),r.getString("subject"),r.getString("body"),r.getString("created"))); }
        }
        return list;
    }
}
