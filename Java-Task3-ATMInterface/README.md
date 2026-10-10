# ATM Interface

A Java console ATM simulation for OASIS INFOBYTE Java Development, Task 3.

## Run

Requires JDK 17 or later (`java` and `javac` on PATH). Tested with JDK 23.
Open this folder in VS Code and run `src/Main.java`, or double-click `run.cmd`.
The launcher compiles the sources, starts the ATM and keeps the window open when it exits.

From PowerShell in this folder:

```powershell
.\run.cmd
```

## Demo accounts

These are fictional accounts, reset whenever the program starts.

| User ID | PIN | Opening balance |
| --- | --- | --- |
| 1001 | 1234 | R1000.00 |
| 1002 | 5678 | R500.00 |

The console displays entered PINs. This is an in-memory learning simulation, not a real banking system. Do not enter real credentials. Balances and transaction histories are not saved after exit. The three-attempt login limit applies to the current program run.

## Features

- [x] User ID and PIN login; access denied after three failed attempts.
- [x] Transaction History, Withdraw, Deposit, Transfer and Quit menu options.
- [x] Exact decimal balances using BigDecimal.
- [x] Positive amounts with at most two decimal places.
- [x] Insufficient-funds checks before withdrawal and transfer.
- [x] Transfer updates both accounts and records both sides.
- [x] Invalid recipient and self-transfer rejection.
- [x] ArrayList transaction history with timestamps, amounts and resulting balances.
- [x] Invalid menu/input recovery and graceful exit when input closes.
- [x] Five application classes: ATM, Account, Transaction, Bank and Main.

## Try a session

1. Log in with User ID `1001`, PIN `1234`.
2. Choose `3` and deposit `100.50`.
3. Choose `2` and withdraw `20`.
4. Choose `4`, recipient `1002`, amount `50`.
5. Choose `1` to view all three transactions. Your balance is `R1030.50`.
6. Choose `5` to quit.

## Design

`Main` creates the demo bank and launches the console. `ATM` handles input, authentication attempts and menu interaction. `Bank` validates account membership, amounts and transfers. `Account` encapsulates balances and exposes read-only history snapshots. `Transaction` stores an immutable transaction entry. All validation occurs before a transfer changes either balance.

## Tests

From this folder, run:

```powershell
.\test.cmd
```

No external dependencies or database are needed. The tests cover authentication, exact decimal arithmetic, full-balance withdrawal, transfer bookkeeping, rejected-operation invariants, immutable history, duplicate accounts, login lockout, invalid console input and end-of-input handling.

Expected result: 22 banking checks and 24 console interaction checks pass.
See `output/test-results.txt` and `output/sample-session.txt` for recorded output.

## Submission evidence

Add your own screenshots to `screenshots/` and record an end-to-end demonstration.
Demo video link: not yet recorded.

Requirements source: supplied OASIS INFOBYTE Task List, Java Task 3 (pages 16-17).
