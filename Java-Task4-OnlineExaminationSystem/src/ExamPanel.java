import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

public final class ExamPanel extends JPanel {
    private final UserAccount account;
    private final int durationSeconds;
    private final LongSupplier clock;
    private final Predicate<String> confirmation;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final Timer timer;
    private ExamSession session;
    private int current;
    private final JLabel time = new JLabel();
    private final JLabel progress = new JLabel();
    private final JLabel question = new JLabel();
    private final ButtonGroup answers = new ButtonGroup();
    private final JRadioButton[] choices = new JRadioButton[4];
    private JButton previous;
    private JButton next;
    private final JLabel resultTitle = new JLabel();
    private final JLabel resultSummary = new JLabel();
    private final JTextArea review = new JTextArea();
    private JTextField name;
    private JTextField username;
    private JPasswordField password;
    private JPasswordField newPassword;
    private JLabel loginMessage;
    private JLabel profileMessage;

    public ExamPanel(UserAccount account, int durationSeconds, LongSupplier clock, Predicate<String> confirmation) {
        this.account = account;
        this.durationSeconds = durationSeconds;
        this.clock = clock;
        this.confirmation = confirmation;
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 32, 24, 32));
        setBackground(new Color(239, 244, 250));
        JLabel heading = new JLabel("EXAM DESK  /  Java fundamentals");
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(new Color(25, 53, 96));
        add(heading, BorderLayout.NORTH);
        pages.add(loginPage(), "login");
        pages.add(profilePage(), "profile");
        pages.add(examPage(), "exam");
        pages.add(resultsPage(), "results");
        add(pages, BorderLayout.CENTER);
        JLabel footer = new JLabel("Practice examination  |  10 questions  |  One answer per question");
        footer.setForeground(new Color(74, 91, 112));
        add(footer, BorderLayout.SOUTH);
        // Timer callbacks and button actions share the Swing event thread.
        timer = new Timer(250, e -> tick());
        cards.show(pages, "login");
    }

    private JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(16, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(28, 28, 28, 28));
        return panel;
    }
    private JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        return label;
    }
    private JButton button(String text, String id, Runnable action) {
        JButton b = new JButton(text);
        b.setName(id);
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setMargin(new Insets(10, 18, 10, 18));
        b.addActionListener(e -> action.run());
        return b;
    }
    private JPanel fields() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 10));
        panel.setOpaque(false);
        return panel;
    }
    private JPanel topAligned(JPanel content) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(content, BorderLayout.NORTH);
        return wrap;
    }
    private JPanel loginPage() {
        JPanel panel = page();
        panel.add(title("Welcome back"), BorderLayout.NORTH);
        JPanel form = fields();
        form.add(new JLabel("Sign in to prepare your profile and start the exam."));
        form.add(new JLabel("Username"));
        username = new JTextField(); username.setName("username"); form.add(username);
        form.add(new JLabel("Password"));
        password = new JPasswordField(); password.setName("password"); form.add(password);
        loginMessage = new JLabel("Demo: student / Student123!");
        loginMessage.setName("loginMessage"); form.add(loginMessage);
        JButton login = button("Sign in", "login", this::login);
        password.addActionListener(e -> login());
        form.add(login);
        panel.add(topAligned(form), BorderLayout.CENTER);
        return panel;
    }
    private void login() {
        char[] supplied = password.getPassword();
        boolean valid;
        try { valid = account.authenticate(username.getText().trim(), supplied); }
        finally { Arrays.fill(supplied, '\0'); password.setText(""); }
        if (!valid) { loginMessage.setText("Invalid username or password."); return; }
        name.setText(account.displayName());
        newPassword.setText("");
        profileMessage.setText("Save any profile changes before starting.");
        cards.show(pages, "profile");
    }
    private JPanel profilePage() {
        JPanel panel = page();
        panel.add(title("Prepare for your exam"), BorderLayout.NORTH);
        JPanel form = fields();
        form.add(new JLabel("Display name"));
        name = new JTextField(); name.setName("displayName"); form.add(name);
        form.add(new JLabel("New password (optional, 8-128 characters)"));
        newPassword = new JPasswordField(); newPassword.setName("newPassword"); form.add(newPassword);
        profileMessage = new JLabel(); profileMessage.setName("profileMessage"); form.add(profileMessage);
        form.add(button("Save profile", "saveProfile", this::saveProfile));
        form.add(new JLabel("10 questions. Use Previous and Next to review your answers."));
        form.add(new JLabel("The exam submits automatically when the timer reaches zero."));
        form.add(button("Start exam", "startExam", this::startExam));
        panel.add(topAligned(form), BorderLayout.CENTER);
        return panel;
    }
    private void saveProfile() {
        char[] supplied = newPassword.getPassword();
        try {
            account.updateProfile(name.getText(), supplied);
            newPassword.setText("");
            profileMessage.setText("Profile saved.");
        } catch (IllegalArgumentException ex) { profileMessage.setText(ex.getMessage()); }
        finally { Arrays.fill(supplied, '\0'); }
    }
    private JPanel examPage() {
        JPanel panel = page();
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        progress.setFont(new Font("SansSerif", Font.BOLD, 18)); progress.setName("progress");
        time.setFont(new Font("Monospaced", Font.BOLD, 22)); time.setName("timer");
        header.add(progress, BorderLayout.WEST); header.add(time, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(10, 22)); body.setOpaque(false);
        question.setFont(new Font("SansSerif", Font.BOLD, 20)); question.setName("question");
        body.add(question, BorderLayout.NORTH);
        JPanel options = new JPanel(new GridLayout(4, 1, 0, 12)); options.setOpaque(false);
        for (int i = 0; i < choices.length; i++) {
            int option = i;
            choices[i] = new JRadioButton(); choices[i].setName("choice" + i);
            choices[i].setOpaque(false); choices[i].setFont(new Font("SansSerif", Font.PLAIN, 18));
            choices[i].addActionListener(e -> {
                if (session != null && !session.choose(current, option)) finish();
                else updateProgress();
            });
            answers.add(choices[i]); options.add(choices[i]);
        }
        body.add(options, BorderLayout.CENTER); panel.add(body, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); controls.setOpaque(false);
        previous = button("Previous", "previous", () -> navigate(-1));
        next = button("Next", "next", () -> navigate(1));
        controls.add(previous); controls.add(next);
        controls.add(button("Submit exam", "submit", this::manualSubmit));
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }
    private void startExam() {
        session = new ExamSession(QuestionBank.javaBasics(), durationSeconds, clock);
        current = 0;
        showQuestion(); cards.show(pages, "exam");
        timer.start(); tick();
    }
    private void navigate(int direction) {
        if (session == null || session.expireIfNeeded()) { finish(); return; }
        current = Math.max(0, Math.min(session.questions().size() - 1, current + direction));
        showQuestion();
    }
    private void showQuestion() {
        Question q = session.questions().get(current);
        question.setText("<html>" + q.prompt() + "</html>");
        answers.clearSelection();
        for (int i = 0; i < choices.length; i++) {
            choices[i].setText((char) ('A' + i) + ".  " + q.options().get(i));
            choices[i].setSelected(session.answerAt(current) == i);
        }
        previous.setEnabled(current > 0);
        next.setEnabled(current < session.questions().size() - 1);
        updateProgress();
    }
    private void updateProgress() {
        if (session != null) progress.setText("Question " + (current + 1) + " of " + session.questions().size()
                + "  |  Answered: " + session.answeredCount());
    }
    void tick() {
        if (session == null || session.isSubmitted()) return;
        time.setText("Time left  " + formatTime(session.remainingSeconds()));
        if (session.expireIfNeeded()) finish();
    }
    private boolean confirm(String message) {
        return confirmation != null ? confirmation.test(message)
                : JOptionPane.showConfirmDialog(this, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
    private void manualSubmit() {
        if (session == null || session.isSubmitted()) return;
        if (session.expireIfNeeded()) { finish(); return; }
        if (confirm("Submit now? " + (session.questions().size() - session.answeredCount()) + " question(s) unanswered.")) {
            finish();
        }
    }
    private JPanel resultsPage() {
        JPanel panel = page();
        JPanel summary = fields();
        resultTitle.setFont(new Font("SansSerif", Font.BOLD, 25)); resultTitle.setName("resultTitle");
        resultSummary.setName("resultSummary"); summary.add(resultTitle); summary.add(resultSummary);
        panel.add(summary, BorderLayout.NORTH);
        review.setEditable(false); review.setLineWrap(true); review.setWrapStyleWord(true);
        review.setFont(new Font("SansSerif", Font.PLAIN, 15)); review.setMargin(new Insets(12, 12, 12, 12));
        review.setName("review"); panel.add(new JScrollPane(review), BorderLayout.CENTER);
        panel.add(button("Log out", "logout", this::logout), BorderLayout.SOUTH);
        return panel;
    }
    private void finish() {
        if (session == null) return;
        session.submit(); timer.stop();
        resultTitle.setText(account.displayName() + " - Score: " + session.score() + " / " + session.questions().size());
        resultSummary.setText((session.timedOut() ? "Time expired. Auto-submitted." : "Exam submitted.")
                + "  Time taken: " + formatTime(session.elapsedSeconds())
                + "  |  Unanswered: " + (session.questions().size() - session.answeredCount()));
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < session.questions().size(); i++) {
            Question q = session.questions().get(i);
            int chosen = session.answerAt(i);
            text.append(i + 1).append(". ").append(q.prompt()).append('\n');
            text.append(chosen < 0 ? "UNANSWERED" : chosen == q.correctIndex() ? "CORRECT" : "INCORRECT");
            text.append("  |  Your answer: ").append(chosen < 0 ? "None" : q.options().get(chosen));
            text.append("\nCorrect answer: ").append(q.options().get(q.correctIndex())).append("\n\n");
        }
        review.setText(text.toString()); review.setCaretPosition(0);
        cards.show(pages, "results");
    }
    private void logout() {
        timer.stop(); session = null; username.setText(""); password.setText("");
        loginMessage.setText("You have been logged out."); cards.show(pages, "login");
    }
    public boolean canClose() {
        if (session != null && !session.isSubmitted() && !confirm("An exam is in progress. Are you sure you want to quit?")) return false;
        timer.stop(); return true;
    }
    private static String formatTime(long seconds) { return String.format("%02d:%02d", seconds / 60, seconds % 60); }
}
