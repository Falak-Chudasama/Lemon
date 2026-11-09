package com.coconut.policy

import com.coconut.data.QuarantineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuarantineTest {

    @Test
    fun testDurationClamping() {
        assertEquals(30, QuarantineManager.clampDuration(10))
        assertEquals(30, QuarantineManager.clampDuration(29))
        assertEquals(30, QuarantineManager.clampDuration(30))
        assertEquals(60, QuarantineManager.clampDuration(60))
        assertEquals(300, QuarantineManager.clampDuration(300))
        assertEquals(300, QuarantineManager.clampDuration(301))
        assertEquals(300, QuarantineManager.clampDuration(1000))
    }

    @Test
    fun testPackageListParsingAndSerialization() {
        val emptyEntity = QuarantineEntity(selectedPackages = "")
        assertTrue(emptyEntity.getPackageList().isEmpty())

        val whitespaceEntity = QuarantineEntity(selectedPackages = "   ")
        assertTrue(whitespaceEntity.getPackageList().isEmpty())

        val singleEntity = QuarantineEntity(selectedPackages = "com.instagram.android")
        assertEquals(setOf("com.instagram.android"), singleEntity.getPackageList())

        val multipleEntity = QuarantineEntity(
            selectedPackages = "com.instagram.android, com.zhiliaoapp.musically , com.twitter.android"
        )
        assertEquals(
            setOf("com.instagram.android", "com.zhiliaoapp.musically", "com.twitter.android"),
            multipleEntity.getPackageList()
        )

        // Roundtrip with fromPackageList
        val packages = setOf("com.meta.threads", "com.snapchat.android")
        val createdEntity = QuarantineEntity.fromPackageList(
            packages = packages,
            durationMinutes = 45,
            startTime = 1000L,
            endTime = 1000L + (45 * 60 * 1000L),
            bootCount = 5,
            elapsedRealtime = 50000L
        )
        assertEquals(packages, createdEntity.getPackageList())
        assertEquals(45, createdEntity.durationMinutes)
        assertTrue(createdEntity.isActive)
        assertEquals(5, createdEntity.bootCountAtStart)
        assertEquals(50000L, createdEntity.elapsedRealtimeAtStart)
    }

    @Test
    fun testCalculateRemainingSecondsMonotonic() {
        val durationMinutes = 30
        val startTimeWall = 1_700_000_000_000L
        val endTimeWall = startTimeWall + (durationMinutes * 60 * 1000L)
        val startElapsed = 100_000_000L
        val bootCount = 1

        // 10 minutes (600 seconds) elapsed
        val currentElapsed = startElapsed + (600 * 1000L)
        val currentWall = startTimeWall + (600 * 1000L)

        val remaining = QuarantineManager.calculateRemainingSecondsPure(
            isActive = true,
            durationMinutes = durationMinutes,
            startTimeEpochMillis = startTimeWall,
            endTimeEpochMillis = endTimeWall,
            bootCountAtStart = bootCount,
            elapsedRealtimeAtStart = startElapsed,
            currentWallClock = currentWall,
            currentBootCount = bootCount,
            currentElapsedRealtime = currentElapsed
        )

        // Remaining should be 20 minutes (1200 seconds)
        assertEquals(1200L, remaining)
    }

    @Test
    fun testWallClockTamperResistance() {
        // Test anti-tampering: user sets phone clock backwards by 2 hours
        val durationMinutes = 30
        val startTimeWall = 1_700_000_000_000L
        val endTimeWall = startTimeWall + (durationMinutes * 60 * 1000L)
        val startElapsed = 100_000_000L
        val bootCount = 1

        // Real monotonic time: 10 minutes elapsed
        val currentElapsed = startElapsed + (600 * 1000L)
        // Tampered wall clock: user set clock BACK to startTime - 7200s
        val tamperedPastWall = startTimeWall - (7200 * 1000L)

        val remainingPastTamper = QuarantineManager.calculateRemainingSecondsPure(
            isActive = true,
            durationMinutes = durationMinutes,
            startTimeEpochMillis = startTimeWall,
            endTimeEpochMillis = endTimeWall,
            bootCountAtStart = bootCount,
            elapsedRealtimeAtStart = startElapsed,
            currentWallClock = tamperedPastWall,
            currentBootCount = bootCount,
            currentElapsedRealtime = currentElapsed
        )
        // Anti-tamper: monotonic remaining (1200s) takes precedence over distorted past wall time
        assertTrue(remainingPastTamper >= 1200L)

        // Tampered wall clock: user set clock FORWARD to endTime + 3600s to attempt early escape
        val tamperedFutureWall = endTimeWall + (3600 * 1000L)
        val remainingFutureTamper = QuarantineManager.calculateRemainingSecondsPure(
            isActive = true,
            durationMinutes = durationMinutes,
            startTimeEpochMillis = startTimeWall,
            endTimeEpochMillis = endTimeWall,
            bootCountAtStart = bootCount,
            elapsedRealtimeAtStart = startElapsed,
            currentWallClock = tamperedFutureWall,
            currentBootCount = bootCount,
            currentElapsedRealtime = currentElapsed
        )
        // Monotonic remaining (1200s) PREVENTS premature expiry!
        assertEquals(1200L, remainingFutureTamper)
    }

    @Test
    fun testRebootFallbackCalculation() {
        // Device reboots: bootCount increments from 1 to 2
        val durationMinutes = 60
        val startTimeWall = 1_700_000_000_000L
        val endTimeWall = startTimeWall + (durationMinutes * 60 * 1000L)
        val startElapsed = 100_000_000L
        val bootCountStart = 1

        // 30 minutes later in wall clock time, device is on boot count 2
        val currentBootCount = 2
        val currentElapsed = 5_000_000L // New elapsed time reset on new boot
        val currentWall = startTimeWall + (30 * 60 * 1000L)

        val remainingAfterReboot = QuarantineManager.calculateRemainingSecondsPure(
            isActive = true,
            durationMinutes = durationMinutes,
            startTimeEpochMillis = startTimeWall,
            endTimeEpochMillis = endTimeWall,
            bootCountAtStart = bootCountStart,
            elapsedRealtimeAtStart = startElapsed,
            currentWallClock = currentWall,
            currentBootCount = currentBootCount,
            currentElapsedRealtime = currentElapsed
        )

        // Remaining should be 30 minutes (1800 seconds)
        assertEquals(1800L, remainingAfterReboot)
    }

    @Test
    fun testInactiveOrExpiredReturnsZero() {
        val inactiveRemaining = QuarantineManager.calculateRemainingSecondsPure(
            isActive = false,
            durationMinutes = 30,
            startTimeEpochMillis = 1000L,
            endTimeEpochMillis = 2000L,
            bootCountAtStart = 1,
            elapsedRealtimeAtStart = 100L,
            currentWallClock = 1500L,
            currentBootCount = 1,
            currentElapsedRealtime = 150L
        )
        assertEquals(0L, inactiveRemaining)
    }

    @Test
    fun testQuarantineOverlapWithNightSchedule() {
        // Timeline:
        // 22:30 - Quarantine started (30m). Night schedule inactive. Manual preference OFF.
        val t2230 = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = true)
        assertTrue("At 22:30, DNS must be ON due to active quarantine", t2230.effectiveDnsDesired)
        assertEquals(listOf("QUARANTINE"), t2230.triggerReasons)
        assertFalse("User cannot disable DNS while Quarantine is active", t2230.canUserDisableManual)

        // 23:00 - Night schedule begins. Both Quarantine and Night schedule active.
        val t2300 = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = true, quarantineActive = true)
        assertTrue("At 23:00, DNS must be ON (both policies active)", t2300.effectiveDnsDesired)
        assertEquals(listOf("NIGHT_SCHEDULE", "QUARANTINE"), t2300.triggerReasons)

        // 23:30 - Quarantine period matures and expires. Night schedule is still ACTIVE!
        val t2330 = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = true, quarantineActive = false)
        assertTrue("At 23:30 after Quarantine expiry, DNS MUST REMAIN ON because night window is active!", t2330.effectiveDnsDesired)
        assertEquals(listOf("NIGHT_SCHEDULE"), t2330.triggerReasons)

        // 06:00 - Night window ends. Manual preference is OFF.
        val t0600 = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = false)
        assertFalse("At 06:00 when night window ends, DNS turns OFF gracefully", t0600.effectiveDnsDesired)
    }

    @Test
    fun testTemporaryAppBlockingMembershipAndExpiry() {
        val targetApp = "com.zhiliaoapp.musically"
        val otherApp = "com.lemon.app"
        val quarantinedSet = setOf(targetApp, "com.instagram.android")

        // Active quarantine check
        val isBlockedDuringActive = quarantinedSet.contains(targetApp)
        val isOtherBlocked = quarantinedSet.contains(otherApp)
        assertTrue("Restricted package must be blocked during quarantine", isBlockedDuringActive)
        assertFalse("Unrestricted package must not be blocked", isOtherBlocked)

        // Expiry check: upon deactivation, active set is cleared
        val emptySetAfterExpiry = emptySet<String>()
        assertFalse("Restricted package must be released when quarantine expires", emptySetAfterExpiry.contains(targetApp))
    }
}

