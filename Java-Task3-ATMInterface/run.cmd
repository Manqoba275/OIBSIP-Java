@echo off
setlocal
cd /d "%~dp0"
if not exist bin mkdir bin
echo Compiling Java sources...
javac -J-Xmx128m -d bin src\*.java
if errorlevel 1 (
  echo Compilation failed. See the error above.
  pause
  exit /b 1
)
echo Running Main...
java -Xms16m -Xmx128m -cp bin Main
set "taskExit=%ERRORLEVEL%"
echo.
echo Program finished with exit code %taskExit%.
pause
exit /b %taskExit%
