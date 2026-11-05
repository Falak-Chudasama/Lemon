# Coconut — Privacy-First Native Android Device Policy Controller (DPC)

Coconut is a native, offline-first Android device-management application built in Kotlin and Jetpack Compose. It is designed from the ground up for privacy, extreme simplicity, and permanent hardware-enforced device management.

---

## 1. Absolute Device and User Data Safety Invariant

Coconut operates strictly **ON TOP OF** the existing Android operating system. It is a self-control and application-restriction layer, **NOT** a destructive device-management tool. 

### 1.1 Non-Destructive Operation
Coconut guarantees 100% device and personal data integrity:
- **Zero Wiping**: Under no circumstances does Coconut invoke `DevicePolicyManager.wipeData()`, trigger factory resets, recovery wipes, or device formatting. The `<wipe-data />` policy directive has been strictly purged from `device_admin.xml`.
- **Zero File Tampering**: Coconut never deletes, accesses, or modifies photographs, videos, documents, downloads, messages, contacts, or personal files.
- **Zero App Data Deletion**: Coconut restricts applications using Android's native package suspension (`DevicePolicyManager.setPackagesSuspended()`). Suspending an app restricts its launch capability at the OS kernel/framework level; it **never** clears application storage, user databases, or app data.
- **Zero Account Modification**: Coconut never deletes user accounts, Google accounts, or work accounts.
- **Zero Destructive Shell Commands**: Coconut never executes destructive ADB or shell scripts.
- **Non-Destructive Database Versioning**: Coconut uses structured, non-destructive SQLite migrations (`MIGRATION_1_2`, `MIGRATION_2_3`) that safely preserve the Snap Registry, user audit logs, and settings across all upgrades.

### 1.2 Protected Critical System Allowlist
To guarantee that the phone can never be bricked or locked out, `SafePackageHelper` enforces a non-suspendable allowlist for core system services:
- Android System UI (`com.android.systemui`)
- Active Home Launcher (resolved dynamically via `CATEGORY_HOME`)
- System Settings (`com.android.settings`, `com.google.android.settings`)
- Input Method Editor / Soft Keyboard (`InputMethodManager`)
- Telephony & Emergency Services (`com.android.phone`, `com.android.dialer`, `com.google.android.dialer`, `com.samsung.android.dialer`, `com.android.server.telecom`, `com.android.emergency`)
- Package Installer & Permissions (`com.android.packageinstaller`, `com.android.permissioncontroller`)
- Google Play Services (`com.google.android.gms`, `com.android.vending`)
- Coconut itself (`com.coconut`)

---

## 2. Core Functional Pillars

### 2.1 The Snap (Permanent & Irreversible App Suspension)
- **Frictionless Fingerprint Verification**: Tapping "Snap" triggers native biometric authentication (`BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`). Upon verification, the target package is permanently suspended via `dpm.setPackagesSuspended()`.
- **Fallback Confirmation**: If a device lacks biometric hardware or enrolled screen credentials, a dedicated confirmation dialog guarantees that functionality is never blocked.
- **Irreversible Enforcement**: Once an application is snapped, it cannot be unsuspended.
- **Launch Interception**: Any attempt to launch a snapped application is blocked by Android's package suspension mechanism, and Coconut's dedicated blocked dialog appears with a "Head Back" action.
- **Permanent Registry**: Uninstalling a snapped application preserves its registry record as `AWAITING_INSTALLATION`. If reinstalled, Coconut detects it immediately via `PackageReconcileReceiver` and auto-resuspends it.

### 2.2 Content Shield (Device-Wide Adult Content Filtering)
- Enforces device-wide adult-content filtering via native DNS-over-TLS (DoT, RFC 7858) across Wi-Fi and mobile networks.
- **Default Provider**: CleanBrowsing Family Filter (`family-filter-dns.cleanbrowsing.org`).
- **Alternative Provider**: Cloudflare 1.1.1.1 for Families (`family.cloudflare-dns.com`).
- **Private DNS Lock**: Enforces `UserManager.DISALLOW_CONFIG_PRIVATE_DNS` to prevent tampering with Private DNS settings in Android Settings.
- **Live Connectivity Testing**: Features a built-in TCP port 853 socket diagnostic tool that validates DoT connectivity and displays round-trip latency.

