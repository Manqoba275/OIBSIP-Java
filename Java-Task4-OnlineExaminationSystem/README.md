# Online Examination System

OASIS INFOBYTE - Java Development, Task 4.

**Status: scaffold only. Implementation has not started.**

## Required technology

Java and Swing.

## Requirements checklist

- [ ] Login with username and password, then load the exam interface.
- [ ] Allow profile display-name and password changes before the exam.
- [ ] Show one MCQ at a time with four radio-button options.
- [ ] Provide Next and Previous navigation.
- [ ] Show a countdown timer and auto-submit when time runs out.
- [ ] Provide manual submission with a confirmation dialog.
- [ ] Show score (X out of Y), time taken and correct/incorrect answer breakdown.
- [ ] Confirm before closing the window during an exam.
- [ ] Provide logout from the results screen back to login.

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
