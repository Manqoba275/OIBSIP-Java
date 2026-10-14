import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReservationPanelTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("UI check " + checks); }
    private static Component find(Container parent, String name) {
        for (Component c : parent.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container child) { Component result = find(child, name); if (result != null) return result; }
        }
        return null;
    }
    private static void text(ReservationPanel p, String name, String text) { ((javax.swing.text.JTextComponent) find(p, name)).setText(text); }
    private static String value(ReservationPanel p, String name) { return ((javax.swing.text.JTextComponent) find(p, name)).getText(); }
    private static void click(ReservationPanel p, String name) { ((AbstractButton) find(p, name)).doClick(); }
    private static String status(ReservationPanel p) { return ((JLabel) find(p, "status")).getText(); }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container container) layout(container); }
    private static void capture(ReservationPanel p, String name) {
        try {
            p.setSize(1000, 760); layout(p);
            BufferedImage image = new BufferedImage(1000, 760, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); p.printAll(g); g.dispose();
            Files.createDirectories(Path.of("screenshots")); ImageIO.write(image, "png", Path.of("screenshots", name).toFile());
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempDirectory("reservation-ui-test").resolve("test.db");
        try {
            ReservationService service = new ReservationService(new DBConnection(file), Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC));
            SwingUtilities.invokeAndWait(() -> {
                ArrayList<String> notices = new ArrayList<>(); AtomicBoolean confirm = new AtomicBoolean(false);
                ReservationPanel p = new ReservationPanel(service, notices::add, message -> confirm.get());
                capture(p, "login.png");
                text(p, "username", "traveler"); text(p, "password", "wrong"); click(p, "login");
                check(((JLabel) find(p, "loginMessage")).getText().contains("Invalid"));
                text(p, "password", "Travel123!"); click(p, "login");
                check(value(p, "trainName").equals("Highveld Express"));
                text(p, "trainNumber", "102"); check(value(p, "trainName").equals("Coastal Link"));
                text(p, "trainNumber", "101");
                click(p, "book"); check(status(p).contains("passenger"));
                text(p, "passenger", "Sanele Mazibuko"); text(p, "journeyDate", "2026-10-20");
                click(p, "book"); capture(p, "reservation.png");
                check(notices.size() == 1 && notices.get(0).contains("Reservation confirmed"));
                String savedPnr = value(p, "pnr"); check(!savedPnr.isBlank());
                ((JTabbedPane) find(p, "tabs")).setSelectedIndex(1); click(p, "fetch");
                check(value(p, "details").contains("Sanele Mazibuko")); capture(p, "booking.png");
                click(p, "cancel"); check(notices.size() == 1); check(((JButton) find(p, "cancel")).isEnabled());
                text(p, "pnr", "missing"); check(!((JButton) find(p, "cancel")).isEnabled());
                click(p, "fetch"); check(status(p).contains("No booking"));
                text(p, "pnr", savedPnr); click(p, "fetch"); confirm.set(true); click(p, "cancel");
                check(status(p).equals("Reservation cancelled."));
                check(!((JButton) find(p, "cancel")).isEnabled());
                click(p, "fetch"); check(status(p).contains("No booking"));
                capture(p, "cancelled.png"); click(p, "logout");
                check(value(p, "pnr").isEmpty() && value(p, "passenger").isEmpty());
            });
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.getParent()); }
        System.out.println("Swing reservation checks passed: " + checks);
    }
}
