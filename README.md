# Coconut

Coconut is a native, privacy-first Android personal self-control and local DNS filtering application built with Kotlin and Jetpack Compose.

It operates entirely on-device without cloud accounts, telemetry, external servers, root, Shizuku, or Device Owner requirements.

---

## 1. Core Capabilities

### 1.1 Snap (App Launch Interception)
- **Foreground Detection**: Utilizes an Android `AccessibilityService` (`SnapAccessibilityService`) to detect foreground window and package transitions in real time.
- **Immediate Return Home**: Automatically triggers `GLOBAL_ACTION_HOME` the instant a snapped application enters the foreground, dismissing the distracting application with zero artificial delay.
- **Debounce Model**: Initial blocking decisions have 0ms latency. Debounce (250ms) applies strictly to duplicate window/content events for the *same* in-progress launch. When the home launcher or any permitted application is foregrounded, the tracker resets immediately so subsequent launches are blocked instantly.
- **Blocked Screen**: Launches a dedicated, minimal `BlockedAppActivity` confirming the block with a single "Return Home" action.
- **Overlay Fallback**: Uses `TYPE_ACCESSIBILITY_OVERLAY` when direct Activity dispatch is constrained by the system.
- **Persistent Registry**: Retains Snap records in local Room database even if the target application is uninstalled. If reinstalled, Coconut detects the package via `PackageReconcileReceiver` and immediately re-enforces Snap.
- **Protected System Services**: Automatically protects phone dialers, settings, Coconut itself, active home launchers, and input methods from being snapped.

### 1.2 Strict Mode
- **Tamper Resistance**: Optional protection layer designed to prevent impulsive disabling of Coconut's enforcement.
- **Settings Hierarchy Inspection**: Monitors Android Settings windows strictly when Strict Mode is enabled and Android Settings (`com.android.settings`, `com.google.android.settings`) is in the foreground. Traversal is bounded (max depth 5, max 50 nodes) to prevent navigating into Coconut's App Info, Accessibility service toggle, VPN settings, or Private DNS configuration.
- **PBKDF2 Passcode Hashing**: Passcode hashed using `PBKDF2WithHmacSHA256` with 100,000 iterations and a cryptographically secure 16-byte random salt (`SecureRandom`).
- **Time-Delayed Emergency Unlock**: Requests to disable Strict Mode initiate an irreversible 15-minute countdown timer followed by a 10-minute temporary unlock window.

### 1.3 Local DNS Engine
- **Local Virtual TUN Interface**: Utilizes Android `VpnService` (`CoconutVpnService`) to create a DNS-only loopback tunnel (`10.255.0.1/32` IPv4, `fd00::1/128` IPv6).
- **IP-Prefix Routing**: Android `VpnService` routes IP traffic based on destination IP address prefixes (`addRoute("10.255.0.2", 32)` and `addRoute("fd00::2", 128)`), not by transport port. Standard device TCP/UDP internet traffic is never captured (`0.0.0.0/0` is not routed). Non-DNS packets addressed to the virtual endpoint are safely dropped.
- **UDP Port 53 Support**: Full support for IPv4 and IPv6 UDP DNS queries.
- **Mandatory IPv6 UDP Checksums**: Computes mandatory RFC 2460 / RFC 8200 UDP checksums across the IPv6 pseudo-header for all synthetic IPv6 responses. Computes IPv4 header and UDP checksums.
- **TCP over TUN Limitation & TCP RST**: Coconut supports UDP DNS only. It does not implement a full user-space TCP state machine (handshake, sequence/ACK tracking, sliding window, retransmissions). When a client resolver attempts a TCP connection to port 53 of the virtual DNS IP, Coconut immediately returns a standard TCP RST packet, prompting the client/OS resolver to fall back to UDP port 53 without hanging.
- **Deterministic Domain Matching**: Subdomain matching respects DNS label boundaries (e.g. `ads.example.com` matches `example.com`, but `badexample.com` does not). Supports IDN / Punycode domain normalization.
- **Local Policy Resolution Order**:
  1. Built-in HaGeZi Threat Intelligence (TIF Mini).
  2. Built-in HaGeZi Adult Content (NSFW).
  3. User Custom Allowlist (always overrides blocklists).
  4. User Custom Blocklist (exact domains and subdomains).
  5. Built-in HaGeZi Multi PRO Mini (Ads & Trackers).
  6. Default: Allow.
