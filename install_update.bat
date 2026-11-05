@echo off
echo ========================================================
echo Coconut In-Place Application Update Script
echo ========================================================
echo.
echo This script performs a legitimate in-place update of Coconut.
echo It preserves Device Owner status, Snap Registry, and configuration.
echo.
if exist "app\build\outputs\apk\release\app-release.apk" (
    set APK_PATH=app\build\outputs\apk\release\app-release.apk
) else (
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
    ) else (
        echo [ERROR] No APK found. Build the project first.
        pause
        exit /b 1
    )
)

echo Installing update from: %APK_PATH%
adb install -r "%APK_PATH%"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Coconut updated in place successfully!
    echo Triggering policy reconciliation on device...
    adb shell am broadcast -a android.intent.action.MY_PACKAGE_REPLACED -n com.coconut/.policy.PackageReconcileReceiver
    echo.
    echo Verifying Device Owner status:
    adb shell dumpsys device_policy | findstr "admin=ComponentInfo{com.coconut"
) else (
    echo.
    echo [ERROR] In-place update failed!
)
pause

