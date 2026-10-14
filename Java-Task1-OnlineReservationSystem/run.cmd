@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1"
if errorlevel 1 (pause & exit /b 1)
if not exist bin mkdir bin
javac -J-Xmx128m --release 17 -d bin src\*.java
if errorlevel 1 (pause & exit /b 1)
java -Xmx128m -cp "bin;lib/*" Main
if errorlevel 1 pause