- **Synthetic NXDOMAIN**: Blocked queries receive immediate locally crafted `NXDOMAIN` (RFC 1035) responses and are never forwarded to the physical network.
- **Local Bounded LRU Cache**: Memory-bounded cache (1,024 entries) with monotonic time TTL tracking (`SystemClock.elapsedRealtime()`). Policy checks always precede cache lookup.
- **Physical Network Forwarding**: Permitted queries are forwarded through Android's underlying physical network using `DnsResolver.rawQuery` bound to `NET_CAPABILITY_NOT_VPN` to prevent tunnel routing loops.
- **Always-on VPN Support**: Declares `android.net.VpnService.SUPPORTS_ALWAYS_ON` in manifest without requiring VPN lockdown.
- **Zero DNS Logging**: DNS query history is completely disabled by default for absolute privacy.

---

## 2. Requirements

- **Android Version**: Android 10 (API level 29) or higher.
- **Permissions**:
  - Accessibility Service (`SnapAccessibilityService`): Required for foreground app launch detection.
  - VPN Service (`CoconutVpnService`): Required for local DNS filtering.
  - Post Notifications: Required for VPN foreground service status notification on Android 13+.
- **No Root or Shizuku**: Standard unprivileged Android installation.
- **No Developer Options**: ADB and USB debugging are not required.

---

## 3. Building from Source

### Prerequisites
- JDK 17 (LTS)
- Android SDK with Build-Tools 35.0.0 / 34.0.0 and Platform SDK 35

### Build Commands

#### Debug APK
```bash
.\gradlew.bat assembleDebug
```
Output: `app\build\outputs\apk\debug\app-debug.apk`  
Application ID: `com.coconut.debug` (does not collide with production)

#### Production Release APK
```bash
.\gradlew.bat assembleRelease
```
Output: `app\build\outputs\apk\release\app-release.apk`  
Application ID: `com.coconut`

---

## 4. Release Signing Configuration

Release signing credentials must not be committed to version control. The build system reads signing configuration through the following precedence:

1. Environment variables:
   - `COCONUT_KEYSTORE_PATH`
   - `COCONUT_KEYSTORE_PASSWORD`
   - `COCONUT_KEY_ALIAS`
   - `COCONUT_KEY_PASSWORD`
2. Gitignored `local.properties`:
   ```properties
   COCONUT_KEYSTORE_PATH=C:/path/to/keystore/coconut-release.jks
   COCONUT_KEYSTORE_PASSWORD=your_keystore_password
   COCONUT_KEY_ALIAS=your_key_alias
   COCONUT_KEY_PASSWORD=your_key_password
   ```
3. Secure user directory default:
   - `~/.coconut/keystore/coconut-release.jks`

If signing credentials are not configured, the release build fails explicitly rather than silently falling back to a debug certificate.

---

## 5. Manual Installation & Setup

1. **Install Release APK**:
   Transfer `app-release.apk` to your phone and tap to install. When updating an existing installation, Android preserves all existing database records and Room v5 migrations apply automatically.

2. **Enable Accessibility (For Snap)**:
   - Open Coconut and go to the **Snap** tab.
   - Tap the **Enable Accessibility** banner or navigate to `Android Settings > Accessibility > Installed Apps > Coconut`.
   - Toggle Coconut to **ON** and confirm.

3. **Enable VPN (For Local DNS)**:
   - Go to the **DNS** tab in Coconut.
   - Toggle **Local DNS** to **ON**.
   - Android will present a system VPN connection request dialog. Tap **OK**.

