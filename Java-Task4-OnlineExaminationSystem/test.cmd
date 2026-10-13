@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
javac -J-Xmx128m -d bin src\*.java tests\*.java
if errorlevel 1 exit /b 1
java -Xmx128m -cp bin ExamSessionTest
if errorlevel 1 exit /b 1
java -Xmx128m -cp bin UserAccountTest
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -Xmx128m -cp bin ExamPanelTest
exit /b %ERRORLEVEL%
