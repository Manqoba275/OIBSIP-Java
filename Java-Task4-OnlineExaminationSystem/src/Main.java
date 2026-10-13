import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExamPanel panel = new ExamPanel(new UserAccount("student", "Student", "Student123!".toCharArray()),
                    30 * 60, System::nanoTime, null);
            JFrame frame = new JFrame("Online Examination System");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setContentPane(panel);
            frame.setMinimumSize(new Dimension(850, 620));
            frame.setSize(1000, 740);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) {
                    // Let the active exam confirm closing before its timer and window are disposed.
                    if (panel.canClose()) frame.dispose();
                }
            });
            frame.setVisible(true);
        });
    }
}
