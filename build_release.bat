@echo off
echo ========================================================
echo Building Coconut - Release APK (Signed)
echo ========================================================
call gradlew.bat assembleRelease
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Release APK built and signed successfully at:
    echo app\build\outputs\apk\release\app-release.apk
) else (
    echo.
    echo [ERROR] Release build failed with exit code %ERRORLEVEL%
)
pause

