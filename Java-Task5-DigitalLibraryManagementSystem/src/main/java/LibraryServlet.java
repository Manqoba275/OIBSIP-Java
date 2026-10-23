import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;

/** Server-rendered pages: all changes require POST, a session token, and service authorization. */
public final class LibraryServlet extends HttpServlet {
    private final Library library;
    private final String css;
    public LibraryServlet(Library library) throws IOException {
        this.library=library;
        css=Files.readString(Path.of("src/main/webapp/style.css"));
    }
    private static String e(Object value) {
        return value==null?"":value.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
    private static String token(HttpServletRequest req) {
        HttpSession s=req.getSession();
        if(s.getAttribute("csrf")==null) s.setAttribute("csrf",UUID.randomUUID().toString());
        return s.getAttribute("csrf").toString();
    }
    private Library.User current(HttpServletRequest req) throws SQLException {
        Object id=req.getSession().getAttribute("user");
        if(id==null) return null;
        try { return library.user((Long)id); }
        catch(SecurityException ex) { req.getSession().invalidate(); return null; }
    }
    private static String form(HttpServletRequest req,String action) {
        return "<form method='post' action='/"+action+"'><input type='hidden' name='csrf' value='"+token(req)+"'>";
    }
    private static String hidden(String name,Object value) { return "<input type='hidden' name='"+name+"' value='"+e(value)+"'>"; }
    private static String field(String label,String name,String type,Object value,int max) {
        return "<label>"+e(label)+"<input required name='"+name+"' type='"+type+"' maxlength='"+max+"' value='"+e(value)+"'></label>";
    }
    private static String action(HttpServletRequest req,String route,long id,String label) {
        return form(req,route)+hidden("id",id)+"<button class='small'>"+e(label)+"</button></form>";
    }
    private static String table(String heads,String rows) {
        StringBuilder h=new StringBuilder("<div class='table-wrap'><table><thead><tr>");
        for(String head:heads.split("\\|")) h.append("<th scope='col'>").append(head).append("</th>");
        return h+"</tr></thead><tbody>"+rows+"</tbody></table></div>";
    }
    private static String cell(Object value) { return "<td>"+e(value)+"</td>"; }
    private static String empty(String message) { return "<p class='empty'>"+e(message)+"</p>"; }
    private void page(HttpServletRequest req,HttpServletResponse res,Library.User user,String title,String content) throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        String nav="<a href='/'>Catalogue</a>";
        if(user==null) nav+="<a href='/login'>Sign in</a><a href='/register'>Join the library</a>";
        else {
            nav+="<a href='/loans'>My shelf</a><a href='/contact'>Contact</a>";
            if(user.role().equals("ADMIN")) nav+="<a href='/admin'>Administration</a>";
            nav+=form(req,"logout")+"<button class='nav-button'>Sign out</button></form>";
        }
        Object flash=req.getSession().getAttribute("flash");req.getSession().removeAttribute("flash");
        res.getWriter().print("<!doctype html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'><title>"+e(title)+" · The Reading Room</title><link rel='stylesheet' href='/style.css'></head><body><a class='skip' href='#main'>Skip to content</a><header><a class='brand' href='/'><span class='mark'>R</span><span>The Reading Room<small>DIGITAL LIBRARY</small></span></a><nav aria-label='Main navigation'>"+nav+"</nav></header><main id='main'>"+(flash==null?"":"<p class='notice' role='status'>"+e(flash)+"</p>")+content+"</main><footer><span>The Reading Room · OASIS INFOBYTE / Task 5</span><span>14-day loans · ₹5 per overdue day</span></footer></body></html>");
    }
    @Override protected void service(HttpServletRequest req,HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8");
        res.setHeader("Content-Security-Policy","default-src 'none'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
        res.setHeader("X-Content-Type-Options","nosniff");res.setHeader("Cache-Control","no-store");
        res.setHeader("Referrer-Policy","same-origin");
        try {
            if(req.getMethod().equals("GET")) get(req,res);
            else if(req.getMethod().equals("POST")) post(req,res);
            else res.sendError(405);
        } catch(SecurityException ex) {
            res.setStatus(403);page(req,res,null,"Access denied","<h1>Access denied</h1><p>"+e(ex.getMessage())+"</p><a class='button' href='/login'>Sign in</a>");
        } catch(IllegalArgumentException ex) {
            res.setStatus(400);page(req,res,currentSafely(req),"Check your request","<h1>Check your request</h1><p class='notice error' role='alert'>"+e(ex.getMessage())+"</p><p>Use your browser’s Back button to review the form, or return to the catalogue.</p><a class='button' href='/'>Catalogue</a>");
        } catch(SQLException ex) {
            log("Database operation failed",ex);res.setStatus(500);page(req,res,null,"Please try again","<h1>Please try again</h1><p>The library could not save or load your request. No success has been reported. Return to the catalogue and try again.</p><a href='/'>Catalogue</a>");
        }
    }
    private Library.User currentSafely(HttpServletRequest req) { try { return current(req); } catch(SQLException ex) { return null; } }
    private void get(HttpServletRequest req,HttpServletResponse res) throws SQLException,IOException {
        String path=req.getRequestURI();
        if(path.equals("/style.css")) { res.setContentType("text/css;charset=UTF-8");res.getWriter().print(css);return; }
        Library.User user=current(req);
        String content;
        switch(path) {
            case "/": content=catalogue(req,user);break;
            case "/login": content="<section class='auth'><p class='eyebrow'>WELCOME BACK</p><h1>Your next chapter<br>starts here.</h1><p>Sign in to borrow books and manage your reading.</p>"+form(req,"login")+field("Email","email","email","",160)+field("Password","password","password","",128)+"<button>Sign in</button></form><p>New reader? <a href='/register'>Create an account</a></p><details><summary>Local demo accounts</summary><p>Reader: reader@library.test / Reader123!<br>Admin: admin@library.test / Admin123!</p></details></section>";break;
            case "/register": content="<section class='auth'><p class='eyebrow'>BECOME A MEMBER</p><h1>Make room<br>for a good book.</h1>"+form(req,"register")+field("Full name","name","text","",80)+field("Email","email","email","",160)+field("Password (8–128 characters)","password","password","",128)+"<button>Create account</button></form></section>";break;
            case "/loans": needUser(user);content=shelf(req,user);break;
            case "/contact": needUser(user);content="<section class='auth'><p class='eyebrow'>ASK THE LIBRARY</p><h1>How can we help?</h1><p>Your message will be visible to the library administrator.</p>"+form(req,"contact")+field("Subject","subject","text","",120)+"<label>Message<textarea name='body' rows='6' maxlength='2000' required></textarea></label><button>Send message</button></form></section>";break;
            case "/admin": needAdmin(user);content=admin(req,user);break;
            case "/book": needAdmin(user);content=bookForm(req);break;
            case "/delete": needAdmin(user);long id=id(req);Library.Book b=library.books("","").stream().filter(x->x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Book not found."));content="<section class='auth'><h1>Delete this book?</h1><p>"+e(b.title())+" will be removed from the catalogue. Loan history is retained. All copies must be returned first.</p>"+action(req,"delete",id,"Delete book")+"<a href='/admin'>Keep book</a></section>";break;
            default:res.setStatus(404);content="<h1>Page not found</h1><a href='/'>Return to the catalogue</a>";
        }
        page(req,res,user,"Digital Library",content);
    }
    private static void needUser(Library.User user) { if(user==null) throw new SecurityException("Please sign in first."); }
    private static void needAdmin(Library.User user) { needUser(user);if(!user.role().equals("ADMIN")) throw new SecurityException("This page is for administrators."); }
    private static long id(HttpServletRequest req) { try{return Long.parseLong(req.getParameter("id"));}catch(Exception ex){throw new IllegalArgumentException("A valid record ID is required.");} }
    private void post(HttpServletRequest req,HttpServletResponse res) throws SQLException,IOException {
        String supplied=req.getParameter("csrf");
        if(supplied==null || !MessageDigest.isEqual(token(req).getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8))) throw new SecurityException("This form has expired. Reload the page and try again.");
        String path=req.getRequestURI(),destination="/",message="Saved successfully.";
        Library.User user=current(req);
        if(path.equals("/login")) {
            Library.User signed=library.login(req.getParameter("email"),req.getParameter("password"));
            req.getSession().invalidate();req.getSession().setAttribute("user",signed.id());message="Welcome back, "+signed.name()+".";
        } else if(path.equals("/register")) {
            long registered=library.register(req.getParameter("name"),req.getParameter("email"),req.getParameter("password"));
            req.getSession().invalidate();req.getSession().setAttribute("user",registered);message="Your membership is ready. Find your next book below.";
        } else {
            needUser(user);
            switch(path) {
                case "/logout":req.getSession().invalidate();message="You have signed out.";break;
                case "/borrow":library.borrow(user.id(),id(req));message="Book issued for 14 days. Your due date is on My shelf.";destination="/loans";break;
                case "/reserve":library.reserve(user.id(),id(req));message="You joined the reservation queue.";destination="/loans";break;
                case "/return":library.returnBook(user.id(),id(req));message="Book returned. Any overdue fine is shown below.";destination="/loans";break;
                case "/cancel":library.cancelReservation(user.id(),id(req));message="Reservation cancelled.";destination="/loans";break;
                case "/contact":library.contact(user.id(),req.getParameter("subject"),req.getParameter("body"));message="Message sent to the administrator.";destination="/contact";break;
                case "/save-book":needAdmin(user);int quantity;try{quantity=Integer.parseInt(req.getParameter("quantity"));}catch(Exception ex){throw new IllegalArgumentException("Quantity must be a whole number.");}library.saveBook(user.id(),id(req),req.getParameter("title"),req.getParameter("author"),req.getParameter("isbn"),req.getParameter("category"),quantity);destination="/admin";message="Catalogue updated.";break;
                case "/delete":library.deleteBook(user.id(),id(req));destination="/admin";message="Book removed from the catalogue.";break;
                case "/member":library.setActive(user.id(),id(req),"1".equals(req.getParameter("active")));destination="/admin";message="Member access updated.";break;
                case "/pay":library.payFine(user.id(),id(req));destination="/admin";message="Fine recorded as paid.";break;
                default:res.sendError(404);return;
            }
        }
        req.getSession().setAttribute("flash",message);res.setStatus(303);res.setHeader("Location",destination);
    }
    private String catalogue(HttpServletRequest req,Library.User user) throws SQLException {
        String q=Objects.toString(req.getParameter("q"),""),category=Objects.toString(req.getParameter("category"),"");
        List<Library.Book> all=library.books("","");List<Library.Book> books=library.books(q,category);
        StringBuilder s=new StringBuilder("<section class='hero'><div><p class='eyebrow'>A LITTLE CURIOSITY GOES A LONG WAY</p><h1>Find your<br>next chapter.</h1><p>Explore the shelves. Borrow a favourite.<br>Make a little time for something new.</p></div><aside class='hero-card'><span class='big-number'>"+all.size()+"</span><span>titles on our shelves</span><hr><strong>Borrow. Read. Return.</strong><p>Keep each book for 14 days.<br>Reserve a title when copies are taken.</p></aside></section><section><div class='section-heading'><h2>Explore the collection</h2><span>"+books.size()+" titles found</span></div><form class='search' method='get' action='/'><label>Search title or author<input name='q' maxlength='100' placeholder='A title, an author, a new idea…' value='"+e(q)+"'></label><label>Category<select name='category'><option value=''>All categories</option>");
        for(String c:new TreeSet<>(all.stream().map(Library.Book::category).toList()))s.append("<option ").append(c.equals(category)?"selected ":"").append("value='").append(e(c)).append("'>").append(e(c)).append("</option>");
        s.append("</select></label><button>Search shelves</button><a href='/'>Clear</a></form><div class='books'>");
        int index=0;
        for(Library.Book b:books) {
            s.append("<article class='book'><div class='book-cover tone-").append(index++%4).append("'><span>").append(e(b.category())).append("</span><h3>").append(e(b.title())).append("</h3><p>").append(e(b.author())).append("</p><span class='cover-line'>THE READING ROOM COLLECTION</span></div><div class='book-info'><span class='badge'>").append(b.available()).append(" / ").append(b.quantity()).append(" copies on shelf</span><p class='isbn'>ISBN ").append(e(b.isbn())).append("</p><div class='actions'>");
            if(user==null)s.append("<a class='button small' href='/login'>Sign in to borrow</a>");
            else s.append(action(req,"borrow",b.id(),"Borrow")).append(action(req,"reserve",b.id(),"Reserve"));
            s.append("</div></div></article>");
        }
        s.append("</div>");if(books.isEmpty())s.append(empty("No books match that search. Try another title, author or category."));
        return s+"<p class='hint'>Reservations have priority over walk-in borrowing. Use Reserve only when no unreserved copy is available.</p></section>";
    }
    private String shelf(HttpServletRequest req,Library.User user) throws SQLException {
        List<Library.Loan> loans=library.loans(user.id(),false);
        StringBuilder s=new StringBuilder("<p class='eyebrow'>YOUR READING JOURNEY</p><h1>My shelf</h1><p>Manage your loans, due dates and place in the queue.</p><h2>Loans &amp; returns</h2>");
        s.append(loanTable(req,loans,false));
        s.append("<h2>Reservations</h2><p>A returned copy is held for readers in queue order. Visit the catalogue to borrow it.</p>");
        List<Library.Reservation> reservations=library.reservations(user.id());StringBuilder rows=new StringBuilder();
        for(var r:reservations)rows.append("<tr>").append(cell(r.title())).append(cell("#"+r.position())).append("<td>").append(action(req,"cancel",r.id(),"Cancel reservation")).append("</td></tr>");
        s.append(reservations.isEmpty()?empty("No reservations yet."):table("Book|Queue position|Action",rows.toString()));return s.toString();
    }
    private String loanTable(HttpServletRequest req,List<Library.Loan> loans,boolean admin) {
        if(loans.isEmpty()) return empty("No loans yet. Choose a book from the catalogue to get started.");
        StringBuilder rows=new StringBuilder();
        for(var l:loans) {
            rows.append("<tr>").append(cell(l.title()));if(admin)rows.append(cell(l.member()));
            rows.append(cell(l.due())).append(cell(l.returned()==null?"On loan":"Returned "+l.returned())).append(cell("₹"+l.fine()+(l.paid()?" · paid":l.fine()>0?" · unpaid":""))).append("<td>");
            if(!admin && l.returned()==null)rows.append(action(req,"return",l.id(),"Return book"));
            if(admin && l.returned()!=null && l.fine()>0 && !l.paid())rows.append(action(req,"pay",l.id(),"Mark fine paid"));
            rows.append("</td></tr>");
        }
        return table("Book|"+(admin?"Member|":"")+"Due date|Status|Fine|Action",rows.toString());
    }
    private String admin(HttpServletRequest req,Library.User user) throws SQLException {
        List<Library.Book> books=library.books("","");List<Library.User> members=library.members(user.id());List<Library.Loan> loans=library.loans(user.id(),true);
        StringBuilder s=new StringBuilder("<p class='eyebrow'>LIBRARY DESK</p><h1>Administration</h1><div class='stats'><div><strong>"+books.size()+"</strong>Catalogue titles</div><div><strong>"+members.size()+"</strong>Registered members</div><div><strong>"+loans.stream().filter(l->l.returned()==null).count()+"</strong>Active loans</div></div><div class='section-heading'><h2>Catalogue</h2><a class='button' href='/book'>Add a book</a></div>");
        StringBuilder rows=new StringBuilder();for(var b:books)rows.append("<tr>").append(cell(b.title())).append(cell(b.author())).append(cell(b.available()+" / "+b.quantity())).append("<td><a href='/book?id=").append(b.id()).append("'>Edit</a> · <a href='/delete?id=").append(b.id()).append("'>Delete</a></td></tr>");
        s.append(table("Title|Author|On shelf / total|Actions",rows.toString())).append("<h2>Issued books &amp; fines</h2><p>Fines are ₹5 per overdue day. Record payment only after the book is returned and payment has been received.</p>").append(loanTable(req,loans,true)).append("<h2>Members</h2>");
        rows=new StringBuilder();for(var m:members)rows.append("<tr>").append(cell(m.name())).append(cell(m.email())).append(cell(m.active()?"Active":"Disabled")).append("<td>").append(form(req,"member")).append(hidden("id",m.id())).append(hidden("active",m.active()?0:1)).append("<button class='small'>").append(m.active()?"Disable":"Enable").append("</button></form></td></tr>");
        s.append(table("Name|Email|Access|Action",rows.toString())).append("<h2>Reader messages</h2>");
        List<Library.Message> messages=library.messages(user.id());if(messages.isEmpty())s.append(empty("No messages from readers yet."));
        for(var m:messages)s.append("<article class='message'><p class='eyebrow'>").append(e(m.member())).append(" · ").append(e(m.created())).append("</p><h3>").append(e(m.subject())).append("</h3><p class='preserve'>").append(e(m.body())).append("</p></article>");
        return s.toString();
    }
    private String bookForm(HttpServletRequest req) throws SQLException {
        long id=req.getParameter("id")==null?0:id(req);Library.Book b=null;
        if(id!=0)b=library.books("","").stream().filter(x->x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Book not found."));
        return "<section class='auth'><p class='eyebrow'>CATALOGUE DETAILS</p><h1>"+(b==null?"Add a book":"Edit book")+"</h1>"+form(req,"save-book")+hidden("id",id)+field("Title","title","text",b==null?"":b.title(),160)+field("Author","author","text",b==null?"":b.author(),120)+field("ISBN-10 or ISBN-13","isbn","text",b==null?"":b.isbn(),30)+field("Category","category","text",b==null?"":b.category(),50)+"<label>Total copies<input required type='number' name='quantity' min='1' max='1000' value='"+(b==null?1:b.quantity())+"'></label><button>Save book</button></form><p><a href='/admin'>Back to administration</a></p></section>";
    }
}
