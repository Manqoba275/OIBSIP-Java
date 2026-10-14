import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public class LoginForm extends JPanel {
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel message = new JLabel("Demo: traveler / Travel123!");
    public LoginForm(ReservationService service, Runnable onSuccess) {
        super(new BorderLayout(0, 20)); setBackground(Color.WHITE); setBorder(new EmptyBorder(32, 40, 32, 40));
        JLabel heading = new JLabel("Sign in to manage your journeys"); heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(heading, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridLayout(0, 1, 8, 12)); form.setOpaque(false);
        form.add(new JLabel("Username")); username.setName("username"); form.add(username);
        form.add(new JLabel("Password")); password.setName("password"); form.add(password);
        message.setName("loginMessage"); form.add(message);
        JButton login = new JButton("Sign in"); login.setName("login");
        Runnable signIn = () -> {
            char[] value = password.getPassword();
            try {
                if (service.login(username.getText(), value)) { message.setText("Signed in."); onSuccess.run(); }
                else message.setText("Invalid username or password.");
            } catch (Exception ex) { message.setText("Unable to open the local booking database."); }
            finally { Arrays.fill(value, '\0'); password.setText(""); }
        };
        login.addActionListener(e -> signIn.run()); password.addActionListener(e -> signIn.run()); form.add(login);
        form.setPreferredSize(new Dimension(500, 300));
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(form, BorderLayout.NORTH); add(wrap, BorderLayout.CENTER);
    }
}
