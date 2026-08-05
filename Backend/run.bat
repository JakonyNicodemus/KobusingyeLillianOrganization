@echo off
echo ========================================
echo   🇺🇬 UGANDA EMPLOYEE MANAGEMENT SYSTEM
echo ========================================
echo.
echo Compiling Java files...
javac -d . src/com/ems/*.java

if errorlevel 1 (
    echo.
    echo ❌ Compilation failed! Please check your code.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation successful!
echo.
echo Starting server...
java com.ems.Main

pause