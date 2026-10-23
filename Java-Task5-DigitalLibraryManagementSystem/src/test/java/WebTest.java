import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.regex.*;
import org.apache.catalina.startup.Tomcat;

public final class WebTest {
    static int checks;
    static String base;
    static void check(boolean valid,String message){checks++;if(!valid)throw new AssertionError(message);}
    static final class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        String csrf;
        HttpResponse<String> get(String path) throws Exception {
            var r=client.send(HttpRequest.newBuilder(URI.create(base+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
            var m=Pattern.compile("name='csrf' value='([^']+)'").matcher(r.body());if(m.find())csrf=m.group(1);
            return r;
        }
        HttpResponse<String> post(String path,String... pairs) throws Exception {
            Map<String,String> fields=new LinkedHashMap<>();fields.put("csrf",csrf==null?"":csrf);
            for(int i=0;i<pairs.length;i+=2)fields.put(pairs[i],pairs[i+1]);
            StringJoiner body=new StringJoiner("&");fields.forEach((k,v)->body.add(URLEncoder.encode(k,java.nio.charset.StandardCharsets.UTF_8)+"="+URLEncoder.encode(v,java.nio.charset.StandardCharsets.UTF_8)));
            return client.send(HttpRequest.newBuilder(URI.create(base+path)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
        }
        void login(String email,String password) throws Exception {
            get("/login");check(post("/login","email",email,"password",password).statusCode()==303,"Login redirect");get("/");
        }
    }
    public static void main(String[] args) throws Exception {
        Path temp=Files.createTempDirectory("library-http-test");Tomcat server=null;
        try {
            Library library=new Library(new Database(temp.resolve("test.db")),Clock.systemUTC());library.seedDemo();
            server=Main.start(library,temp.resolve("server"),0);base="http://127.0.0.1:"+server.getConnector().getLocalPort();
            Browser guest=new Browser();var home=guest.get("/");
            check(home.statusCode()==200 && home.body().contains("Find your"),"Public catalogue");
            check(home.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"),"CSP");
            check(guest.get("/style.css").statusCode()==200,"Stylesheet");
            check(guest.get("/admin").statusCode()==403,"Anonymous admin denied");
            check(guest.get("/loans").statusCode()==403,"Anonymous loans denied");
            check(guest.post("/borrow","id","1").statusCode()==403,"Missing CSRF denied");
            check(guest.get("/borrow?id=1").statusCode()==404,"GET cannot borrow");
            check(guest.get("/unknown").statusCode()==404,"Unknown route");
            Browser reader=new Browser();reader.login("reader@library.test","Reader123!");
            check(reader.get("/admin").statusCode()==403,"Member admin denied");
            reader.get("/");
            check(reader.post("/delete","id","1").statusCode()==403,"Member cannot delete book");
            check(reader.post("/borrow","id","1","csrf","wrong").statusCode()==403,"Wrong CSRF denied");
            check(reader.post("/borrow","id","1").statusCode()==303,"Borrow via HTTP");
            var shelf=reader.get("/loans");check(shelf.body().contains("On loan") && shelf.body().contains("The Hobbit"),"Loan shown");
            check(reader.post("/borrow","id","1").statusCode()==400,"Duplicate borrow rejected");
            check(reader.post("/contact","subject","<script>alert(1)</script>","body","Text & details").statusCode()==303,"Contact form");
            Browser admin=new Browser();admin.login("admin@library.test","Admin123!");
            var desk=admin.get("/admin");
            check(desk.statusCode()==200,"Admin access");
            check(desk.body().contains("&lt;script&gt;") && !desk.body().contains("<script>"),"Stored text escaped");
            check(admin.post("/member","id","2","active","0").statusCode()==400,"Active loan prevents disabling");
            long loan=library.loans(2,false).get(0).id();
            reader.get("/loans");check(reader.post("/return","id",Long.toString(loan)).statusCode()==303,"Return via HTTP");
            admin.get("/admin");check(admin.post("/member","id","2","active","0").statusCode()==303,"Admin disables member");
            check(reader.get("/loans").statusCode()==403,"Existing session loses access when disabled");
            Browser newcomer=new Browser();newcomer.get("/register");check(newcomer.post("/register","name","New Reader","email","new@test.com","password","New12345!").statusCode()==303,"Registration via HTTP");
            check(newcomer.get("/loans").statusCode()==200,"Registration signs in");
            check(newcomer.post("/logout").statusCode()==303,"Logout");
            check(newcomer.get("/loans").statusCode()==403,"Logout invalidates session");
            System.out.println("WebTest: "+checks+" checks passed.");
        } finally {
            if(server!=null){server.stop();server.destroy();}
            try(var paths=Files.walk(temp)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
        }
    }
}
