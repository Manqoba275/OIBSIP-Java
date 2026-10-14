import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ReservationPanel extends JPanel {
    private final ReservationService service;
    private final Consumer<String> notification;
    private final Predicate<String> confirmation;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final JTextField passenger = field("passenger");
    private final JTextField number = field("trainNumber");
    private final JTextField trainName = field("trainName");
    private final JTextField date = field("journeyDate");
    private final JComboBox<String> travelClass = new JComboBox<>(ReservationService.CLASSES.toArray(String[]::new));
    private final JComboBox<String> source = new JComboBox<>();
    private final JComboBox<String> destination = new JComboBox<>();
    private final JTextField pnr = field("pnr");
    private final JTextArea details = new JTextArea();
    private final JLabel status = new JLabel("Ready to book a journey.");
    private final JButton cancel = new JButton("Confirm cancellation");
    private Booking fetched;

    public ReservationPanel(ReservationService service, Consumer<String> notification, Predicate<String> confirmation) {
        super(new BorderLayout(0, 20));
        this.service = service; this.notification = notification; this.confirmation = confirmation;
        setBorder(new EmptyBorder(24, 28, 24, 28)); setBackground(new Color(236, 243, 247));
        JLabel brand = new JLabel("JOURNEY DESK  /  Reservations"); brand.setFont(new Font("SansSerif", Font.BOLD, 25));
        brand.setForeground(new Color(20, 71, 88)); add(brand, BorderLayout.NORTH);
        pages.add(new LoginForm(service, () -> { date.setText(service.today().toString()); cards.show(pages, "booking"); }), "login");
        pages.add(bookingPage(), "booking"); add(pages, BorderLayout.CENTER);
        JLabel footer = new JLabel("Local demonstration  |  Fictional trains and routes  |  No payment required");
        add(footer, BorderLayout.SOUTH);
        trainName.setEditable(false); number.setText("101"); date.setText(service.today().toString());
        cards.show(pages, "login");
    }
    private static JTextField field(String name) { JTextField f = new JTextField(); f.setName(name); return f; }
    private static void changed(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }
    private JPanel bookingPage() {
        JPanel panel = new JPanel(new BorderLayout(0, 18)); panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JLabel title = new JLabel("Book or cancel a reservation"); title.setFont(new Font("SansSerif", Font.BOLD, 21));
        JButton logout = new JButton("Log out"); logout.setName("logout");
        logout.addActionListener(e -> { service.logout(); passenger.setText(""); pnr.setText(""); status.setText("Ready to book a journey."); cards.show(pages, "login"); });
        header.add(title, BorderLayout.WEST); header.add(logout, BorderLayout.EAST); panel.add(header, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane(); tabs.setName("tabs");
        tabs.addTab("New reservation", reservationForm()); tabs.addTab("Find / cancel", cancellationForm());
        panel.add(tabs, BorderLayout.CENTER); status.setName("status"); panel.add(status, BorderLayout.SOUTH);
        return panel;
    }
    private JPanel reservationForm() {
        JPanel panel = new JPanel(new BorderLayout(10, 18)); panel.setBorder(new EmptyBorder(22, 22, 22, 22)); panel.setBackground(Color.WHITE);
        JPanel form = new JPanel(new GridLayout(0, 2, 18, 14)); form.setOpaque(false);
        travelClass.setName("travelClass"); source.setName("source"); destination.setName("destination");
        addRow(form, "Passenger name", passenger); addRow(form, "Train number (101, 102 or 103)", number);
        addRow(form, "Train name", trainName); addRow(form, "Travel class", travelClass);
        addRow(form, "Journey date (YYYY-MM-DD)", date); addRow(form, "Source station", source); addRow(form, "Destination station", destination);
        form.setPreferredSize(new Dimension(750, 350));
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(form, BorderLayout.NORTH); panel.add(wrap, BorderLayout.CENTER);
        JButton book = new JButton("Book journey"); book.setName("book"); book.addActionListener(e -> book()); panel.add(book, BorderLayout.SOUTH);
        changed(number, this::updateTrain);
        return panel;
    }
    private void addRow(JPanel form, String label, JComponent component) { form.add(new JLabel(label)); form.add(component); }
    private void updateTrain() {
        source.removeAllItems(); destination.removeAllItems();
        try {
            Train t = Train.find(number.getText().trim()); trainName.setText(t.name());
            for (String stop : t.stops()) { source.addItem(stop); destination.addItem(stop); }
            destination.setSelectedIndex(destination.getItemCount() - 1);
        } catch (IllegalArgumentException ex) { trainName.setText("Enter 101, 102 or 103"); }
    }
    private void book() {
        try {
            Booking b = service.book(passenger.getText(), number.getText().trim(), (String) travelClass.getSelectedItem(),
                    date.getText().trim(), (String) source.getSelectedItem(), (String) destination.getSelectedItem());
            pnr.setText(b.pnr()); fetched = b; details.setText(b.details()); cancel.setEnabled(true);
            status.setText("Booking saved. Use Find / cancel to review your PNR.");
            notifyUser("Reservation confirmed\n\n" + b.details());
        } catch (Exception ex) { showError(ex); }
    }
    private JPanel cancellationForm() {
        JPanel panel = new JPanel(new BorderLayout(12, 18)); panel.setBackground(Color.WHITE); panel.setBorder(new EmptyBorder(22, 22, 22, 22));
        JPanel search = new JPanel(new BorderLayout(12, 0)); search.setOpaque(false);
        search.add(new JLabel("PNR"), BorderLayout.WEST); search.add(pnr, BorderLayout.CENTER);
        JButton fetch = new JButton("Fetch booking"); fetch.setName("fetch"); fetch.addActionListener(e -> fetch()); search.add(fetch, BorderLayout.EAST);
        panel.add(search, BorderLayout.NORTH);
        details.setEditable(false); details.setFont(new Font("SansSerif", Font.PLAIN, 18)); details.setMargin(new Insets(16, 16, 16, 16));
        details.setName("details"); details.setLineWrap(true); details.setWrapStyleWord(true); panel.add(new JScrollPane(details), BorderLayout.CENTER);
        cancel.setName("cancel"); cancel.setEnabled(false); cancel.addActionListener(e -> cancel()); panel.add(cancel, BorderLayout.SOUTH);
        // Editing a PNR invalidates the fetched selection, preventing cancellation of stale results.
        changed(pnr, () -> { fetched = null; details.setText(""); cancel.setEnabled(false); });
        return panel;
    }
    private void fetch() {
        fetched = null; cancel.setEnabled(false); details.setText("");
        try {
            fetched = service.fetch(pnr.getText()).orElse(null);
            if (fetched == null) { status.setText("No booking found for this PNR."); return; }
            details.setText(fetched.details()); cancel.setEnabled(true); status.setText("Booking found.");
        } catch (Exception ex) { showError(ex); }
    }
    private void cancel() {
        if (fetched == null) return;
        String selectedPnr = fetched.pnr();
        String message = "Are you sure you want to cancel?\n\n" + fetched.details();
        boolean accepted = confirmation != null ? confirmation.test(message)
                : JOptionPane.showConfirmDialog(this, message, "Cancel reservation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (!accepted) return;
        try {
            boolean removed = service.cancel(selectedPnr);
            fetched = null; cancel.setEnabled(false); details.setText("");
            status.setText(removed ? "Reservation cancelled." : "This reservation no longer exists.");
            notifyUser(status.getText());
        } catch (Exception ex) { showError(ex); }
    }
    private void notifyUser(String message) {
        if (notification != null) notification.accept(message);
        else JOptionPane.showMessageDialog(this, message, "Reservation", JOptionPane.INFORMATION_MESSAGE);
    }
    private void showError(Exception ex) {
        status.setText(ex instanceof IllegalArgumentException || ex instanceof IllegalStateException
                ? ex.getMessage() : "Could not access the booking database. Please try again.");
    }
}
