# Coconut Android — Google Play Release Readiness & Policy Compliance

**Date**: November 7, 2026  
**Package Identifier**: `com.coconut`  
**Distribution Track**: Google Play Store Publishing Audit  

---

## 1. Executive Summary

Publishing Coconut on the Google Play Store is the primary long-term distribution solution to resolve sideloading friction and Google Play Protect automated warnings. This document provides a complete audit of Coconut's compliance with Google Play Developer Program Policies, with specific focus on the **Accessibility API Policy**, **VpnService Policy**, **Data Safety declarations**, and **Target SDK requirements**.

---

## 2. Accessibility API Policy Compliance (Snap Feature)

Google Play enforces strict policies regarding the `AccessibilityService` API ([Google Play Policy Reference](https://support.google.com/googleplay/android-developer/answer/10964491)).

### 2.1 Tool Classification (`isAccessibilityTool`)
- **Policy Rule**: Only applications primarily designed to support individuals with disabilities may set `android:isAccessibilityTool="true"` in their service configuration or store listing.
- **Coconut Posture**: Coconut is a **personal self-control and focus application**, not an assistive tool for people with disabilities.
- **Compliance Status**: `android:isAccessibilityTool="false"` (omitted from XML metadata). Coconut **never** falsely represents itself as assistive technology.

### 2.2 Prominent In-App Disclosure & Affirmative Consent
- **Policy Rule**: Apps using Accessibility APIs for non-disability purposes must present a prominent in-app disclosure before the user navigates to the Android Accessibility settings screen. It cannot be buried in Terms of Service or Privacy Policies.
- **Implemented in Coconut 2.0**:
  - A prominent disclosure dialog is presented in `SnapScreen.kt` when enabling the Snap service or tapping to snap an application.
  - Explicitly states:
    1. **What is observed**: Foreground application launch events (package names only).
    2. **Why it is needed**: To match against user-chosen Snapped applications.
    3. **What is NOT collected**: Screen text, passwords, messages, keystrokes, personal data, or usage logs.
    4. **Action taken**: Issues `GLOBAL_ACTION_HOME` and presents the Coconut blocking screen.
    5. **Affirmative consent**: Requires the user to explicitly tap *"Agree & Continue"* before launching system settings.

### 2.3 Strict Mode Audit & Settings Policy Compliance
- **Policy Rule**: Google's Accessibility policy explicitly states:
  > *"Accessibility APIs cannot be used to prevent users from disabling or uninstalling any app or service, or stopping users or apps from tapping or interacting with other apps or services, unless authorized by a parent or guardian via parental control apps or authorized administrators via enterprise management software."*
- **Previous Violation in Coconut 1.x**: Coconut previously intercepted Android Settings to prevent users from disabling Accessibility or uninstalling Coconut.
- **Remediation in Coconut 2.0**:
  - **Removed all Settings-blocking actions**: Coconut no longer traverses `rootInActiveWindow` looking for "Settings" or "Uninstall".
  - **Removed `canRetrieveWindowContent`**: Set `android:canRetrieveWindowContent="false"` in `snap_accessibility_service_config.xml`.
  - **Policy-Compliant Strict Mode**: Strict Mode now operates **exclusively within Coconut's in-app UI**, locking in-app blocklist and DNS changes behind a PBKDF2-HMAC-SHA256 passcode and countdown timer. The user's ability to manage their device via Android OS Settings is left completely unhindered.

---

## 3. VpnService Policy Compliance (Local DNS Engine)

Google Play regulates applications declaring `android.net.VpnService` ([VpnService Policy Reference](https://support.google.com/googleplay/android-developer/answer/9421225)).

### 3.1 Legitimate Use Case Declaration
- **Permissible Category**: Coconut falls under the permitted use case: **"Device security / Local content filtering"**.
- **On-Device Filtering Guarantee**:
  - Coconut does **not** operate an external proxy or commercial VPN network.
  - The TUN interface routes exclusively virtual DNS endpoints (`10.255.0.2/32` and `fd00::2/128`).
  - No non-DNS traffic is captured or rerouted.
  - Lookups are checked locally against bundled HaGeZi threat/adult blocklists and forwarded upstream via the Android system's native `DnsResolver`.
- **User Notification**: Coconut presents a persistent foreground service notification (`Coconut Local DNS: Local domain filtering active on-device`) meeting Android VpnService visibility requirements.

---

## 4. Google Play Console Declaration Checklist

When submitting Coconut to the Google Play Console, the developer must complete the following declaration questionnaires:

### 4.1 Accessibility Service Declaration Form
1. **Does your app use the Accessibility API?**: `Yes`
2. **Is your app an Accessibility Tool?**: `No`
3. **Primary purpose**: Select `"Application Management / Productivity & Focus"`.
4. **Description text**:
   > *"Coconut uses the AccessibilityService API solely to detect when user-selected distracting applications enter the foreground. When a restricted app is launched, Coconut executes the Home global action and presents a blocking screen to help the user maintain digital focus. Coconut does not retrieve window content, does not inspect screen text or passwords, does not monitor settings, and transmits zero accessibility data off the device."*
5. **Video Demonstration Link**: Provide a YouTube/cloud link showing:
   - In-app prominent disclosure dialog.
   - User affirmative consent.
   - Enabling the service in Android Settings.
   - Launching a snapped app and seeing the Coconut blocking screen.

### 4.2 Data Safety Form
- **Data Collected**: `None`
- **Data Shared**: `None`
- **Personal Information**: None (No name, email, phone number, address).
- **Financial Information**: None.
- **Health / Fitness**: None.
- **Messages / Photos / Audio**: None.
- **App Activity / Usage**: None (Foreground transitions are processed transiently in volatile memory; no analytics or tracking).
- **Web Browsing**: None (DNS resolution is processed locally; no query logging leaves device).
- **Device Identifiers**: None.

### 4.3 App Access & Sensitive Permissions
- `QUERY_ALL_PACKAGES`: Justified under `"Application management"` / focus app blocking. The user selects from their installed apps to configure Snap.
- `RECEIVE_BOOT_COMPLETED`: Resumes local DNS service and database state upon device restart.
- `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`: Required for persistent `VpnService` operation.

---

## 5. Technical Specifications & Release Readiness

| Requirement | Target Standard | Coconut Status |
| :--- | :--- | :--- |
| **Target SDK** | Android 15 (API 35) | **Compliant** (`targetSdk = 35`, `compileSdk = 35`) |
| **Minimum SDK** | Android 10 (API 29) | **Compliant** (`minSdk = 29`) |
| **64-bit Architecture** | ARM64 & x86_64 native support | **Compliant** |
| **App Bundle Format** | Android App Bundle (`.aab`) | Ready (`./gradlew bundleRelease`) |
| **R8 / ProGuard** | Minification and resource shrinking | **Enabled** (`isMinifyEnabled = true`, `isShrinkResources = true`) |
| **Third-Party Trackers** | Zero trackers | **Verified 100% clean** (no Firebase, AdMob, or third-party analytics) |

---

## 6. Play Store Submission Roadmap

1. **Step 1**: Register or access the verified Google Play Console developer account.
2. **Step 2**: Record a short (< 2 min) screen demonstration video showing the Prominent Accessibility Disclosure and Snap blocking workflow.
3. **Step 3**: Generate a release bundle using `./gradlew bundleRelease`.
4. **Step 4**: Complete the Store Listing, Data Safety, Accessibility Declaration, and Content Rating questionnaires.
5. **Step 5**: Submit for standard Google Play Policy Review.

