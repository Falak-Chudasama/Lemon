@echo off
echo ========================================================
echo Coconut Device Owner Provisioning Script
echo ========================================================
echo.
echo [DATA SAFETY GUARANTEE]
echo Coconut NEVER wipes, resets, erases, or deletes any personal data,
echo photos, accounts, or installed applications on your phone.
echo.
echo Requirements for Device Owner via ADB:
echo  1. Android device connected via USB with USB Debugging enabled.
echo  2. Target device must have no accounts added yet (Android OS security constraint).
echo.
echo [SAFETY WARNING]
echo If this is your personal primary device with existing accounts:
echo DO NOT factory reset your device and DO NOT remove your accounts.
echo Coconut will operate safely in standard mode on top of your OS
echo without risking any personal data.
echo.
echo Checking connected ADB devices...
adb devices
echo.
set /p CONFIRM="Proceed with non-destructive provisioning? (Y/N): "
if /i not "%CONFIRM%"=="Y" (
    echo Aborted by user.
    pause
    exit /b 0
)

echo.
echo Installing Coconut APK...
if exist "app\build\outputs\apk\release\app-release.apk" (
    adb install -r app\build\outputs\apk\release\app-release.apk
) else (
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        adb install -r app\build\outputs\apk\debug\app-debug.apk
    ) else (
        echo [ERROR] No APK found! Run build_debug.bat or build_release.bat first.
        pause
        exit /b 1
    )
)

echo.
echo Attempting to set Coconut as Device Owner...
adb shell dpm set-device-owner com.coconut/.policy.CoconutDeviceAdminReceiver

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================================
    echo [SUCCESS] Coconut is now provisioned as Device Owner!
    echo ========================================================
    echo Full hardware-level policy enforcement is active.
    echo You may now open Coconut on your device.
) else (
    echo.
    echo ========================================================
    echo [NOTICE] Device Owner could not be assigned.
    echo ========================================================
    echo Android OS requires that no accounts exist to set Device Owner via ADB.
    echo.
    echo DATA SAFETY REMINDER:
    echo DO NOT wipe or reset your device!
    echo Coconut runs safely on your device in standard mode.
    echo Your preferences, app blacklists, and DNS configurations are saved
    echo locally in Coconut without compromising your phone or personal data.
)
echo.
pause
