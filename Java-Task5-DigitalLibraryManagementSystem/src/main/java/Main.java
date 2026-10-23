import java.nio.file.*;
import java.time.Clock;
import java.util.Set;
import jakarta.servlet.SessionTrackingMode;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

public final class Main {
    public static Tomcat start(Library library, Path base, int port) throws Exception {
        Files.createDirectories(base);
        Tomcat server=new Tomcat();
        server.setBaseDir(base.toAbsolutePath().toString());
        server.setPort(port);
        // This learning application is deliberately reachable only from this PC.
        server.getConnector().setProperty("address","127.0.0.1");
        server.getConnector().setProperty("maxThreads","12");
        server.getConnector().setProperty("minSpareThreads","2");
        server.getConnector().setMaxPostSize(16*1024);
        Context context=server.addContext("",base.toAbsolutePath().toString());
        context.setParentClassLoader(Main.class.getClassLoader());
        context.setSessionTimeout(30);
        context.setUseHttpOnly(true);
        Rfc6265CookieProcessor cookies=new Rfc6265CookieProcessor();
        cookies.setSameSiteCookies("strict");
        context.setCookieProcessor(cookies);
        context.addServletContainerInitializer((classes, servletContext) ->
            servletContext.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE)),null);
        Tomcat.addServlet(context,"library",new LibraryServlet(library));
        context.addServletMapping("/","library");
        server.start();
        return server;
    }
    public static void main(String[] args) throws Exception {
        int port=Integer.parseInt(System.getProperty("library.port","8080"));
        Database db=new Database(Path.of(System.getProperty("library.db","data/library.db")));
        Library library=new Library(db,Clock.systemDefaultZone());
        library.seedDemo();
        Tomcat server=start(library,Path.of("build/server"),port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { server.stop(); server.destroy(); } catch(Exception ignored) {} }));
        System.out.println("Digital Library is ready: http://127.0.0.1:"+server.getConnector().getLocalPort());
        server.getServer().await();
    }
}
