import java.time.LocalDate;

public record Booking(String pnr, String passenger, String trainNumber, String trainName,
                      String travelClass, LocalDate journeyDate, String source, String destination) {
    public String details() {
        return "PNR: " + pnr + "\nPassenger: " + passenger + "\nTrain: " + trainNumber + " - " + trainName
                + "\nClass: " + travelClass + "\nJourney date: " + journeyDate
                + "\nFrom: " + source + "\nTo: " + destination;
    }
}
