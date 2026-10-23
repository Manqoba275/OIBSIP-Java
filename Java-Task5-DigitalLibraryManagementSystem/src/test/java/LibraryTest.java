import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public final class LibraryTest {
    private static int checks;
    interface Work { void run() throws Exception; }
    private static void check(boolean value,String message) {checks++;if(!value)throw new AssertionError(message);}
    private static void rejects(Class<? extends Exception> type,Work work) throws Exception {
        checks++;try{work.run();throw new AssertionError("Expected "+type.getSimpleName());}catch(Exception ex){if(!type.isInstance(ex))throw ex;}
    }
    static final class MutableClock extends Clock {
        Instant now=Instant.parse("2026-01-01T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}
        public Instant instant(){return now;}
        void days(int n){now=now.plus(Duration.ofDays(n));}
    }
    public static void main(String[] args) throws Exception {
        Path file=Files.createTempDirectory("library-service-test").resolve("test.db");
        MutableClock clock=new MutableClock();
        try {
            Library lib=new Library(new Database(file),clock);lib.seedDemo();lib.seedDemo();
            long admin=lib.login("admin@library.test","Admin123!").id();
            long reader=lib.login("READER@library.test","Reader123!").id();
            check(lib.books("","").size()==6,"Demo should seed exactly once");
            check(lib.members(admin).size()==1,"Only reader listed as member");
            rejects(IllegalArgumentException.class,()->lib.login("reader@library.test","wrong"));
            rejects(IllegalArgumentException.class,()->lib.login("nobody@library.test","wrong"));
            rejects(IllegalArgumentException.class,()->lib.register("Duplicate","READER@library.test","Reader123!"));
            rejects(IllegalArgumentException.class,()->lib.register("Name","bad","Reader123!"));
            rejects(IllegalArgumentException.class,()->lib.register("Name","ok@test.com","short"));
            rejects(IllegalArgumentException.class,()->lib.register(" ","ok@test.com","Reader123!"));
            long second=lib.register("Second Reader","second@library.test","Second123!");
            long third=lib.register("Third Reader","third@library.test","Third123!");
            check(lib.user(second).role().equals("MEMBER"),"Registration must not grant admin");
            rejects(SecurityException.class,()->lib.members(reader));
            rejects(SecurityException.class,()->lib.messages(reader));
            rejects(SecurityException.class,()->lib.loans(reader,true));
            check(lib.books("austen","").size()==1,"Author search");
            check(lib.books("","Technology").size()==2,"Category filter");
            check(lib.books("' OR 1=1 --","").isEmpty(),"Search must be literal");
            check(Library.validIsbn("0306406152"),"ISBN10");
            check(!Library.validIsbn("9780132350885"),"Bad check digit");
            rejects(SecurityException.class,()->lib.saveBook(reader,0,"Title","Author","9780306406157","Science",1));
            rejects(IllegalArgumentException.class,()->lib.saveBook(admin,0,"Title","Author","9780306406158","Science",1));
            lib.saveBook(admin,0,"Test Book","Test Author","9780306406157","Science",1);
            long book=lib.books("Test Book","").get(0).id();
            rejects(IllegalArgumentException.class,()->lib.saveBook(admin,0,"Duplicate","Author","9780306406157","Science",1));
            rejects(IllegalArgumentException.class,()->lib.reserve(reader,book));
            lib.borrow(reader,book);
            check(lib.books("Test Book","").get(0).available()==0,"Borrow decrements stock");
            Library.Loan loan=lib.loans(reader,false).get(0);
            check(loan.due().equals("2026-01-15"),"Fourteen-day due date");
            rejects(IllegalArgumentException.class,()->lib.borrow(reader,book));
            rejects(IllegalArgumentException.class,()->lib.borrow(second,book));
            rejects(IllegalArgumentException.class,()->lib.reserve(reader,book));
            rejects(IllegalArgumentException.class,()->lib.deleteBook(admin,book));
            rejects(IllegalArgumentException.class,()->lib.setActive(admin,reader,false));
            rejects(IllegalArgumentException.class,()->lib.returnBook(second,loan.id()));
            lib.reserve(second,book);lib.reserve(third,book);
            check(lib.reservations(third).get(0).position()==2,"FIFO queue position");
            rejects(IllegalArgumentException.class,()->lib.reserve(second,book));
            rejects(IllegalArgumentException.class,()->lib.cancelReservation(reader,lib.reservations(second).get(0).id()));
            clock.days(14);check(lib.loans(reader,false).get(0).fine()==0,"No fine on due date");
            clock.days(3);check(lib.loans(reader,false).get(0).fine()==15,"Three overdue days");
            rejects(IllegalArgumentException.class,()->lib.payFine(admin,loan.id()));
            lib.returnBook(reader,loan.id());clock.days(5);
            check(lib.loans(reader,false).get(0).fine()==15,"Fine stops growing at return");
            check(lib.books("Test Book","").get(0).available()==1,"Return restores stock");
            rejects(IllegalArgumentException.class,()->lib.returnBook(reader,loan.id()));
            rejects(IllegalArgumentException.class,()->lib.borrow(third,book));
            rejects(IllegalArgumentException.class,()->lib.borrow(reader,book));
            lib.borrow(second,book);check(lib.reservations(second).isEmpty(),"Borrow consumes reservation");
            check(lib.reservations(third).get(0).position()==1,"Queue advances");
            lib.cancelReservation(third,lib.reservations(third).get(0).id());
            check(lib.reservations(third).isEmpty(),"Cancellation removes queue entry");
            rejects(SecurityException.class,()->lib.payFine(reader,loan.id()));
            rejects(IllegalArgumentException.class,()->lib.setActive(admin,reader,false));
            lib.payFine(admin,loan.id());check(lib.loans(reader,false).get(0).paid(),"Admin records payment");
            rejects(IllegalArgumentException.class,()->lib.payFine(admin,loan.id()));
            lib.setActive(admin,reader,false);
            rejects(IllegalArgumentException.class,()->lib.login("reader@library.test","Reader123!"));
            rejects(SecurityException.class,()->lib.borrow(reader,book));
            lib.setActive(admin,reader,true);check(lib.login("reader@library.test","Reader123!").active(),"Re-enable member");
            rejects(IllegalArgumentException.class,()->lib.setActive(admin,admin,false));
            lib.contact(reader,"Help","Can I suggest a book?");check(lib.messages(admin).get(0).body().equals("Can I suggest a book?"),"Persisted contact message");
            rejects(IllegalArgumentException.class,()->lib.contact(reader,"Help"," "));
            lib.returnBook(second,lib.loans(second,false).get(0).id());
            lib.saveBook(admin,book,"Updated Test Book","Test Author","9780306406157","Science",1);
            check(lib.books("Updated Test","").size()==1,"Book edit");
            // Concurrent requests for one copy must produce exactly one successful loan.
            ExecutorService pool=Executors.newFixedThreadPool(2);
            try {
                List<Future<Boolean>> results=pool.invokeAll(List.of(()->{try{lib.borrow(reader,book);return true;}catch(IllegalArgumentException ex){return false;}},()->{try{lib.borrow(third,book);return true;}catch(IllegalArgumentException ex){return false;}}));
                int successes=0;for(Future<Boolean> result:results)if(result.get())successes++;
                check(successes==1,"One last-copy winner");
                check(lib.books("Updated Test","").get(0).available()==0,"Stock never negative");
            } finally {pool.shutdownNow();}
            for(var l:lib.loans(admin,true))if(l.returned()==null)lib.returnBook(l.userId(),l.id());
            lib.deleteBook(admin,book);check(lib.books("Updated Test","").isEmpty(),"Deleted book hidden");
            check(!lib.loans(reader,false).isEmpty(),"Archive retains loan history");
            rejects(IllegalArgumentException.class,()->lib.borrow(reader,book));
            Library reopened=new Library(new Database(file),clock);
            check(reopened.messages(admin).size()==1,"Messages survive restart");
            check(reopened.loans(reader,false).get(0).returned()!=null,"Loans survive restart");
            System.out.println("LibraryTest: "+checks+" checks passed.");
        } finally {Files.deleteIfExists(file);Files.deleteIfExists(file.getParent());}
    }
}