### 2.3 Coconut Self-Protection & Device Hardening
- **Anti-Uninstallation**: Enforces `dpm.setUninstallBlocked(admin, "com.coconut", true)` to block removal of Coconut from the device.
- **Safe Boot Lock**: Enforces `UserManager.DISALLOW_SAFE_BOOT` to block booting into Safe Mode.
- **Anti-Bypass Reset Lock**: Enforces `UserManager.DISALLOW_FACTORY_RESET` to prevent wiping the phone to circumvent restrictions.
- **VPN Tamper Lock**: Enforces `UserManager.DISALLOW_CONFIG_VPN` to block third-party VPN overrides.
- **USB Debugging Lock**: Enforces `UserManager.DISALLOW_DEBUGGING_FEATURES` to seal ADB access once configured.
- **Sideloading Lock**: Enforces `UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY` to prevent unverified APK installations.
- **App Control Lock**: Enforces `UserManager.DISALLOW_APPS_CONTROL` to prevent force-stopping or modifying apps.

---

## 3. Design, Typography & Branding

Coconut features a minimal, sleek, high-contrast aesthetic built with Jetpack Compose:
- **Typography**:
  - Headings & Titles: **Host Grotesk** (`res/font/host_grotesk_bold.ttf`).
  - Body & Labels: **Urbanist** (`res/font/urbanist_medium.ttf`, `res/font/urbanist_regular.ttf`).
  - Fully offline, embedded font assets requiring zero network connectivity.
- **Color Palette & Accents**:
  - Deep Coconut Espresso (`#603913`): High-contrast primary branding.
  - Coconut Shell Bronze (`#8B5E3C`): Warm secondary tone.
  - Caramel Husk (`#A97C50`): Tertiary warm highlight.
  - Coconut Cream (`#F3E5D8`): Container backgrounds and card surfaces.
  - Coconut Meat White (`#F5F8FA`): High-clarity light surface.
  - Protected Green (`#2E7D32`): Active hardware-enforcement indicator.
  - Snapped Red (`#D32F2F`): Permanent snap locked badge.
  - Awaiting Amber (`#F57C00`): Saved preference / pending Device Owner indicator.
- **Performance Architecture**:
  - High-speed `AppIconCache` (in-memory LRU) offloads icon decoding from the main UI thread to `Dispatchers.IO`, preventing UI frame drops and freezing.

---

## 4. Provisioning Device Owner Mode Safely

Coconut distinguishes between **Device Owner Mode** (full hardware enforcement) and **Standard Mode** (preference persistence with pending enforcement).

### Safety Principles
1. **Never factory-reset your device to enable Device Owner mode.**
2. **Never delete your personal accounts just to satisfy ADB constraints.**
3. Coconut operates safely in Standard Mode on devices with accounts, preserving all user configurations locally without risking data loss.

### Method A: ADB Non-Destructive Provisioning (Fresh/Test Devices)
On a device with no accounts configured:
1. Connect via USB with **USB Debugging** enabled.
2. Run the Windows batch script:
   ```cmd
   provision_device_owner.bat
   ```
   Or execute via ADB:
   ```cmd
   adb install -r app\build\outputs\apk\release\app-release.apk
   adb shell dpm set-device-owner com.coconut/.policy.CoconutDeviceAdminReceiver
   ```
3. Verify status:
   ```cmd
   adb shell dumpsys device_policy | findstr "mDeviceOwner"
   ```

