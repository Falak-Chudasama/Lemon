# Google Play Protect Dispute & Classification Appeal (Final)

**Document Reference**: `docs/security/PLAY_PROTECT_APPEAL_FINAL.md`  
**Official Google Portal**: [Google Play Protect Dispute Submission](https://support.google.com/android/answer/2992033)  
**Developer Reference**: [Play Protect Warnings Developer Guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)  
**Submission Status**: Pending Manual Developer Review & Submission  

---

## 1. Application Identity & Binary Artifacts

| Parameter | Authoritative Value | Source / Tool Verification |
| :--- | :--- | :--- |
| **Package Identifier** | `com.coconut` | `AndroidManifest.xml` / `aapt2` |
| **Application Version Name** | `2.0.0` | `build.gradle.kts` |
| **Application Version Code** | `3` | `build.gradle.kts` |
| **Target SDK Version** | `36` (Android 16) | `aapt2 dump badging` |
| **Minimum SDK Version** | `29` (Android 10) | `aapt2 dump badging` |
| **APK Binary SHA-256** | `436F9C733CB742CE6CF774A218CB1D7BA6C471176AD62BDB8EBAC83A8A734697` | PowerShell `Get-FileHash` |
| **APK Byte Size** | `6,356,085 bytes` (~6.36 MB) | Filesystem size |
| **Signer Certificate DN** | `CN=Coconut, OU=Mobile Security, O=Coconut Project, L=Unknown, ST=Unknown, C=US` | `apksigner verify --print-certs` |
| **Signer Certificate SHA-256** | `ba:9e:e3:e1:23:75:fe:7b:e6:d5:fa:da:a9:8f:7e:86:c0:d0:bd:7a:f0:cc:5f:16:0e:60:5c:bf:07:98:7a:80` | `apksigner` |
| **Build Timestamp** | November 7, 2026 | Compilation timestamp |
| **Source Repository** | `https://github.com/Falak-Chudasama/Coconut.git` | Branch `main` |

---

## 2. Technical Explanation of Sensitive Capabilities

### 2.1 Purpose of Android AccessibilityService (`SnapAccessibilityService`)
Coconut is a personal digital wellbeing and self-control application. Its core feature—**The Snap**—enables users to set strict personal boundaries against distracting applications (such as social media or gambling apps).

1. **Why Foreground Detection is Required**: To prevent access to a user-restricted app, Coconut must know when that application enters the foreground. Android offers no non-privileged public intent or callback for real-time application launch detection without AccessibilityService.
2. **Deterministic, User-Configured Workflow**:
   - The user opens Coconut and explicitly checks which apps to restrict.
   - When a restricted app launches, Android dispatches a `typeWindowStateChanged` event containing the target package name (`event.packageName`).
   - If the package matches the user's local blocked list, Coconut calls `performGlobalAction(GLOBAL_ACTION_HOME)` to minimize the distracting app and displays [`BlockedAppActivity`](file:///c:/Users/ADMIN/OneDrive/CODES/Projects/Projects/Basket/fruits/coconut-android/app/src/main/java/com/coconut/ui/BlockedAppActivity.kt).
3. **Window Content Access is Disabled**:
   - In `snap_accessibility_service_config.xml`, Coconut declares:
     ```xml
     android:canRetrieveWindowContent="false"
     ```
   - Coconut **cannot** inspect or extract text, passwords, messages, bank details, or on-screen UI nodes.
4. **No Settings Traversal or Anti-Uninstall**:
   - Coconut contains zero code to intercept Android OS Settings or prevent the user from disabling the service or uninstalling the app.

### 2.2 Purpose of Android VpnService (`CoconutVpnService`)
Coconut's second core feature—**Content Shield**—is an on-device DNS filter providing local malware, phishing, adult content, and tracker blocking.

1. **Local Loopback Routing**: Coconut creates a virtual network interface that captures **only** virtual DNS endpoints (`10.255.0.2/32` and `fd00::2/128` on port 53). It explicitly does **not** route all device traffic (`0.0.0.0/0`).
2. **On-Device Policy Engine**: Lookups are checked against bundled, public, maintainer-verified blocklists (HaGeZi TIF Mini, HaGeZi NSFW, HaGeZi Multi PRO Mini). Blocked domains receive an immediate synthetic `NXDOMAIN` response.
3. **Upstream Forwarding via Native System Resolver**: Allowed queries are forwarded to the user's active physical network DNS resolver via `android.net.DnsResolver.rawQuery()`.
4. **Why No Cloud Backend Exists**: Processing occurs 100% in local volatile memory. Coconut operates no external proxy servers, logs zero queries remotely, and requires no cloud infrastructure.

---

## 3. Affirmative User Consent & Data Residency

1. **Prominent In-App Disclosure**: Before routing the user to system Accessibility settings, Coconut displays a dedicated Material 3 disclosure dialog directly in `SnapScreen`.
   - Explains that the service observes foreground app transitions solely to identify Snapped apps.
   - Guarantees that screen content, text, passwords, and personal messages are **never** read.
   - Confirms that zero data leaves the device.
2. **Affirmative Action**: The user must explicitly tap **"Agree & Continue"** to proceed to Android Settings. Tapping **"Not Now"** dismisses the prompt and leaves the service inactive.
3. **Local Data Storage**: User rules and Snapped packages are persisted exclusively in a local SQLite database (`AppDatabase` Room schema) on internal app storage.

---

## 4. Formal Security Assurances & Prohibited Capability Audit

Static analysis of the compiled binary verifies the total absence of high-risk capabilities:

- **NO SMS Access**: `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS` are absent.
- **NO Contacts Access**: `READ_CONTACTS`, `WRITE_CONTACTS` are absent.
- **NO Camera Access**: `CAMERA` is absent.
- **NO Microphone Access**: `RECORD_AUDIO` is absent.
- **NO Location Access**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` are absent.
- **NO Phone State Access**: `READ_PHONE_STATE`, `CALL_PHONE` are absent.
- **NO Analytics or Ad SDKs**: Contains zero advertising or telemetry libraries (no Firebase, Adjust, AdMob).
- **NO Dynamic Code Loading**: Zero usage of `DexClassLoader` or reflection-based code downloaders.
- **NO Remote Accessibility Transmission**: Zero network transmission of accessibility events.

---

## 5. Appeal Text for Submission to Google Play Protect

> **Subject**: Classification Review for Coconut (`com.coconut`) — Legitimate Self-Control Utility
>
> **Dear Google Play Protect Review Team,**
>
> We request a review of the automated installation block currently triggered when sideloading Coconut (`com.coconut`, Version `2.0.0`, Version Code `3`, Target SDK `36`, SHA-256: `436F9C733CB742CE6CF774A218CB1D7BA6C471176AD62BDB8EBAC83A8A734697`).
>
> The automated warning ("This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.") is triggered heuristically due to the declaration of `BIND_ACCESSIBILITY_SERVICE` in an internet-sideloaded package.
>
> Coconut is a genuine, verified personal focus and local DNS privacy tool:
> 1. In `res/xml/snap_accessibility_service_config.xml`, Coconut explicitly sets `android:canRetrieveWindowContent="false"`. It technically cannot access screen text, credentials, bank data, or UI elements.
> 2. The service observes strictly foreground package transitions (`typeWindowStateChanged`) to minimize user-selected distracting apps via `GLOBAL_ACTION_HOME`.
> 3. Coconut does not monitor or block Android OS Settings or uninstallation.
> 4. Coconut implements an in-app prominent disclosure dialog with affirmative user consent.
> 5. Coconut contains zero advertising SDKs, zero telemetry libraries, and transmits zero data off the device.
>
> We respectfully request that Google Play Protect whitelist this package and signing certificate.

---

## 6. Evidence Checklist for Manual Developer Submission

- [x] APK Binary: `app/build/outputs/apk/release/app-release.apk`
- [x] Exact SHA-256: `436F9C733CB742CE6CF774A218CB1D7BA6C471176AD62BDB8EBAC83A8A734697`
- [x] Signing Certificate Fingerprint: `ba:9e:e3:e1:23:75:fe:7b:e6:d5:fa:da:a9:8f:7e:86:c0:d0:bd:7a:f0:cc:5f:16:0e:60:5c:bf:07:98:7a:80`
- [x] Service Metadata: `canRetrieveWindowContent="false"`, `isAccessibilityTool="false"`
- [x] Prominent Disclosure: Integrated in `SnapScreen.kt`
- [x] Public GitHub Repository: `https://github.com/Falak-Chudasama/Coconut.git`

