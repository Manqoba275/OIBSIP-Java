# ATM Interface

OASIS INFOBYTE - Java Development, Task 3.

**Status: scaffold only. Implementation has not started.**

## Required technology

Java console application with object-oriented design.

## Requirements checklist

- [ ] Authenticate with User ID and PIN; deny access after 3 incorrect attempts.
- [ ] Menu: Transaction History, Withdraw, Deposit, Transfer and Quit.
- [ ] Withdraw: validate balance, update balance and log the transaction.
- [ ] Deposit: update balance and log the transaction.
- [ ] Transfer: ask for recipient account ID and amount, validate balance, update both accounts and log the transaction.
- [ ] Show Insufficient Funds when withdrawal or transfer exceeds the balance.
- [ ] Store current-session transactions in an ArrayList and display the history clearly.
- [ ] Use at least five classes: ATM, Account, Transaction, Bank and Main.
- [ ] Quit with a goodbye message.

## Develop in VS Code

Open this project folder directly in VS Code. Edit `src/Main.java`, then use **Run Java** above `main` or press F5. The entry point is intentionally empty and currently produces no output. Add your own classes and implementation. For command-line use from this folder:

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin src/*.java
java -cp bin Main
```

## Evidence and documentation

Add screenshots to `screenshots/` and relevant non-sensitive outputs to `output/`. Update this README with your design, setup steps, usage, verification and demo link after implementing the project. Empty folders are tracked with `.gitkeep`.

Source: supplied OASIS INFOBYTE Task List, Java task cards (pages 15-18).
