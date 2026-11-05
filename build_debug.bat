@echo off
echo ========================================================
echo Building Coconut - Debug APK
echo ========================================================
call gradlew.bat assembleDebug
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Debug APK built successfully at:
    echo app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo [ERROR] Build failed with exit code %ERRORLEVEL%
)
pause

