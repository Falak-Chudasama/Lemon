# Coconut Android — Signing Key Security & Remediation Report

**Date**: November 7, 2026  
**Package Identifier**: `com.coconut`  
**Security Classification**: Critical Infrastructure Audit  

---

## 1. Executive Summary

During repository security auditing, the release signing keystore `coconut-release.jks` and its corresponding passwords were discovered to have been committed directly into Git version control on a publicly tracking remote.

This report establishes:
1. The exact exposure history and threat model.
2. Why immediate casual rotation is prohibited due to Android platform signature-continuity constraints.
3. The mitigation architecture implemented in Coconut 2.0.
4. A verified migration strategy using Android APK Signature Scheme v3 key rotation.

---

## 2. Exposure History & Current Security Posture

### 2.1 Git Repository Exposure
- **Remote Repository**: `https://github.com/Falak-Chudasama/Coconut.git`
- **Branch**: `main`
- **Originating Commit**: `eda5186e6292cca502eb0873ce2090a9a09beac7` (*"feat: initialize coconut-android application structure with core features and UI components"*)
- **Exposed Assets**:
  - Binary keystore: `coconut-release.jks` committed to repository root.
  - Plaintext passwords committed to `app/build.gradle.kts`:
    - `storePassword`
    - `keyPassword`
- **Current Remote State**: Commit `eda5186e` was pushed to `origin/main`. Consequently, anyone with read access to the GitHub repository must be assumed to have obtained the private signing key.

### 2.2 Active Certificate Details
- **Signer DN**: `CN=Coconut, OU=Mobile Security, O=Coconut Project, L=Unknown, ST=Unknown, C=US`
- **Key Algorithm**: RSA 2048-bit
- **Certificate SHA-256 Digest**:  
  `ba:9e:e3:e1:23:75:fe:7b:e6:d5:fa:da:a9:8f:7e:86:c0:d0:bd:7a:f0:cc:5f:16:0e:60:5c:bf:07:98:7a:80`
- **Certificate SHA-1 Digest**:  
  `6a:76:aa:ca:f9:c3:b5:9f:4b:1c:d5:e1:e4:b8:06:fd:5d:de:d5:08`
- **Certificate MD5 Digest**:  
  `9d:0f:16:b7:73:ca:b6:2f:34:4c:0a:16:b2:e3:6b:3c`

---

## 3. Threat Model & Risk Assessment

| Threat Vector | Severity | Analysis & Mitigations |
| :--- | :--- | :--- |
| **Malicious Sideload Update** | **High** | An attacker possessing the private key could sign a trojanized APK with package ID `com.coconut`. If an end-user downloads and installs this APK over their existing installation, Android's Package Manager will accept it as a legitimate update because signatures match. |
| **Sandbox Data Extraction by Other Apps** | **Zero** | Android enforces application sandboxing at the Linux kernel/UID level. Third-party apps installed on the same device cannot read Coconut's Room database or configuration files, even if the signing key is public. Only an app signed with the same key and declaring the same shared UID could access private storage. |
| **Device Compromise via Existing Install** | **Low** | The existing installed APK on the user's phone cannot be altered over the air without user interaction or physical ADB access. |

---

## 4. Why Casual Key Rotation Is Prohibited

Under standard Android package management:
1. When an application update is installed (`pm install -r` or sideload), Android checks whether the signing certificate of the new APK matches the certificate of the currently installed application (`signatures-match` check).
2. If the developer generates a new keypair and signs Coconut with it, the OS rejects the update:
   ```
   Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.coconut signatures do not match that of the new package]
   ```
3. To install a newly-keyed APK, the user would be forced to **uninstall** Coconut first.
4. **Critical Safety Constraint**: Because Coconut serves as a self-control tool and holds Device Administrator capabilities, uninstalling Coconut destroys all local databases, custom blocklists, DNS history, and device administration state. In locked self-control scenarios, forcing an uninstall can brick administrative configurations or lock out personal data.

Therefore, **signature continuity must be preserved** for all existing device installations.

---

## 5. Remediation Architecture Implemented

1. **Physical Keystore Eviction from Source Control**:
   - `coconut-release.jks` was deleted from the git working directory.
   - Root `.gitignore` was hardened with explicit patterns:
     ```gitignore
     *.jks
     *.keystore
     *.p12
     *.pem
     ```
2. **Secure External Backup**:
   - The authoritative keystore file has been securely relocated outside of the repository tree to:
     `C:\Users\ADMIN\.coconut\keystore\coconut-release.jks`
   - File permissions restrict access to the local user profile.
3. **Externalized Build Configuration**:
   - `app/build.gradle.kts` has been completely purged of hardcoded secrets.
   - Keystore path and passwords are read dynamically from environment variables or untracked `local.properties`:
     ```kotlin
     val configuredPath = System.getenv("COCONUT_KEYSTORE_PATH")
         ?: (project.findProperty("COCONUT_KEYSTORE_PATH") as? String)
         ?: localProperties.getProperty("COCONUT_KEYSTORE_PATH")
     ```
   - `local.properties` is strictly ignored by version control.

---

## 6. Long-Term Key Rotation Roadmap (APK Signature Scheme v3)

For future production and store distribution, Android provides a cryptographic rotation mechanism that preserves update compatibility while retiring compromised keys.

### 6.1 APK Signature Scheme v3 (`apksigner rotate`)
Introduced in Android 9.0 (API 28), APK Signature Scheme v3 supports cryptographic signing-key rotation with backward compatibility:
- **Lineage Proof**: The `apksigner` tool embeds a cryptographic proof-of-rotation record (lineage) signed by the old key authorizing the new key.
- **Verification on Android 9+ (API 28–35)**: Android recognizes the lineage proof and allows the newly signed APK to update over an app signed with the old key without triggering `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- **Legacy Devices (< API 28)**: Coconut's `minSdk` is API 29 (Android 10), meaning **100% of Coconut's target device fleet supports v3 rotation**.

### 6.2 Key Rotation Procedure (When Ready for Production Cutover)
1. **Generate New Cryptographically Secure Keypair**:
   ```bash
   keytool -genkeypair -v -keystore coconut-production-v2.jks \
     -alias coconut_v2 -keyalg RSA -keysize 4096 -validity 10000 \
     -dname "CN=Coconut Production, OU=Mobile Security, O=Coconut, C=US"
   ```
2. **Generate Rotation Lineage Record**:
   ```bash
   apksigner rotate \
     --out coconut.lineage \
     --old-keystore C:\Users\ADMIN\.coconut\keystore\coconut-release.jks \
     --old-key-alias coconut \
     --new-keystore C:\Users\ADMIN\.coconut\keystore\coconut-production-v2.jks \
     --new-key-alias coconut_v2
   ```
3. **Sign New Builds with Both Lineage and New Key**:
   ```bash
   apksigner sign \
     --in app-unsigned.apk \
     --out app-release.apk \
     --lineage coconut.lineage \
     --ks C:\Users\ADMIN\.coconut\keystore\coconut-production-v2.jks \
     --ks-key-alias coconut_v2
   ```

### 6.3 Google Play App Signing Migration
When publishing via Google Play:
- Enroll in **Google Play App Signing**.
- Google Play maintains the master app signing key in Google's secure key management service.
- The developer uses a separate **upload key** to push builds to the Play Console. If an upload key is compromised, Google Play allows the developer to reset the upload key via Play Console support without affecting users.

