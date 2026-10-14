import javax.swing.*;
import java.awt.Dimension;
import java.nio.file.Path;
import java.time.Clock;

public class Main {
    public static void main(String[] args) {
        try {
            // Resolve one local database per project; all JDBC resources are closed after each operation.
            DBConnection database = new DBConnection(Path.of("data", "reservations.db"));
            ReservationService service = new ReservationService(database, Clock.systemDefaultZone());
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Online Reservation System");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setContentPane(new ReservationPanel(service, null, null));
                frame.setMinimumSize(new Dimension(850, 650)); frame.setSize(1000, 760);
                frame.setLocationRelativeTo(null); frame.setVisible(true);
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Unable to start. Run setup.ps1 and check that the project folder is writable.",
                    "Startup problem", JOptionPane.ERROR_MESSAGE));
            ex.printStackTrace(System.err);
        }
    }
}
