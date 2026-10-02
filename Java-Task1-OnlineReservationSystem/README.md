# Online Reservation System

OASIS INFOBYTE - Java Development, Task 1.

**Status: scaffold only. Implementation has not started.**

## Required technology

Java GUI (Swing or JavaFX), JDBC, MySQL or SQLite.

## Requirements checklist

- [ ] Login with username and password; deny invalid credentials.
- [ ] Reservation form: passenger name, train number, automatically populated train name, class, journey date, source and destination.
- [ ] Save bookings to a database and generate a unique PNR.
- [ ] Show a confirmation dialog with booking details.
- [ ] Fetch and display booking details using a PNR.
- [ ] Confirm cancellation with a dialog, then remove the booking from the database.
- [ ] Validate required fields, dates and numeric train numbers.

## Develop in VS Code

Open this project folder directly in VS Code. Edit `src/Main.java`, then use **Run Java** above `main` or press F5. The entry point is intentionally empty and currently produces no output. Add your own classes and implementation. For command-line use from this folder:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin src/*.java
java -cp bin Main
```

Add your chosen JDBC driver in `lib/` (or configure a build tool) when you implement database access. Database setup and JavaFX dependencies, if selected, are not installed.

## Evidence and documentation

Add screenshots to `screenshots/` and relevant non-sensitive outputs to `output/`. Update this README with your design, setup steps, usage, verification and demo link after implementing the project. Empty folders are tracked with `.gitkeep`.

Source: supplied OASIS INFOBYTE Task List, Java task cards (pages 15-18).
