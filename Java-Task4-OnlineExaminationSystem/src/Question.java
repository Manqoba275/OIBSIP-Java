import java.util.List;

public record Question(String prompt, List<String> options, int correctIndex) {
    public Question {
        if (prompt == null || prompt.isBlank() || options == null || options.size() != 4
                || options.stream().anyMatch(x -> x == null || x.isBlank())
                || correctIndex < 0 || correctIndex >= 4) {
            throw new IllegalArgumentException("A question needs four choices and a valid answer.");
        }
        options = List.copyOf(options);
    }
}
