@echo off
echo ========================================================
echo Coconut Release APK Installation Guidance
echo ========================================================
echo.
echo Coconut 2.0 does not automatically install via ADB.
echo The user installs the signed production APK manually to ensure device safety.
echo.
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo [FOUND] Production Release APK:
    echo %CD%\app\build\outputs\apk\release\app-release.apk
    echo.
    echo Manual installation instructions:
    echo 1. Transfer 'app-release.apk' to your Android phone (via USB, Quick Share, or storage).
    echo 2. On your device, tap the APK in Files / Downloads to install in-place.
    echo 3. Existing snapped apps and configuration in Room v5 will be safely retained.
    echo 4. Grant Accessibility and VPN permissions inside Coconut when prompted.
) else (
    echo [ERROR] No release APK found. Run build_release.bat first.
)
echo.
pause