4. **Private DNS Compatibility (Important)**:
   - Open `Android Settings > Connections / Network > More connection settings > Private DNS`.
   - Set Private DNS to **Off** or **Automatic**.
   - *Note*: If set to "Strict / Private DNS provider hostname", Android's system resolver connects directly over TLS (port 853) to the external provider, bypassing local port 53 VPN DNS.

5. **Optional: Always-on VPN**:
   - Open `Android Settings > Connections / Network > More connection settings > VPN`.
   - Tap the gear icon next to **Coconut**.
   - Enable **Always-on VPN**. (Do **NOT** enable *Block connections without VPN*).

6. **Optional: Strict Mode**:
   - Go to the **Settings** tab.
   - Under **Strict Mode**, tap **Enable Strict Mode** and set a passcode.
   - To unlock, request unlock and wait for the 15-minute countdown to elapse.

---

## 6. Blocklists & Custom Rules

- **Bundled Snapshots**: Coconut includes offline snapshots in `assets/`:
  - `threat_blocklist.txt`: 10,000 high-confidence domains from HaGeZi TIF Mini.
  - `adult_blocklist.txt`: 10,000 adult domains from HaGeZi NSFW.
  - `adblock_blocklist.txt`: 10,000 ad/tracker domains from HaGeZi Multi PRO Mini.
- **Updates**: In the **DNS** tab, tap the refresh icon to atomically download and install the latest rules directly from HaGeZi's official GitHub repository over HTTPS.
- **Custom Rules**:
  - Add individual domains under the **Blocked** or **Allowed** tabs.
  - Use **Bulk Add** to paste multiple domains (one per line or space-separated).
  - Use **Export Rules** and **Import Rules** in Settings to backup or transfer domain lists via clipboard.

---

## 7. Performance & Resource Footprint

- **Memory (RAM)**:
  - Base process footprint: ~45MB to ~55MB on Android ART.
  - Built-in blocklists in heap: ~1MB to ~1.2MB per 10,000 domains (~3.3MB total when all three feeds are active).
  - DNS Cache: Bounded to 1,024 records (~150 KB).
  - App Icon Cache: Bounded to 150 bitmaps (~3 MB).
- **CPU & Battery**:
  - Only port 53 DNS queries to `10.255.0.2` and `fd00::2` enter the TUN interface.
  - Bulk internet traffic (media, streaming, downloads) routes directly over the physical network.
  - Under typical usage, battery consumption is comparable to standard on-device DNS filters (approximately 1–3% daily battery share depending on query frequency).

---

## 8. Privacy & Security Model

- **No Remote Servers**: Coconut runs no cloud backends, user tracking, or telemetry.
- **Zero Query Logging**: Domain lookups are evaluated in memory and discarded. No query log or lookup history is saved to disk.
- **No Destructive Operations**: Coconut never formats devices, wipes storage, or clears third-party application data.
- **Graceful Uninstallation**: Coconut does not lock itself against device administrator removal. The user can disable Accessibility and VPN services at any time (subject to Strict Mode cooldown if configured) and uninstall the app cleanly.

---

## 9. Known Limitations

- **Strict Private DNS**: When Android Private DNS is configured with a strict hostname (e.g. `dns.google`), Android's OS resolver sends encrypted queries directly over port 853 to the remote server, bypassing port 53 VPN DNS.
- **Browser Encrypted DNS (DoH)**: If a web browser (e.g., Chrome or Firefox) is explicitly configured to use "Secure DNS" (DNS-over-HTTPS) with an external provider, the browser makes encrypted queries over HTTPS (port 443), bypassing the system port 53 resolver. To filter browser traffic, set browser Secure DNS to "Default" or "Off".
- **Direct IP Traffic**: Direct socket connections made to raw IP addresses without a DNS hostname lookup bypass DNS filtering.
- **Accessibility Service Interruption**: On some aggressive vendor battery-saver implementations (e.g. Samsung Sleeping Apps, Xiaomi MIUI/HyperOS), Android may stop background accessibility services. Ensure Coconut is excluded from battery optimization.
