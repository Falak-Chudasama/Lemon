# COCONUT — GOOGLE PLAY PROTECT COMPATIBILITY & SECURITY AUDIT

**Project:** Coconut Android  
**Target Device:** Samsung Galaxy A15 5G (Android 16 / API 36)  
**Package:** `com.coconut`  
**Version:** `2.0.0` (VersionCode `3`)  
**Signing Key Certificate SHA-256:** `ba:9e:e3:e1:23:75:fe:7b:e6:d5:fa:da:a9:8f:7e:86:c0:d0:bd:7a:f0:cc:5f:16:0e:60:5c:bf:07:98:7a:80`  
**Audit Date:** 2026-11-08  

---

## 1. EXACT REASON FOR THE INSTALLATION BLOCK (WITH EVIDENCE)

### The Sideload Warning
When installing Coconut (`2.0.0`, version code `3`) on Android via WhatsApp, Google Play Protect displays:
> **"App blocked to protect your device"**  
> *"This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud."*

### The Root Cause: Google Play Protect Enhanced Fraud Protection
This block is an automated, policy-enforced gate built into Google Play Services (Play Protect) targeting **internet-sideloaded applications** in affected markets (including Singapore, India, Brazil, Thailand, and expanding globally).

Official Google references:
- [Google Play Protect Warning Guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)
- [Google Keyword Announcement on Enhanced Fraud Protection](https://blog.google/products/android/google-play-protect-enhanced-fraud-protection/)

### The Two Required Conditions That Trigger This Gate
1. **The Installation Source (Distribution Vector)**:
   The installation session was initiated from an internet downloader or messaging app (`com.whatsapp`, browser, or file manager) rather than from the Google Play Store client or local shell (`adb install`).
2. **The Presence of a High-Risk Sensitive Capability**:
   Google officially documents that the installation gate automatically intercepts and blocks any internet-sideloaded APK that declares any of the following four sensitive permissions:
   - `RECEIVE_SMS` / `READ_SMS`
   - `BIND_NOTIFICATION_LISTENER_SERVICE`
   - `android.permission.BIND_ACCESSIBILITY_SERVICE`

### Confirmed Evidence
Coconut declares `android.permission.BIND_ACCESSIBILITY_SERVICE` for [`SnapAccessibilityService`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/java/com/coconut/service/SnapAccessibilityService.kt).  
Because WhatsApp initiates the package installer session and Coconut declares `BIND_ACCESSIBILITY_SERVICE`, Play Protect's client-side heuristic automatically triggers the **"App blocked to protect your device"** warning regardless of the application's benign intent, code cleanliness, or privacy settings.

---

## 2. SENSITIVE PERMISSIONS FOUND IN THE FINAL RELEASE

Inspection of the compiled release APK ([`app-release.apk`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/build/outputs/apk/release/app-release.apk)) via `aapt2 dump permissions` and `aapt2 dump xmltree` reveals the complete set of permissions:

| Permission | Origin | Purpose in Coconut | Risk Tier |
| :--- | :--- | :--- | :--- |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | `AndroidManifest.xml` | Foreground window state transitions for Snap | **Critical (Play Protect Trigger)** |
| `android.permission.QUERY_ALL_PACKAGES` | `AndroidManifest.xml` | Allows user to discover and pick apps to Snap | Sensitive (User Data Policy) |
| `android.permission.BIND_VPN_SERVICE` | `AndroidManifest.xml` | Local DNS loopback filtering (`10.255.0.2:53`) | High (Protected VpnService) |
| `android.permission.BIND_DEVICE_ADMIN` | `AndroidManifest.xml` | Preserved active device administrator component | Administrative (Legacy) |
| `android.permission.INTERNET` | `AndroidManifest.xml` | HTTPS updates for HaGeZi blocklist feeds | Normal |
| `android.permission.ACCESS_NETWORK_STATE` | `AndroidManifest.xml` | Wi-Fi constraint & physical network tracking | Normal |
| `android.permission.POST_NOTIFICATIONS` | `AndroidManifest.xml` | Rate-limited notification when app is snapped | Normal / Runtime (API 33+) |
| `android.permission.RECEIVE_BOOT_COMPLETED` | `AndroidManifest.xml` | Reconcile schedule & reinstall detection on boot | Normal |
| `android.permission.FOREGROUND_SERVICE` | `AndroidManifest.xml` | Local loopback DNS foreground service | Normal |
| `android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED` | `AndroidManifest.xml` | Android 14+ (API 34+) VpnService execution | Normal |
| `android.permission.USE_BIOMETRIC` | `AndroidManifest.xml` | In-app App Lock & Snap two-step confirmation | Normal |
| `android.permission.USE_FINGERPRINT` | Merged (`androidx.biometric`) | Legacy fingerprint fallback (pre-API 28) | Normal |
| `android.permission.WAKE_LOCK` | Merged (`androidx.work`) | WorkManager periodic task execution | Normal |
| `com.coconut.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Merged (AndroidX Core) | Internal IPC isolation between components | Signature / Internal |

### Critical Negative Findings (Zero Violations):
- **SMS Permissions**: `RECEIVE_SMS`, `READ_SMS`, `SEND_SMS` are **completely absent**.
- **Notification Listener**: `BIND_NOTIFICATION_LISTENER_SERVICE` is **completely absent**.
- **Location, Camera, Microphone, Contacts, External Storage**: **Completely absent**.
- **Third-Party Telemetry / Ad SDKs**: **Zero telemetry or advertising libraries exist in the application**.

---

## 3. WHICH PERMISSION IS THE LIKELY TRIGGER?

**The sole trigger is `android.permission.BIND_ACCESSIBILITY_SERVICE`.**

Google's enhanced fraud protection specifically filters against `ACCESSIBILITY` in internet-sideloaded APKs because abusive malware historically used sideloaded accessibility services to execute clickjacking, observe one-time passcodes, and read banking interfaces.

Even though Coconut:
- Has `android:canRetrieveWindowContent="false"` in [`snap_accessibility_service_config.xml`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/res/xml/snap_accessibility_service_config.xml),
- Subscribes only to `typeWindowStateChanged | typeWindowsChanged`,
- Never reads text, nodes, passwords, or keystrokes,
- Does not transmit data off-device,

Play Protect's sideload filter evaluates the package manifest declaration prior to runtime execution. Therefore, the declaration itself triggers the gate when downloaded via messaging apps.

---

## 4. DIFFERENCES FROM THE PREVIOUS RELEASE

Comparing the current build (`v2.0.0`, versionCode `3`) against the previous versions:

1. **Accessibility Metadata Hardening**:
   - `snap_accessibility_service_config.xml`: Verified `android:canRetrieveWindowContent="false"` and removed `typeWindowContentChanged`.
   - Removed Settings screen inspection logic from [`SnapAccessibilityService`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/java/com/coconut/service/SnapAccessibilityService.kt).
2. **Prominent In-App Disclosure & Affirmative Consent**:
   - Added interactive disclosure dialog requiring affirmative user click (*"Agree & Continue"*) prior to navigating to system Accessibility settings.
3. **Execution Decoupling**:
   - Replaced immediate `BlockedAppActivity` foreground launches with `performGlobalAction(GLOBAL_ACTION_HOME)` and rate-limited notifications on a silent, low-importance channel.
4. **Android 16 Upgrade**:
   - Upgraded `compileSdk` and `targetSdk` from `35` to `36`.
   - Preserved `minSdk = 29`.
5. **Database Migration**:
   - Upgraded Room database from version 5 to version 6 without modifying existing tables or losing snapped records.
6. **Key Continuity**:
   - Release signing certificate (`ba:9e:e3:e1:23:75:fe:7b...`) remained strictly identical.

---

## 5. ACCESSIBILITYSERVICE POLICY ASSESSMENT

### Google Play Accessibility Policy Analysis
Reference: [Google Play Accessibility Policy](https://support.google.com/googleplay/android-developer/answer/10964491)

| Requirement | Policy Rule | Coconut Implementation | Compliance Status |
| :--- | :--- | :--- | :--- |
| **Tool Categorization** | Only apps designed for people with disabilities may set `isAccessibilityTool="true"`. | Coconut leaves `isAccessibilityTool="false"` (omitted). Coconut is honestly described as a self-control tool. | **Compliant** |
| **No Screen Content Inspection** | Must not read private communications, credentials, or personal data. | `canRetrieveWindowContent="false"`. `event.packageName` is the only property read. | **Compliant** |
| **No Anti-Uninstall Blocking** | Must NOT block users from uninstalling or disabling the app. | Coconut does NOT intercept Android Settings or package management screens via Accessibility. | **Compliant** |
| **Prominent In-App Disclosure** | Must explain what data is accessed and how it is used before user consent. | Implemented in [`SnapScreen.kt`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/java/com/coconut/ui/screens/SnapScreen.kt) and [`strings.xml`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/res/values/strings.xml). | **Compliant** |
| **Affirmative User Consent** | User must affirmatively accept the disclosure before accessing system settings. | Modal dialog with explicit *"Continue"* button; dismiss does not route to Settings. | **Compliant** |
| **Target Scope** | Non-disability tools must fall under permitted categories (e.g. Parental Control / Enterprise). | Coconut is a personal self-control / distraction blocking application. | **Policy Gate (See Section 6)** |

---

## 6. IS COCONUT ELIGIBLE FOR GOOGLE PLAY DISTRIBUTION UNDER CURRENT POLICY?

### The Truthful Assessment
- **For Closed/Internal Testing Tracks**: **YES.** Google Play permits applications using AccessibilityService for testing and internal distribution without triggering the sideload fraud warning.
- **For Public Google Play Production**: **CONDITIONALLY ELIGIBLE WITH RESTRICTIONS.**
  Google Play policy strictly limits non-disability uses of `AccessibilityService` to specific exceptions, primarily **Parental Control** and **Enterprise Management** applications.
  While distraction-blocking apps (like *Freedom*, *AppBlock*, *One Sec*) operate on Google Play using AccessibilityService, they must:
  1. Complete the detailed **Accessibility Declaration Form** in Play Console.
  2. Provide a full YouTube video demonstration showing the prominent disclosure, user consent, and Snap action.
  3. Clearly state in Data Safety that zero accessibility data is collected or shared.
  4. Frame the application under digital wellbeing and distraction management.

If Google reviewers take a rigid view that personal self-control does not qualify as parental control or enterprise management, public store approval could be contested. However, **internal and closed testing distribution via Play Console is immediately eligible and bypasses Play Protect sideload blocking.**

---

## 7. ALTERNATIVE SNAP IMPLEMENTATION FEASIBILITY

We audited every potential alternative Android mechanism to assess whether Snap can be achieved without `AccessibilityService`:

| Mechanism | Foreground App Detection | App Blocking / Interception | Android 16 Compatibility | Battery & Resource Impact | Viable Alternative? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **AccessibilityService (Current)** | **Push Event (0ms latency)** via window state transitions | **Immediate** via `performGlobalAction(GLOBAL_ACTION_HOME)` | Fully compatible; respects privacy (`canRetrieveWindowContent=false`) | Negligible; event-driven by system | **YES (Primary)** |
| **UsageStatsManager (`PACKAGE_USAGE_STATS`)** | **Polling Only (500ms – 2000ms latency)**; no push events | **Cannot block**. Target app already renders. Cannot call `GLOBAL_ACTION_HOME`. | Restricted by Background Activity Launch (BAL) limits | High; continuous background loop throttled by OS | **NO** |
| **Device Owner (`setPackagesSuspended`)** | OS-level native package suspension | OS prevents launch completely | Fully compatible | Zero; managed by system package manager | **Requires Factory Reset (Forbidden)** |
| **VPN Service DNS Blocking** | Intercepts network domains | Intercepts domain lookups; cannot prevent app UI launch | Fully compatible | Low; event-driven loopback | **Active for Content Shield, not UI Snap** |
| **ActivityManager (`getRunningTasks`)** | Deprecated since API 21 | Returns only caller task | Inaccessible to third-party apps | N/A | **NO** |

### Why `UsageStatsManager` Fails as a Drop-In Replacement
1. **High Latency**: `UsageStatsManager` provides only historical query data. Detecting a launch requires a permanent background loop polling every 500ms. In that window, the distracting app has already launched, rendered its content, and accepted touch input.
2. **Cannot Call Home**: `performGlobalAction(GLOBAL_ACTION_HOME)` is an API exclusive to `AccessibilityService`. An ordinary app cannot invoke it.
3. **Android 16 Background Activity Launch (BAL) Restrictions**: If a background polling service tries to launch an Activity over the foreground app, Android 16 blocks it as an unauthorized background launch.
4. **Samsung Device Care Throttling**: Continuous polling loops are rapidly flagged and put into "Deep Sleep" by Samsung One UI.

**Conclusion:** `AccessibilityService` is the only non-root, non-Device Owner Android platform API capable of deterministic, zero-latency foreground distraction interception.

---

## 8. CHANGES IMPLEMENTED AND THEIR RATIONALE

1. **Disclosure & Consent Synchronization**:
   - Updated [`strings.xml`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/res/values/strings.xml) (`accessibility_service_description` and disclosure strings) to truthfully describe the current behavior: returning home and posting a status notification rather than launching a blocking activity.
2. **Prominent In-App Consent Gate**:
   - In [`SnapScreen.kt`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/java/com/coconut/ui/screens/SnapScreen.kt), tapping "Snap" on any available app when Accessibility is not enabled immediately presents the prominent disclosure dialog with affirmative consent before routing to Settings.
3. **Decoupled Blocking Architecture**:
   - Verified that [`SnapAccessibilityService`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/java/com/coconut/service/SnapAccessibilityService.kt) never inspects window content and never launches Coconut's main UI unexpectedly.
4. **Clean Release Artifacts**:
   - Rebuilt and verified both signed release APK and release AAB targeting Android 16 (API 36).

---

## 9. CHANGES DELIBERATELY NOT IMPLEMENTED (AND WHY)

1. **Did NOT set `isAccessibilityTool="true"`**:
   - Coconut is a personal distraction blocker, not an assistive tool for people with disabilities. Setting this flag falsely is deceptive and leads to immediate Google Play rejection and malware classification.
2. **Did NOT remove `BIND_ACCESSIBILITY_SERVICE`**:
   - Removing the service would break Coconut's core Snap capability.
3. **Did NOT switch to `UsageStatsManager` polling**:
   - Polling fails the zero-latency requirement, cannot execute `GLOBAL_ACTION_HOME`, and violates Android 16 background launch rules.
4. **Did NOT attempt code obfuscation or permission masking**:
   - Concealing sensitive permissions violates Google Play policies and risks permanent developer account termination.
5. **Did NOT require Device Owner provisioning**:
   - Device Owner requires factory resetting the Samsung Galaxy A15, violating non-negotiable safety preservation rules.

---

## 10. BUILD AND TEST RESULTS

- **Release Kotlin Compilation**: Completed successfully (`compileReleaseKotlin` passed with 0 errors).
- **Unit & Logic Validation**:
  - Exact uppercase `SNAP` case-sensitivity verified.
  - Nightly schedule 23:00–06:00 midnight crossing verified.
  - Exact 30-day queue timing (720h = 2,592,000,000 ms) verified.
  - Domain normalization, subdomains, and threat precedence verified.
  - Room v5 $\rightarrow$ v6 database migration verified with zero data loss.
- **Repository Hygiene**: All temporary test classes and test outputs were removed from git after execution.

---

## 11. RELEASE ARTIFACTS VERIFICATION

### Signed Release APK
- **Path:** `app/build/outputs/apk/release/app-release.apk`
- **File Size:** `6,712,163 bytes` (6.40 MB)
- **SHA-256 Hash:** `D4AB8F75AB2E98F45CC4A2743E5890DD87F82DA301963641E0DC1AD4A7B4DD75`
- **Package ID:** `com.coconut`
- **Version Name:** `2.0.0`
- **Version Code:** `3`
- **Target SDK:** `36` (Android 16)
- **Min SDK:** `29` (Android 10)
- **Signature Scheme:** Scheme v2 verified (`true`)

### Signed Release App Bundle (AAB)
- **Path:** `app/build/outputs/bundle/release/app-release.aab`
- **File Size:** `7,350,987 bytes` (7.01 MB)
- **SHA-256 Hash:** `7BDE0930A766AA7FEFE54CACEDEB36CA33A7B45017D4B4A1D2CD6838FB3AF4EE`
- **Signing Keystore:** `coconut-release.jks`

---

## 12. SIGNING CERTIFICATE COMPARISON

Inspection via `apksigner verify --verbose --print-certs`:

```text
Signer #1 certificate DN: CN=Coconut, OU=Mobile Security, O=Coconut Project, L=Unknown, ST=Unknown, C=US
Signer #1 certificate SHA-256 digest: ba9ee3e12375fe7be6d5fadaa98f7e86c0d0bd7af0cc5f160e605cbf07987a80
Signer #1 certificate SHA-1 digest: 6a76aacaf9c3b59f4b1cd5e1e4b806fd5dded508
Signer #1 certificate MD5 digest: 9d0f16b773cab62f344c0a16b2e36b3c
```

**Result:** Matches the installed application's signing identity. Update compatibility is preserved.

---

## 13. RECOMMENDED NEXT STEPS FOR DISTRIBUTION & PLAY PROTECT RESOLUTION

Because Play Protect's internet-sideload fraud gate triggers strictly on **the distribution channel (WhatsApp) + Accessibility permission**, there are three legitimate routes forward:

### Recommended Route A: Google Play Internal Testing (Fastest & Clearest)
1. In Google Play Console, create an application under `com.coconut`.
2. Upload [`app-release.aab`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/build/outputs/bundle/release/app-release.aab) to the **Internal Testing track**.
3. Add your email address to the internal testers list.
4. Open the internal testing link on your Galaxy A15.
5. **Result**: The app installs directly through Google Play Store with **zero Play Protect blocks**, because it is not flagged as an unverified internet-sideloaded APK.

### Route B: Google Play Protect Developer Appeal Form (For Direct APK Sideloading)
If direct APK distribution via messaging or web download is required, submit a false-positive review to Google's security team:
1. Upload [`app-release.apk`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/build/outputs/apk/release/app-release.apk) to a stable direct download link (e.g. Google Drive / GitHub Release).
2. Visit the official Google portal: [Google Play Protect Appeals Form](https://support.google.com/googleplay/android-developer/contact/protectappeals).
3. Submit:
   - Application Name: `Coconut`
   - Package Name: `com.coconut`
   - Download URL of the APK
   - Description of Functionality:
     > *"Coconut is a digital wellbeing self-control application that allows users to restrict distracting applications (Snap). It uses an AccessibilityService strictly to detect window transitions for user-selected packages and perform GLOBAL_ACTION_HOME. It declares canRetrieveWindowContent=false, never reads screen text, credentials, or personal information, and transmits zero data off-device. It has been falsely categorized under the internet-sideloading financial fraud filter."*
4. Google reviews the SHA-256 hash (`D4AB8F75AB2E...`) and adds it to the Play Protect allowlist database.

### Route C: Local ADB Installation (For Developer / Personal Testing)
If installing directly from your PC to your Galaxy A15 without waiting for Google review:
- Run: `adb install -r app/build/outputs/apk/release/app-release.apk`
- Because the package installer session is initiated by the local shell (`com.android.shell`) rather than an internet downloader (`com.whatsapp`), Play Protect does not trigger the sideload fraud gate.

