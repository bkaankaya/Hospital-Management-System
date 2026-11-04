@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>&1
if errorlevel 1 (
    echo Apache Maven is required. Install Maven and add mvn to PATH.
    exit /b 1
)
call mvn -q -DskipTests package
if errorlevel 1 (
    echo Build failed. See Maven output above.
    exit /b 1
)
java -jar target\hospital-management-system-2.0.0.jar
exit /b %errorlevel%
