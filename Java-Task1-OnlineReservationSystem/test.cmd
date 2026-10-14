@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1"
if errorlevel 1 exit /b 1
if not exist bin mkdir bin
javac -J-Xmx128m --release 17 -d bin src\*.java tests\*.java
if errorlevel 1 exit /b 1
java -Xmx128m -cp "bin;lib/*" DatabaseTest
if errorlevel 1 exit /b 1
java -Xmx128m -cp "bin;lib/*" ReservationServiceTest
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -Xmx128m -cp "bin;lib/*" ReservationPanelTest
exit /b %ERRORLEVEL%
