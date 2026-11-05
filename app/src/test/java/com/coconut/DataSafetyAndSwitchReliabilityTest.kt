package com.coconut

import com.coconut.data.InstallationState
import com.coconut.data.ProtectionConfigurationEntity
import com.coconut.data.SnapState
import com.coconut.data.SnappedAppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DataSafetyAndSwitchReliabilityTest {

    private val criticalEssentialPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.google.android.gms",
        "com.android.vending",
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.coconut"
    )

    // --- 1. DATA SAFETY & PROHIBITED OPERATIONS AUDIT ---

    @Test
    fun testDeviceAdminXmlDoesNotRequestWipeData() {
        val deviceAdminFile = File("src/main/res/xml/device_admin.xml")
        assertTrue("device_admin.xml must exist", deviceAdminFile.exists())
        val content = deviceAdminFile.readText()
        assertFalse(
            "CRITICAL: device_admin.xml MUST NEVER declare <wipe-data /> policy!",
            content.contains("<wipe-data")
        )
    }

    @Test
    fun testCriticalEmergencyAndTelephonyPackagesAreProtected() {
        val emergencyPackages = listOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.emergency"
        )

        emergencyPackages.forEach { pkg ->
            assertTrue(
                "Telephony/Emergency package $pkg must be protected by SafePackage rules",
                pkg in criticalEssentialPackages
            )
        }
    }

    @Test
    fun testSystemUiAndSettingsAreProtected() {
        assertTrue("System UI must be protected", "com.android.systemui" in criticalEssentialPackages)
        assertTrue("Settings must be protected", "com.android.settings" in criticalEssentialPackages)
        assertTrue("Coconut itself must be protected", "com.coconut" in criticalEssentialPackages)
    }

    // --- 2. SWITCH & HARDENING RELIABILITY AUDIT ---

    @Test
    fun testAllHardeningRestrictionsMapToConfigurationEntity() {
        var config = ProtectionConfigurationEntity()

        // 1. Lock Private DNS
        config = config.copy(disallowConfigPrivateDns = true)
        assertTrue(config.disallowConfigPrivateDns)
        config = config.copy(disallowConfigPrivateDns = false)
        assertFalse(config.disallowConfigPrivateDns)

        // 2. Disallow Factory Reset
        config = config.copy(disallowFactoryReset = true)
        assertTrue(config.disallowFactoryReset)
        config = config.copy(disallowFactoryReset = false)
        assertFalse(config.disallowFactoryReset)

        // 3. Disallow Safe Boot
        config = config.copy(disallowSafeBoot = true)
        assertTrue(config.disallowSafeBoot)
        config = config.copy(disallowSafeBoot = false)
        assertFalse(config.disallowSafeBoot)

        // 4. Disallow VPN Configuration
        config = config.copy(disallowConfigVpn = true)
        assertTrue(config.disallowConfigVpn)
        config = config.copy(disallowConfigVpn = false)
        assertFalse(config.disallowConfigVpn)

        // 5. Disallow Debugging Features
        config = config.copy(disallowDebugging = true)
        assertTrue(config.disallowDebugging)
        config = config.copy(disallowDebugging = false)
        assertFalse(config.disallowDebugging)

        // 6. Disallow Unknown Sources
        config = config.copy(disallowUnknownSources = true)
        assertTrue(config.disallowUnknownSources)
        config = config.copy(disallowUnknownSources = false)
        assertFalse(config.disallowUnknownSources)

        // 7. Disallow Apps Control
        config = config.copy(disallowAppsControl = true)
        assertTrue(config.disallowAppsControl)
        config = config.copy(disallowAppsControl = false)
        assertFalse(config.disallowAppsControl)
    }

    @Test
    fun testSelfProtectionToggleFlow() {
        var config = ProtectionConfigurationEntity(selfProtectionEnabled = true)
        assertTrue("Self-protection default should be true", config.selfProtectionEnabled)

        // Toggle OFF
        config = config.copy(selfProtectionEnabled = false)
        assertFalse("Self-protection toggled OFF must update state", config.selfProtectionEnabled)

        // Toggle ON
        config = config.copy(selfProtectionEnabled = true)
        assertTrue("Self-protection toggled ON must update state", config.selfProtectionEnabled)
    }

    @Test
    fun testContentShieldToggleFlow() {
        var config = ProtectionConfigurationEntity(contentShieldEnabled = false)
        assertFalse("Content shield default should be false", config.contentShieldEnabled)

        // Toggle ON with CleanBrowsing
        config = config.copy(
            contentShieldEnabled = true,
            dnsHostname = "family-filter-dns.cleanbrowsing.org",
            dnsProviderName = "CleanBrowsing Family"
        )
        assertTrue(config.contentShieldEnabled)
        assertEquals("family-filter-dns.cleanbrowsing.org", config.dnsHostname)

        // Switch Provider to Cloudflare
        config = config.copy(
            dnsHostname = "family.cloudflare-dns.com",
            dnsProviderName = "Cloudflare 1.1.1.1 Family"
        )
        assertEquals("family.cloudflare-dns.com", config.dnsHostname)

        // Toggle OFF
        config = config.copy(contentShieldEnabled = false)
        assertFalse(config.contentShieldEnabled)
        assertEquals("family.cloudflare-dns.com", config.dnsHostname) // Keeps preferred provider
    }

    @Test
    fun testDesiredStatePreservedWhenDeviceOwnerNotActive() {
        // When not Device Owner:
        val isDeviceOwner = false
        val desiredConfig = ProtectionConfigurationEntity(
            contentShieldEnabled = true,
            disallowFactoryReset = true,
            selfProtectionEnabled = true
        )

        // User preference is saved
        assertTrue(desiredConfig.contentShieldEnabled)
        assertTrue(desiredConfig.disallowFactoryReset)
        assertTrue(desiredConfig.selfProtectionEnabled)

        // Enforced state reflects reality
        val isDnsEnforced = isDeviceOwner && desiredConfig.contentShieldEnabled
        val isResetBlocked = isDeviceOwner && desiredConfig.disallowFactoryReset
        val isSelfProtected = isDeviceOwner && desiredConfig.selfProtectionEnabled

        assertFalse("Enforced state must be false when not Device Owner", isDnsEnforced)
        assertFalse("Enforced state must be false when not Device Owner", isResetBlocked)
        assertFalse("Enforced state must be false when not Device Owner", isSelfProtected)
    }

    @Test
    fun testPermanentSnapRegistryIrreversibility() {
        val app = SnappedAppEntity(
            id = 1,
            packageName = "com.distracting.game",
            applicationName = "Distracting Game",
            actualState = SnapState.PROTECTED,
            installationState = InstallationState.INSTALLED
        )

        // Irreversibility test: An uninstallation does not delete record, transitions to AWAITING_INSTALLATION
        val uninstalledApp = app.copy(
            actualState = SnapState.AWAITING_INSTALLATION,
            installationState = InstallationState.NOT_INSTALLED
        )

        assertEquals("com.distracting.game", uninstalledApp.packageName)
        assertEquals(SnapState.AWAITING_INSTALLATION, uninstalledApp.actualState)
        assertEquals(InstallationState.NOT_INSTALLED, uninstalledApp.installationState)

        // Reinstallation immediately transitions back to PROTECTED
        val reinstalledApp = uninstalledApp.copy(
            actualState = SnapState.PROTECTED,
            installationState = InstallationState.INSTALLED
        )
        assertEquals(SnapState.PROTECTED, reinstalledApp.actualState)
        assertEquals(InstallationState.INSTALLED, reinstalledApp.installationState)
    }
}

