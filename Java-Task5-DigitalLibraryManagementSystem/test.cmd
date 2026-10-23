@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b 1
javac -J-Xmx256m --release 17 -encoding UTF-8 -cp "bin;lib/*" -d bin src\test\java\*.java
if errorlevel 1 exit /b 1
java -Xmx192m -cp "bin;lib/*" DatabaseTest
if errorlevel 1 exit /b 1
java -Xmx192m -cp "bin;lib/*" LibraryTest
if errorlevel 1 exit /b 1
java -Xmx192m -cp "bin;lib/*" WebTest
exit /b %errorlevel%
