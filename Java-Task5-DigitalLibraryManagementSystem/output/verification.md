# Verification — 23 October 2026

Environment: Windows, JDK 23, `javac --release 17`, SQLite JDBC 3.53.4.0, embedded Tomcat 10.1.61.

`test.cmd` completed with exit code 0:

```text
Dependencies verified.
DatabaseTest: 6 checks passed.
LibraryTest: 61 checks passed.
WebTest: 27 checks passed.
```

The lending suite uses an injected clock to check the due-date boundary, a three-day overdue fine and the frozen amount after return. Two concurrent requests for a single copy produced exactly one successful loan. Reopening the temporary database preserved loans and reader messages.

HTTP tests started the real servlet container on an available loopback port. Checks included member/admin permissions, blocked GET mutations, CSRF rejection, HTML escaping of stored messages, account disabling and logout.

Microsoft Edge browser verification passed for member login, borrowing, returning, contact submission, admin login, book creation/deletion and title/author search. Desktop screenshots were taken at 1360×1000; the mobile catalogue was checked at 390×844 with no document-level horizontal overflow. All screenshots were visually inspected.

The browser and test databases were separate from the application's normal `data/library.db`. No live member data was used.
