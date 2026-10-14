import java.util.List;

public record Train(String number, String name, List<String> stops) {
    public Train { stops = List.copyOf(stops); }
    public static List<Train> catalogue() {
        return List.of(
            new Train("101", "Highveld Express", List.of("Johannesburg", "Pietermaritzburg", "Durban")),
            new Train("102", "Coastal Link", List.of("Cape Town", "George", "Gqeberha")),
            new Train("103", "Northern Star", List.of("Pretoria", "Polokwane", "Musina"))
        );
    }
    public static Train find(String number) {
        return catalogue().stream().filter(t -> t.number().equals(number)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Choose a listed train number: 101, 102 or 103."));
    }
}
