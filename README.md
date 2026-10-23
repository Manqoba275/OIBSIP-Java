# OIBSIP - Java Development

Four projects for the OASIS INFOBYTE internship. All four projects are implemented and tested: Online Reservation, ATM Interface, Online Examination and Digital Library Management.

## Projects

- [Task 1: Online Reservation System](Java-Task1-OnlineReservationSystem/)
- [Task 3: ATM Interface](Java-Task3-ATMInterface/)
- [Task 4: Online Examination System](Java-Task4-OnlineExaminationSystem/)
- [Task 5: Digital Library Management System](Java-Task5-DigitalLibraryManagementSystem/)

## VS Code

Open `OIBSIP-Java.code-workspace` to see all four projects. For the simplest run/debug workflow, open one project folder directly. Use a JDK and the Java Extension Pack. JDK 23 was detected during setup.

The ATM is a runnable console application, and Online Examination is a Swing desktop application. Online Reservation adds a Swing booking interface with SQLite persistence. Digital Library is a Java Servlet web application with an embedded server, SQLite persistence, member loans and an admin area. All four have demo credentials, run scripts and automated checks. See their project READMEs for instructions.

## Repository naming decision

The supplied document requires a single repository named `OIBSIP` across tracks. The owner explicitly chose two repositories, `OIBSIP-Java` and `OIBSIP-DataAnalytics`, instead. Project folders retain the document's `[TrackName]-[Task]-[ProjectName]` pattern. These repository names differ from the official submission instruction.

## Save and publish your changes

From this repository root, review changes and publish them:

```powershell
git status
git add .
git commit -m "Describe your changes"
git push
```

You can also use VS Code Source Control to stage, commit and sync changes.

## Submission checklist from the supplied document

- [ ] Implement and verify your selected projects (Java requires at least two tasks).
- [ ] Finish each project README and include relevant screenshots or output.
- [ ] Record each completed project working end to end; begin with a two-second title card showing full name, track and task title.
- [ ] Complete the document's LinkedIn/demo, peer-evaluation and submission steps yourself.

No submission or social-media actions have been performed.
