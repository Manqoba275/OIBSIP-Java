@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 (
  echo Build failed. Check that JDK 17 or newer is installed.
  pause
  exit /b 1
)
echo Open http://127.0.0.1:8080 in your browser after the ready message.
java -Xms32m -Xmx192m -cp "bin;lib/*" Main
if errorlevel 1 pause
