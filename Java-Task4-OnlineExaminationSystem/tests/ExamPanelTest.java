import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ExamPanelTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("UI check " + checks); }
    private static Component find(Container parent, String name) {
        for (Component c : parent.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container child) {
                Component found = find(child, name); if (found != null) return found;
            }
        }
        return null;
    }
    private static void click(ExamPanel p, String name) { ((AbstractButton) find(p, name)).doClick(); }
    private static void text(ExamPanel p, String name, String value) { ((javax.swing.text.JTextComponent) find(p, name)).setText(value); }
    private static String label(ExamPanel p, String name) { return ((JLabel) find(p, name)).getText(); }
    private static void layout(Container c) { c.doLayout(); for (Component child : c.getComponents()) if (child instanceof Container next) layout(next); }
    private static void capture(ExamPanel p, String file) {
        try {
            p.setSize(1000, 740); layout(p);
            BufferedImage image = new BufferedImage(1000, 740, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); p.printAll(graphics); graphics.dispose();
            Files.createDirectories(Path.of("screenshots"));
            ImageIO.write(image, "png", Path.of("screenshots", file).toFile());
        } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicLong clock = new AtomicLong(); AtomicBoolean confirm = new AtomicBoolean(false);
            UserAccount user = new UserAccount("student", "Student", "Student123!".toCharArray());
            ExamPanel p = new ExamPanel(user, 1800, clock::get, message -> confirm.get());
            try {
                capture(p, "login.png");
                text(p, "username", "student"); text(p, "password", "bad"); click(p, "login");
                check(label(p, "loginMessage").contains("Invalid"));
                text(p, "password", "Student123!"); click(p, "login");
                text(p, "displayName", "Sanele"); text(p, "newPassword", "Practice123!"); click(p, "saveProfile");
                check(label(p, "profileMessage").equals("Profile saved."));
                capture(p, "profile.png"); click(p, "startExam");
                check(label(p, "timer").contains("30:00"));
                check(!((JButton) find(p, "previous")).isEnabled());
                click(p, "choice1"); click(p, "next"); click(p, "choice0"); click(p, "previous");
                check(((JRadioButton) find(p, "choice1")).isSelected());
                check(label(p, "progress").contains("Answered: 2"));
                capture(p, "exam.png");
                check(!p.canClose()); click(p, "submit");
                check(label(p, "resultTitle").isEmpty());
                clock.set(12_000_000_000L); confirm.set(true); click(p, "submit");
                check(label(p, "resultTitle").contains("Score: 1 / 10"));
                check(label(p, "resultSummary").contains("00:12"));
                String review = ((JTextArea) find(p, "review")).getText();
                check(review.contains("CORRECT") && review.contains("INCORRECT") && review.contains("UNANSWERED"));
                capture(p, "results.png"); click(p, "logout");
                text(p, "username", "student"); text(p, "password", "Student123!"); click(p, "login");
                check(label(p, "loginMessage").contains("Invalid"));
                text(p, "password", "Practice123!"); click(p, "login"); click(p, "startExam");
                clock.addAndGet(1800_000_000_000L); p.tick();
                check(label(p, "resultSummary").contains("Auto-submitted"));
                check(label(p, "resultTitle").contains("Score: 0 / 10"));
                check(p.canClose());
            } finally { confirm.set(true); p.canClose(); }
            AtomicLong deadlineClock = new AtomicLong();
            ExamPanel[] dialogPanel = new ExamPanel[1];
            dialogPanel[0] = new ExamPanel(new UserAccount("student", "Student", "Student123!".toCharArray()),
                    1, deadlineClock::get, message -> {
                        // Simulate a timer event while the confirmation dialog runs its nested event loop.
                        deadlineClock.set(1_000_000_000L);
                        dialogPanel[0].tick();
                        return false;
                    });
            ExamPanel d = dialogPanel[0];
            text(d, "username", "student"); text(d, "password", "Student123!"); click(d, "login");
            click(d, "startExam"); click(d, "submit");
            check(label(d, "resultSummary").contains("Auto-submitted"));
            check(label(d, "resultTitle").contains("Score: 0 / 10"));
            check(d.canClose());
        });
        System.out.println("Swing interaction checks passed: " + checks);
    }
}
