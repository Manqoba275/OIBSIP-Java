@echo off
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File setup.ps1
if errorlevel 1 exit /b 1
if not exist bin mkdir bin
javac -J-Xmx256m --release 17 -encoding UTF-8 -cp "lib/*" -d bin src\main\java\*.java
exit /b %errorlevel%
