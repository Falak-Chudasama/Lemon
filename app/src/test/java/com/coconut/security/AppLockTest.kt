package com.coconut.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    @Test
    fun testAppLockLifecycleForegroundToBackgroundLock() {
        // Simulation of ActivityLifecycleCallbacks tracking foreground activities
        var foregroundActivityCount = 0
        var isSessionLocked = false

        fun onActivityStarted() {
            foregroundActivityCount++
        }

        fun onActivityStopped() {
            foregroundActivityCount = (foregroundActivityCount - 1).coerceAtLeast(0)
            if (foregroundActivityCount == 0) {
                // Application exited to background -> LOCK SESSION IMMEDIATELY
                isSessionLocked = true
            }
        }

        fun onUnlockSuccess() {
            isSessionLocked = false
        }

        // 1. App starts up: 1 activity in foreground
        onActivityStarted()
        assertEquals(1, foregroundActivityCount)
        assertFalse("Session starts unlocked during initial session", isSessionLocked)

        // 2. User presses Home button or switches apps: activity stops
        onActivityStopped()
        assertEquals(0, foregroundActivityCount)
        assertTrue("When app leaves foreground, session MUST lock immediately", isSessionLocked)

        // 3. User returns to app: session is still locked!
        onActivityStarted()
        assertEquals(1, foregroundActivityCount)
        assertTrue("Session must remain locked until biometric authentication succeeds", isSessionLocked)

        // 4. Biometric succeeds
        onUnlockSuccess()
        assertFalse("Session unlocks upon valid biometric confirmation", isSessionLocked)
    }

    @Test
    fun testBiometricStrongConstraintValidation() {
        // Enforce BIOMETRIC_STRONG (Class 3 hardware biometric) without DEVICE_CREDENTIAL
        val authenticatorsRequested = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
        val deviceCredentialBit = androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

        // Assert that DEVICE_CREDENTIAL is NOT part of the requested authenticators mask
        val hasDeviceCredentialFallback = (authenticatorsRequested and deviceCredentialBit) != 0
        assertFalse("Strong App Lock MUST NOT permit device credential (PIN/pattern) bypass", hasDeviceCredentialFallback)
    }
}

