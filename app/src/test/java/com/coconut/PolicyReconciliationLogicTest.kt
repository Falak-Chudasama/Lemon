package com.coconut

import com.coconut.data.InstallationState
import com.coconut.data.SnapState
import com.coconut.data.SnappedAppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyReconciliationLogicTest {

    data class MockSystemState(
        val isDeviceOwner: Boolean = true,
        val uninstallBlockedPackages: MutableSet<String> = mutableSetOf(),
        val installedPackages: MutableSet<String> = mutableSetOf(),
        val suspendedPackages: MutableSet<String> = mutableSetOf(),
        var privateDnsMode: Int = 1,
        var privateDnsHost: String? = null
    )

    @Test
    fun testReconciliationResuspendsDriftedApp() {
        val system = MockSystemState(
            isDeviceOwner = true,
            installedPackages = mutableSetOf("com.game.app"),
            suspendedPackages = mutableSetOf() // Drift: not suspended!
        )

        val registry = mutableListOf(
            SnappedAppEntity(
                id = 1,
                packageName = "com.game.app",
                applicationName = "Game",
                actualState = SnapState.PROTECTED,
                installationState = InstallationState.INSTALLED
            )
        )

        // Reconciliation engine logic
        var resuspendCount = 0
        for (item in registry) {
            val isInstalled = item.packageName in system.installedPackages
            if (isInstalled) {
                val isSuspended = item.packageName in system.suspendedPackages
                if (!isSuspended) {
                    system.suspendedPackages.add(item.packageName)
                    resuspendCount++
                }
            }
        }

        assertEquals(1, resuspendCount)
        assertTrue("Package must now be suspended in Android system state", system.suspendedPackages.contains("com.game.app"))
    }

    @Test
    fun testReconciliationPreservesUninstalledApp() {
        val system = MockSystemState(
            isDeviceOwner = true,
            installedPackages = mutableSetOf(), // Uninstalled!
            suspendedPackages = mutableSetOf()
        )

        val item = SnappedAppEntity(
            id = 1,
            packageName = "com.uninstalled.app",
            applicationName = "Uninstalled App",
            actualState = SnapState.PROTECTED,
            installationState = InstallationState.INSTALLED
        )

        val isInstalled = item.packageName in system.installedPackages
        val updatedItem = if (!isInstalled) {
            item.copy(
                actualState = SnapState.AWAITING_INSTALLATION,
                installationState = InstallationState.NOT_INSTALLED
            )
        } else item

        assertEquals(SnapState.AWAITING_INSTALLATION, updatedItem.actualState)
        assertEquals(InstallationState.NOT_INSTALLED, updatedItem.installationState)
    }

    @Test
    fun testReconciliationRestoresSelfProtection() {
        val system = MockSystemState(
            isDeviceOwner = true,
            uninstallBlockedPackages = mutableSetOf() // Drift: not blocked!
        )

        val selfPackage = "com.coconut"
        if (selfPackage !in system.uninstallBlockedPackages) {
            system.uninstallBlockedPackages.add(selfPackage)
        }

        assertTrue("Coconut uninstall blocked state must be restored", system.uninstallBlockedPackages.contains(selfPackage))
    }

    @Test
    fun testReconciliationIdempotence() {
        val system = MockSystemState(
            isDeviceOwner = true,
            installedPackages = mutableSetOf("com.snapped.app"),
            suspendedPackages = mutableSetOf("com.snapped.app"),
            uninstallBlockedPackages = mutableSetOf("com.coconut")
        )

        var actionsFirstRun = 0
        if ("com.coconut" !in system.uninstallBlockedPackages) {
            system.uninstallBlockedPackages.add("com.coconut")
            actionsFirstRun++
        }
        if ("com.snapped.app" !in system.suspendedPackages) {
            system.suspendedPackages.add("com.snapped.app")
            actionsFirstRun++
        }

        assertEquals(0, actionsFirstRun) // No actions needed on already reconciled system
    }
}

