import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ExamSessionTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("Check " + checks); }
    private static void rejects(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException ex) { checks++; return; }
        throw new AssertionError("Expected validation failure");
    }
    public static void main(String[] args) {
        AtomicLong clock = new AtomicLong();
        List<Question> questions = QuestionBank.javaBasics();
        check(questions.size() == 10);
        ExamSession exam = new ExamSession(questions, 10, clock::get);
        check(exam.remainingSeconds() == 10 && exam.answeredCount() == 0);
        check(exam.answerAt(0) == -1);
        check(exam.choose(0, questions.get(0).correctIndex()));
        check(exam.score() == 1);
        check(exam.choose(0, (questions.get(0).correctIndex() + 1) % 4));
        check(exam.answeredCount() == 1 && exam.score() == 0);
        rejects(() -> exam.choose(-1, 0)); rejects(() -> exam.choose(0, 4));
        clock.set(9_999_999_999L); check(exam.remainingSeconds() == 1);
        clock.set(10_000_000_000L);
        check(!exam.choose(1, questions.get(1).correctIndex()));
        check(exam.isSubmitted() && exam.timedOut() && exam.answerAt(1) == -1);
        check(exam.elapsedSeconds() == 10 && exam.remainingSeconds() == 0);
        clock.set(90_000_000_000L); exam.submit(); check(exam.elapsedSeconds() == 10);
        ExamSession manual = new ExamSession(questions, 30, clock::get);
        for (int i = 0; i < 10; i++) manual.choose(i, questions.get(i).correctIndex());
        clock.addAndGet(5_000_000_000L); manual.submit();
        check(manual.score() == 10 && manual.answeredCount() == 10);
        check(manual.elapsedSeconds() == 5 && !manual.timedOut());
        check(!manual.choose(0, 0)); clock.addAndGet(100_000_000_000L);
        check(manual.elapsedSeconds() == 5);
        ExamSession delayed = new ExamSession(questions, 1, clock::get);
        clock.addAndGet(10_000_000_000L); check(delayed.expireIfNeeded() && delayed.timedOut());
        check(delayed.elapsedSeconds() == 1 && delayed.score() == 0);
        rejects(() -> new ExamSession(List.of(), 10, clock::get));
        rejects(() -> new ExamSession(questions, 0, clock::get));
        rejects(() -> new Question("Test", List.of("a"), 0));
        try { questions.get(0).options().clear(); throw new AssertionError("Mutable choices"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("Exam session checks passed: " + checks);
    }
}
