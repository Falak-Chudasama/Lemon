package com.coconut

import com.coconut.data.InstallationState
import com.coconut.data.SnapState
import com.coconut.data.SnappedAppEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapRegistryEntityTest {

    @Test
    fun testInitialSnapCreation() {
        val now = 1700000000000L
        val app = SnappedAppEntity(
            id = 1,
            packageName = "com.example.distraction",
            applicationName = "Distraction App",
            category = "Social",
            firstSnappedAt = now,
            lastAppliedAt = now,
            desiredState = "SUSPENDED",
            actualState = SnapState.PROTECTED,
            installationState = InstallationState.INSTALLED,
            lastError = null,
            createdAt = now,
            updatedAt = now
        )

        assertEquals("com.example.distraction", app.packageName)
        assertEquals(SnapState.PROTECTED, app.actualState)
        assertEquals(InstallationState.INSTALLED, app.installationState)
        assertNull(app.lastError)
    }

    @Test
    fun testUninstalledAppPreservesSnapRecord() {
        val now = 1700000000000L
        val original = SnappedAppEntity(
            id = 1,
            packageName = "com.example.game",
            applicationName = "Addictive Game",
            category = "Games",
            firstSnappedAt = now,
            lastAppliedAt = now,
            actualState = SnapState.PROTECTED,
            installationState = InstallationState.INSTALLED
        )

        // When app is uninstalled by user, record MUST survive in Room database
        val uninstalledState = original.copy(
            actualState = SnapState.AWAITING_INSTALLATION,
            installationState = InstallationState.NOT_INSTALLED,
            updatedAt = now + 1000L
        )

        assertEquals("com.example.game", uninstalledState.packageName)
        assertEquals(SnapState.AWAITING_INSTALLATION, uninstalledState.actualState)
        assertEquals(InstallationState.NOT_INSTALLED, uninstalledState.installationState)
        assertEquals(original.firstSnappedAt, uninstalledState.firstSnappedAt)
    }

    @Test
    fun testReinstalledAppResuspensionStateTransition() {
        val firstSnapTime = 1700000000000L
        val reinstallTime = 1700050000000L

        val awaitingApp = SnappedAppEntity(
            id = 1,
            packageName = "com.example.social",
            applicationName = "Social App",
            category = "Social",
            firstSnappedAt = firstSnapTime,
            lastAppliedAt = firstSnapTime,
            actualState = SnapState.AWAITING_INSTALLATION,
            installationState = InstallationState.NOT_INSTALLED
        )

        // Reinstallation detected -> Auto resuspend applied
        val resuspendedApp = awaitingApp.copy(
            actualState = SnapState.PROTECTED,
            installationState = InstallationState.INSTALLED,
            lastAppliedAt = reinstallTime,
            updatedAt = reinstallTime
        )

        assertEquals(SnapState.PROTECTED, resuspendedApp.actualState)
        assertEquals(InstallationState.INSTALLED, resuspendedApp.installationState)
        assertEquals(firstSnapTime, resuspendedApp.firstSnappedAt)
        assertEquals(reinstallTime, resuspendedApp.lastAppliedAt)
    }
}

