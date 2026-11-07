# Google Play Protect Warning & False-Positive Appeal Submission

**Reference Policy**: [Google Play Protect Warning Developer Guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)  
**Appeals Portal**: [Google Play Protect Dispute Form](https://support.google.com/googleplay/android-developer/answer/2992033)  
**Submission Date**: November 7, 2026  
**Application Name**: Coconut  
**Package Identifier**: `com.coconut`  

---

## 1. Application & Binary Verification Metadata

| Parameter | Authoritative Specification |
| :--- | :--- |
| **Package Name** | `com.coconut` |
| **Application Version** | `2.0.0` |
| **Version Code** | `3` |
| **Target SDK Version** | `35` (Android 15) |
| **Minimum SDK Version** | `29` (Android 10) |
| **APK Byte Size** | `6,355,997 bytes` (~6.36 MB) |
| **APK SHA-256 Digest** | `8300265EE631E44C3F3C68E26DCF1F2D94B97D4E73658A780762C20759E1B9A5` |
| **Signer Certificate DN** | `CN=Coconut, OU=Mobile Security, O=Coconut Project, L=Unknown, ST=Unknown, C=US` |
| **Signing Certificate SHA-256** | `ba:9e:e3:e1:23:75:fe:7b:e6:d5:fa:da:a9:8f:7e:86:c0:d0:bd:7a:f0:cc:5f:16:0e:60:5c:bf:07:98:7a:80` |
| **Signing Certificate SHA-1** | `6a:76:aa:ca:f9:c3:b5:9f:4b:1c:d5:e1:e4:b8:06:fd:5d:de:d5:08` |
| **Main Repository** | `https://github.com/Falak-Chudasama/Coconut.git` |

---

## 2. Description of the Warning & Installation Trigger

### 2.1 Displayed Warning Dialog
When an end-user receives the Coconut release APK via a messaging application (such as WhatsApp, Telegram) or web browser download in selected markets with Google Play Protect's enhanced anti-financial-fraud feature enabled, the installer displays:

> **"App blocked to protect your device"**  
> *"This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud."*

### 2.2 Technical Root Cause Analysis
Per Google's published guidance on internet-sideloaded applications, Google Play Protect automatically flags or blocks applications installed from non-store sources (web browsers, messaging clients, and file managers) if the application package declares sensitive capabilities—specifically `android.permission.BIND_ACCESSIBILITY_SERVICE`—to prevent financial fraud trojans from abusing accessibility permissions.

Coconut is a **legitimate personal productivity, digital wellbeing, and focus application**. It does **not** engage in financial fraud, credential harvesting, remote access, or screen inspection. This appeal formally demonstrates that Coconut's usage of sensitive APIs is strictly bounded, deterministic, user-controlled, and privacy-preserving.

---

## 3. Scope and Purpose of Sensitive Capabilities

Coconut declares three sensitive system components:

### 3.1 Android AccessibilityService (`SnapAccessibilityService`)
- **Exact Manifest Component**: `com.coconut.service.SnapAccessibilityService`
- **Guarding Permission**: `android.permission.BIND_ACCESSIBILITY_SERVICE`
- **Purpose**: Powering the **"Snap"** self-control feature.
- **Specific Implementation**:
  1. The user explicitly selects distracting applications from their installed list (e.g., social media or gaming applications) to add to their personal "Snapped" list.
  2. `SnapAccessibilityService` registers strictly for `typeWindowStateChanged` and `typeWindowsChanged` events to observe when an application is brought to the foreground.
  3. When an application on the user's Snapped list enters the foreground, the service immediately calls `performGlobalAction(GLOBAL_ACTION_HOME)` to minimize the distracting application and presents Coconut's `BlockedAppActivity`.
- **Strict Privacy Bounds (`snap_accessibility_service_config.xml`)**:
  - `android:canRetrieveWindowContent="false"`: **Explicitly disabled**. Coconut **cannot** read screen text, passwords, bank details, form fields, or UI hierarchies.
  - `android:isAccessibilityTool="false"`: Coconut makes no claim to be assistive technology for disabilities.
  - `typeWindowContentChanged`: **Disabled**. Coconut does not track on-screen content mutations.
  - **Zero settings interference**: Coconut does not intercept, inspect, or block Android OS Settings or uninstallation screens.

### 3.2 Android VpnService (`CoconutVpnService`)
- **Exact Manifest Component**: `com.coconut.dns.CoconutVpnService`
- **Guarding Permission**: `android.permission.BIND_VPN_SERVICE`
- **Purpose**: Powering the **"Content Shield"** on-device DNS filter.
- **Specific Implementation**:
  1. Configures a virtual DNS endpoint on loopback addresses (`10.255.0.2:53` and `fd00::2:53`).
  2. Routes **only** the virtual DNS server addresses (`10.255.0.2/32` and `fd00::2/128`). It does **not** route all device traffic (`0.0.0.0/0`).
  3. Evaluates DNS queries locally against bundled, open-source security feeds (HaGeZi TIF Mini threat intelligence, NSFW adult filtering, and Multi PRO Mini ad/tracker lists).
  4. Blocked domains receive an immediate synthetic `NXDOMAIN` response on-device. Allowed queries are forwarded to the network's normal upstream DNS resolver via Android's native `DnsResolver` API.
  5. **Zero External Tunneling**: Coconut does not operate external VPN servers or collect network telemetry. All DNS inspection occurs strictly in local device memory.

### 3.3 Package Querying (`QUERY_ALL_PACKAGES`)
- **Purpose**: Allows the user to browse their installed applications inside `SnapScreen` and select which distracting apps to restrict.
- **Storage**: Package names are stored strictly in an on-device SQLite database (`snapped_apps` table).

---

## 4. Prominent In-App Disclosure & Affirmative Consent

Coconut complies fully with Google Play's Prominent Disclosure and Consent requirements:
1. **Timing & Visibility**: Before routing the user to the system Accessibility settings screen, Coconut presents an explicit, non-dismissible `AccessibilityDisclosureDialog` directly in `SnapScreen`.
2. **Clear Explanation**:
   - Discloses that Accessibility is used to detect foreground launches of user-selected blocked apps.
   - Explicitly confirms that screen content, text, passwords, and personal messages are **never** inspected.
   - Explicitly confirms that no usage history or keystrokes are recorded.
   - Discloses that zero accessibility data leaves the device.
3. **Affirmative Action**: Requires the user to affirmatively click **"Agree & Continue"** to proceed. Clicking **"Not Now"** leaves the service disabled with no penalty.

---

## 5. Privacy, Data Safety & Telemetry Guarantees

- **No Remote Accessibility Data Collection**: Accessibility events are consumed transiently in memory to compare `event.packageName` against a local hash set. Zero bytes of accessibility data are persisted to disk or transmitted across network interfaces.
- **No Third-Party SDKs or Analytics**: Coconut contains zero advertising SDKs (no AdMob, Unity, etc.), zero third-party telemetry frameworks (no Firebase Analytics, Adjust, AppsFlyer, Sentry), and zero tracking pixels.
- **No Unrelated Permissions**:
  - `NO` SMS permissions (`READ_SMS`, `RECEIVE_SMS`, `SEND_SMS`)
  - `NO` Contact permissions (`READ_CONTACTS`, `WRITE_CONTACTS`)
  - `NO` Microphone or Audio permissions (`RECORD_AUDIO`)
  - `NO` Camera permissions (`CAMERA`)
  - `NO` Location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
  - `NO` Telephony permissions (`READ_PHONE_STATE`, `CALL_PHONE`)

---

## 6. Appeal Statement & Request to Google Play Protect Team

> **To the Google Play Protect Review Team:**
>
> We respectfully request a reclassification of the release APK for **Coconut** (`com.coconut`, version `2.0.0`, version code `3`, SHA-256: `8300265EE631E44C3F3C68E26DCF1F2D94B97D4E73658A780762C20759E1B9A5`).
>
> Coconut is an open, verified self-control and personal focus application. The warning displayed during internet sideloading ("This app can request access to sensitive data...") is triggered generically by the presence of `BIND_ACCESSIBILITY_SERVICE` in sideloaded packages.
>
> As audited in this appeal:
> 1. Coconut has configured `android:canRetrieveWindowContent="false"` in its service metadata, technically preventing it from reading window content, user text, passwords, or financial information.
> 2. Coconut uses Accessibility solely for deterministic foreground package matching against user-selected apps.
> 3. Coconut implements a prominent in-app disclosure with affirmative user consent.
> 4. Coconut includes zero third-party trackers, zero advertising libraries, and collects zero personal data.
>
> We request that Google Play Protect clear the automated block and whitelist this package and signing certificate as safe for users.

---

## 7. Submission Checklist for the Developer

Before submitting through the [Play Protect Dispute Form](https://support.google.com/googleplay/android-developer/answer/2992033):
- [x] Package Name verified: `com.coconut`
- [x] APK Version verified: `2.0.0` (Version Code `3`)
- [x] APK SHA-256 calculated: `8300265EE631E44C3F3C68E26DCF1F2D94B97D4E73658A780762C20759E1B9A5`
- [x] Signing Certificate Fingerprint verified: `ba:9e:e3:e1:23:75:fe:7b:e6:d5:fa:da:a9:8f:7e:86:c0:d0:bd:7a:f0:cc:5f:16:0e:60:5c:bf:07:98:7a:80`
- [x] Service metadata confirmed: `canRetrieveWindowContent="false"`
- [x] In-app disclosure screenshot prepared (`SnapScreen` with disclosure dialog)
- [x] Public GitHub repository reference included: `https://github.com/Falak-Chudasama/Coconut.git`