### Method B: QR-Code Out-of-the-Box Provisioning (New Devices)
On a brand-new, unconfigured device out of the box:
1. Tap the initial Welcome screen 6 times consecutively to activate the built-in QR Code Scanner.
2. Connect to Wi-Fi when prompted.
3. Scan an administrative QR code containing:
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.coconut/.policy.CoconutDeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://your-domain.com/downloads/Coconut-release.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "<SHA-256 CHECKSUM>",
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false
}
```

---

## 5. Comprehensive Switch & Control Audit Matrix

Every interactive component, switch, toggle, and dialog in Coconut has been verified for 100% responsiveness and reliability:

| Component | Control / Action | Architecture Path | Desired State | Enforced State | Reliability Verification |
|---|---|---|---|---|---|
| **The Snap** | "Snap" Action Button | `SnapScreen -> BiometricAuthHelper -> MainViewModel.executeSnap -> CoconutRepository.snapApp -> DPM.setPackagesSuspended` | `actualState = PROTECTED` | Package suspended via Android PM | **VERIFIED**: Requires fingerprint; fallback to confirmation dialog if no biometrics enrolled. Instant update to "Snapped" locked badge. |
| **The Snap** | Category Filter Chips | `SnapScreen -> onCategoryChange -> MainViewModel.setSelectedCategory -> State update` | Filter category string | Filtered list displayed | **VERIFIED**: All 8 categories filter smoothly. |
| **The Snap** | Search Bar | `SnapScreen -> OutlinedTextField -> MainViewModel.setSearchQuery` | Search query string | Filtered app list | **VERIFIED**: Real-time filtering with clear (X) action. |
| **Content Shield** | Master Filter Switch | `ContentShieldScreen -> onToggleShield -> MainViewModel.toggleContentShield -> CoconutRepository.setContentShield -> DPM.setGlobalPrivateDnsModeSpecifiedHost` | `contentShieldEnabled = true/false` | `dpm.getGlobalPrivateDnsMode() == 3` | **VERIFIED**: Switch toggles immediately. Displays "Enforced" when DO, or "Pending DO" when not DO. |
| **Content Shield** | Provider Radio Selection | `ContentShieldScreen -> onSelectProvider -> MainViewModel.selectDnsProvider -> CoconutRepository.setContentShield` | `dnsHostname = CleanBrowsing / Cloudflare` | Live Private DNS host matches selected hostname | **VERIFIED**: Switching between CleanBrowsing and Cloudflare updates instantly. |
| **Content Shield** | "Test DNS Connectivity" | `ContentShieldScreen -> onTestDns -> MainViewModel.testDnsConnectivity -> DnsConnectivityTester.testProvider` | Diagnostic probe | Latency (ms) and IP reported | **VERIFIED**: Background TCP socket test on port 853; displays live latency. |
| **Settings** | Prevent Coconut Uninstallation | `SettingsScreen -> onToggleSelfProtection -> MainViewModel.toggleSelfProtection -> CoconutRepository.setSelfProtection -> DPM.setUninstallBlocked` | `selfProtectionEnabled = true/false` | `dpm.isUninstallBlocked() == true` | **VERIFIED**: Saved locally; enforced via DPM when DO. |
| **Settings** | Lock Private DNS | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_CONFIG_PRIVATE_DNS)` | `disallowConfigPrivateDns = true/false` | `userManager.hasUserRestriction(DISALLOW_CONFIG_PRIVATE_DNS)` | **VERIFIED**: Prevents users from turning off DNS in Android Settings. |
| **Settings** | Disallow Factory Reset | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_FACTORY_RESET)` | `disallowFactoryReset = true/false` | `userManager.hasUserRestriction(DISALLOW_FACTORY_RESET)` | **VERIFIED**: Restricts device reset options in Settings. |
| **Settings** | Disallow Safe Boot | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_SAFE_BOOT)` | `disallowSafeBoot = true/false` | `userManager.hasUserRestriction(DISALLOW_SAFE_BOOT)` | **VERIFIED**: Blocks booting into Safe Mode. |
| **Settings** | Disallow VPN Config | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_CONFIG_VPN)` | `disallowConfigVpn = true/false` | `userManager.hasUserRestriction(DISALLOW_CONFIG_VPN)` | **VERIFIED**: Restricts VPN additions that bypass DNS. |
| **Settings** | Disallow Debugging | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_DEBUGGING_FEATURES)` | `disallowDebugging = true/false` | `userManager.hasUserRestriction(DISALLOW_DEBUGGING_FEATURES)` | **VERIFIED**: Blocks ADB access after setup. |
| **Settings** | Disallow Unknown Sources | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)` | `disallowUnknownSources = true/false` | `userManager.hasUserRestriction(DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY)` | **VERIFIED**: Blocks APK sideloading outside app stores. |
| **Settings** | Disallow App Control | `SettingsScreen -> onToggleRestriction -> MainViewModel.setHardeningRestriction -> CoconutRepository.setAdvancedRestriction -> DPM.addUserRestriction(DISALLOW_APPS_CONTROL)` | `disallowAppsControl = true/false` | `userManager.hasUserRestriction(DISALLOW_APPS_CONTROL)` | **VERIFIED**: Blocks clearing app storage or force stopping. |
| **Setup Wizard** | Device Admin Activate | `SetupWizardDialog -> onRequestDeviceAdmin -> MainActivity.requestDeviceAdmin -> DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` | Admin active | `dpm.isAdminActive() == true` | **VERIFIED**: Launches system admin activation; onResume refreshes UI. |
| **Setup Wizard** | Protection Alerts (Notifications) | `SetupWizardDialog -> onRequestNotificationPermission -> MainActivity.requestNotificationPermission` | Permission granted | `POST_NOTIFICATIONS == GRANTED` | **VERIFIED**: Prompts runtime permission dialog on Android 13+. |
| **Setup Wizard** | Copy ADB Command | `SetupWizardDialog -> ClipboardManager.setPrimaryClip` | Copied to clipboard | Toast confirmation | **VERIFIED**: Copies command to clipboard instantly. |
| **Reconcile** | Sync & Verify Policies | `HomeScreen -> onReconcileNow -> MainViewModel.reconcileNow -> CoconutRepository.reconcile -> PolicyReconciliationManager` | All desired policies | Live DPM state synchronized | **VERIFIED**: Resuspends drifted apps, verifies DoT host, updates audit log. |

---

## 6. Building & Release Signing

### 6.1 Build Commands
- **Build Debug APK**:
  ```cmd
  build_debug.bat
  ```
  Output: `app\build\outputs\apk\debug\app-debug.apk`

- **Build Signed Release APK**:
  ```cmd
  build_release.bat
  ```
  Output: `app\build\outputs\apk\release\app-release.apk`

### 6.2 Keystore Specifications
- **Keystore**: `coconut-release.jks`
- **Key Alias**: `coconut`
- **Key Password**: `coconut_release_key`
- **Signatures**: APK Signature Scheme v1, v2, v3 enabled

### 6.3 In-Place Updateability
Updates signed with `coconut-release.jks` install directly over existing versions (`adb install -r ...`). The `ACTION_MY_PACKAGE_REPLACED` broadcast ensures uninterrupted Device Owner authority and automatically re-verifies all active policies without dropping database records.

---

## 7. Verification Test Suite

Execute all automated unit and security tests:
```cmd
.\gradlew.bat test
```

### Verified Test Matrix:
| Test ID | Test Category | Description | Status |
|---|---|---|---|
| **SAFE-01** | Data Safety | Verified `device_admin.xml` does NOT declare `<wipe-data />` | **PASS** |
| **SAFE-02** | Data Safety | SafePackageHelper strictly protects emergency telephony & dialer packages | **PASS** |
| **SAFE-03** | Data Safety | SafePackageHelper strictly protects System UI, Settings, and Launcher | **PASS** |
| **SW-01** | Switch Reliability | Hardening restrictions mapping to Room configuration entity | **PASS** |
| **SW-02** | Switch Reliability | Content Shield toggle preserves desired state across provider switches | **PASS** |
| **SW-03** | Switch Reliability | Self-protection switch toggles cleanly between ON and OFF | **PASS** |
| **SW-04** | State Separation | Desired state is preserved locally when Device Owner is inactive | **PASS** |
| **SNAP-01** | The Snap | Permanent Snap Registry irreversibility (target uninstall preserves entry) | **PASS** |
| **SNAP-02** | The Snap | App discovery via PackageManager excludes Coconut | **PASS** |
| **SNAP-03** | The Snap | Package suspension via `dpm.setPackagesSuspended` | **PASS** |
| **DNS-01** | Content Shield | CleanBrowsing Family DoT hostname validation | **PASS** |
| **DNS-02** | Content Shield | Cloudflare Family DoT hostname validation | **PASS** |
| **DNS-03** | Content Shield | Socket connectivity testing over DoT port 853 | **PASS** |
| **RECON-01**| Reconciliation | Reconciliation resuspends drifted apps | **PASS** |
| **RECON-02**| Reconciliation | Reconciliation handles uninstalled packages safely | **PASS** |
