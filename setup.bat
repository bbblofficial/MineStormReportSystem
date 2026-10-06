@echo off
REM MineStormReportSystem — one-shot setup (Windows)
echo ===================================================
echo   MineStormReportSystem — Setup
echo ===================================================

cd /d "%~dp0"

echo.
echo Building with Maven...
call mvn -B clean package -DskipTests
if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

set JAR=bukkit\target\MineStormReportSystem-Bukkit-1.0.0.jar
if not exist "%JAR%" (
    echo JAR not found: %JAR%
    exit /b 1
)

echo.
echo Build OK: %JAR%
echo.
echo Copy to your server plugins folder and restart.
