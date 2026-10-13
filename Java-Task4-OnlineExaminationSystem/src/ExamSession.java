import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;

public final class ExamSession {
    private final List<Question> questions;
    private final int[] answers;
    private final long durationNanos;
    private final LongSupplier clock;
    private final long startedAt;
    private long finishedElapsed;
    private boolean submitted;
    private boolean timedOut;

    public ExamSession(List<Question> questions, int durationSeconds, LongSupplier clock) {
        if (questions == null || questions.isEmpty() || durationSeconds <= 0 || clock == null) {
            throw new IllegalArgumentException("Questions, a positive duration and a clock are required.");
        }
        this.questions = List.copyOf(questions);
        this.answers = new int[questions.size()];
        Arrays.fill(answers, -1);
        this.durationNanos = durationSeconds * 1_000_000_000L;
        this.clock = clock;
        this.startedAt = clock.getAsLong();
    }

    public List<Question> questions() { return questions; }
    public int answerAt(int index) { return answers[index]; }
    public boolean isSubmitted() { return submitted; }
    public boolean timedOut() { return timedOut; }

    private long elapsed() {
        return submitted ? finishedElapsed : Math.min(durationNanos, Math.max(0, clock.getAsLong() - startedAt));
    }
    public long remainingSeconds() { return (durationNanos - elapsed() + 999_999_999L) / 1_000_000_000L; }
    public long elapsedSeconds() { return elapsed() / 1_000_000_000L; }

    public boolean choose(int index, int choice) {
        if (index < 0 || index >= answers.length || choice < 0 || choice > 3) {
            throw new IllegalArgumentException("Invalid question or option.");
        }
        if (submitted || expireIfNeeded()) return false;
        answers[index] = choice;
        return true;
    }

    public boolean expireIfNeeded() {
        if (!submitted && elapsed() >= durationNanos) submit();
        return submitted;
    }

    public void submit() {
        if (submitted) return;
        // Use elapsed monotonic time so delayed UI ticks do not extend the exam.
        finishedElapsed = elapsed();
        timedOut = finishedElapsed >= durationNanos;
        submitted = true;
    }

    public int answeredCount() { return (int) Arrays.stream(answers).filter(x -> x >= 0).count(); }
    public int score() {
        int score = 0;
        for (int i = 0; i < answers.length; i++) if (answers[i] == questions.get(i).correctIndex()) score++;
        return score;
    }
}
