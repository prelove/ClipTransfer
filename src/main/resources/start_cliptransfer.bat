@echo off

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 8 or higher and add it to your PATH
    pause
    exit /b 1
)

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Look for JAR file
set JAR_FILE=
for %%f in ("%SCRIPT_DIR%*.jar") do (
    set JAR_FILE=%%f
    goto :found
)

echo Error: No JAR file found in current directory
echo Please make sure ClipTransfer.jar is in the same folder as this script
pause
exit /b 1

:found
REM Create logs directory if it doesn't exist
if not exist "%SCRIPT_DIR%logs" mkdir "%SCRIPT_DIR%logs"

REM Start the application in background and close this window
start /min "" java -Xmx512m -Dfile.encoding=UTF-8 -jar "%JAR_FILE%" 2>"%SCRIPT_DIR%logs\error.log"

REM Exit immediately (don't wait)
exit