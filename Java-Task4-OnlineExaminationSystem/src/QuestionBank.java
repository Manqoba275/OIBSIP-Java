import java.util.List;

public final class QuestionBank {
    private QuestionBank() {}
    public static List<Question> javaBasics() {
        return List.of(
            new Question("Which keyword makes a class inherit another class?", List.of("implements", "extends", "inherits", "super"), 1),
            new Question("Which type stores a true or false value?", List.of("char", "int", "boolean", "String"), 2),
            new Question("Which collection grows dynamically and keeps insertion order?", List.of("ArrayList", "Thread", "Scanner", "Integer"), 0),
            new Question("What does the JVM execute?", List.of("HTML", "SQL", "CSS", "Java bytecode"), 3),
            new Question("Which keyword prevents a variable from being reassigned?", List.of("static", "final", "public", "void"), 1),
            new Question("Which method is the usual entry point of a Java application?", List.of("run()", "start()", "main()", "init()"), 2),
            new Question("Which block handles an exception?", List.of("catch", "switch", "for", "package"), 0),
            new Question("Which access modifier restricts a member to its own class?", List.of("public", "protected", "static", "private"), 3),
            new Question("Which class reads console input?", List.of("Math", "Scanner", "StringBuilder", "SystemClock"), 1),
            new Question("Which Swing component lets a user choose one option in a ButtonGroup?", List.of("JLabel", "JPanel", "JRadioButton", "JScrollPane"), 2)
        );
    }
}
