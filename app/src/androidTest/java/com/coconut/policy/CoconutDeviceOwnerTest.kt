package com.coconut.policy

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coconut.core.SafePackageHelper
import com.coconut.dns.DnsConnectivityTester
import com.coconut.dns.DnsProviders
import com.coconut.dns.DnsTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device Android Enterprise integration tests validating Device Owner policy enforcement,
 * permanent Snap suspension + uninstall blocking on disposable third-party apps,
 * TLS endpoint identification and certificate rejection,
 * Content Shield Private DNS enforcement, and system-level DNS resolver verification.
 */
@RunWith(AndroidJUnit4::class)
class CoconutDeviceOwnerTest {

    private lateinit var context: Context
    private lateinit var policyManager: CoconutPolicyManager
    private lateinit var safePackageHelper: SafePackageHelper
    private lateinit var dnsTester: DnsConnectivityTester

    // Disposable 3rd-party user application explicitly installed for destructive/suspension testing
    private val testCandidatePackage = "com.coconut.disposabletest"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        policyManager = CoconutPolicyManager(context)
        safePackageHelper = SafePackageHelper(context)
        dnsTester = DnsConnectivityTester()
    }

    @Test
    fun testDeviceOwnerIsActive() {
        assertTrue(
            "Coconut must be provisioned as Device Owner for policy enforcement",
            policyManager.isDeviceOwner()
        )
    }

    @Test
    fun testSelfProtectionIsReleasedByDefault() = runBlocking {
        val result = policyManager.releaseSelfProtectionIfActive()
        assertTrue(
            "Releasing self-protection should succeed or already be inactive",
            result is PolicyResult.AppliedAndVerified || !policyManager.isSelfProtectionActive()
        )
        assertFalse(
            "Coconut must not block its own uninstallation by default",
            policyManager.isSelfProtectionActive()
        )
    }

    @Test
    fun testSafePackageHelperProtectsCriticalInfrastructure() {
        assertTrue("Coconut itself must be protected from being snapped", safePackageHelper.isProtectedPackage("com.coconut"))
        assertTrue("System UI must be protected", safePackageHelper.isProtectedPackage("com.android.systemui"))
        assertTrue("Settings must be protected", safePackageHelper.isProtectedPackage("com.android.settings"))
        assertTrue("Google Play Store must be protected", safePackageHelper.isProtectedPackage("com.android.vending"))
        assertTrue("Android OS core must be protected", safePackageHelper.isProtectedPackage("android"))
    }

    @Test
    fun testPermanentSnapOnDisposablePackageAndEmergencyRecovery() = runBlocking {
        val isInstalled = try {
            context.packageManager.getPackageInfo(testCandidatePackage, 0)
            true
        } catch (e: Exception) {
            false
        }
        assertTrue(
            "Disposable test package $testCandidatePackage must be installed for audit verification",
            isInstalled
        )

        // 1. Enforce Snap (Native OS suspension + target app uninstall blocking)
        val snapResult = policyManager.suspendAndProtectPackage(testCandidatePackage)
        assertTrue(
            "Snap enforcement must return AppliedAndVerified: $snapResult",
            snapResult is PolicyResult.AppliedAndVerified
        )
        assertTrue(
            "Package must be reported as suspended by Android OS",
            policyManager.isPackageSuspended(testCandidatePackage)
        )
        assertTrue(
            "Package must be reported as uninstall-blocked by Android OS",
            policyManager.isPackageUninstallBlocked(testCandidatePackage)
        )

        // 2. Emergency Recovery Valve test
        val unsnapResult = policyManager.emergencyRecoveryUnsnap(testCandidatePackage)
        assertTrue(
            "Emergency unsnap must return AppliedAndVerified: $unsnapResult",
            unsnapResult is PolicyResult.AppliedAndVerified
        )
        assertFalse(
            "Package must no longer be suspended after emergency recovery",
            policyManager.isPackageSuspended(testCandidatePackage)
        )
        assertFalse(
            "Package must no longer be uninstall-blocked after emergency recovery",
            policyManager.isPackageUninstallBlocked(testCandidatePackage)
        )
    }

    @Test
    fun testContentShieldReversibleEnforcement() = runBlocking {
        val testHostname = DnsProviders.CLEAN_BROWSING.hostname

        // 1. Enable Content Shield
        val enableResult = policyManager.enableContentShield(testHostname)
        assertTrue(
            "Enabling Content Shield must return AppliedAndVerified: $enableResult",
            enableResult is PolicyResult.AppliedAndVerified
        )
        var dnsStatus = policyManager.getPrivateDnsStatus()
        assertTrue(
            "Content Shield must be active",
            dnsStatus.isEnforced
        )
        assertTrue(
            "Private DNS settings must be locked while Content Shield is active",
            dnsStatus.isConfigBlocked
        )
        assertEquals(
            "Active Private DNS host must match provider",
            testHostname,
            dnsStatus.host
        )

        // 2. Disable Content Shield (Guaranteed clean release)
        val disableResult = policyManager.disableContentShield()
        assertTrue(
            "Disabling Content Shield must return AppliedAndVerified: $disableResult",
            disableResult is PolicyResult.AppliedAndVerified
        )
        dnsStatus = policyManager.getPrivateDnsStatus()
        assertFalse(
            "Content Shield must be inactive",
            dnsStatus.isEnforced
        )
        assertFalse(
            "Private DNS lock must be cleared when Content Shield is disabled",
            dnsStatus.isConfigBlocked
        )
        assertNull(
            "Private DNS host must be null when opportunistic",
            dnsStatus.host
        )
        delay(1500)
    }

    @Test
    fun testLiveDnsOverTlsConnectivity() = runBlocking {
        val result = dnsTester.testProvider(DnsProviders.CLEAN_BROWSING.hostname)
        assertTrue(
            "DoT TLS handshake and query should succeed on live network: $result",
            result is DnsTestResult.Success
        )
        val success = result as DnsTestResult.Success
        assertTrue("TLS handshake must succeed", success.dotHandshakeVerified)
        assertTrue("DNS query must succeed", success.dnsQueryVerified)
        assertTrue("Latency must be recorded", success.latencyMs > 0)
    }

    @Test
    fun testTlsRejectionOfMismatchedHostname() = runBlocking {
        // Provider IP: 185.228.168.168 (CleanBrowsing)
        // Mismatched hostname: invalid.attacker.fake
        // Must reject connection and never accept mismatched certificate
        val isRejected = dnsTester.verifyRejectionOfMismatchedTlsHostname(
            validIp = "185.228.168.168",
            invalidHostname = "invalid.attacker.fake"
        )
        assertTrue("TLS engine must reject mismatched hostname with HTTPS endpoint identification", isRejected)
    }

    @Test
    fun testSystemResolverDnsResolution() = runBlocking {
        val result = dnsTester.testSystemResolver(DnsProviders.CLEAN_BROWSING.hostname)
        assertTrue("System resolver must successfully resolve normal domain (${result.normalDomain})", result.normalSuccess)
        assertNotNull("Resolved IP for normal domain must not be null", result.normalResolvedIp)
    }
}
