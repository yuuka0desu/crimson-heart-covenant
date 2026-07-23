@echo off
chcp 65001 >nul
setlocal

title Crimson Heart Covenant 2.0.0 Test Server
cd /d "%~dp0"

echo ==============================================
echo  Crimson Heart Covenant 2.0.0 Test Server
echo  Minecraft 1.20.1 / Forge 47.3.22
echo ==============================================
echo.

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat was not found in the project directory.
    pause
    exit /b 1
)

call gradlew.bat runServer
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if "%EXIT_CODE%"=="0" (
    echo Test server stopped normally.
) else (
    echo Test server exited with code %EXIT_CODE%.
)

pause
exit /b %EXIT_CODE%
