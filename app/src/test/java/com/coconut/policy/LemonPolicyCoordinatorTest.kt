package com.coconut.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LemonPolicyCoordinatorTest {

    @Test
    fun testAllPolicyCombinations() {
        // Combination 1: All false -> effective desired OFF
        val c1 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = false,
            nightScheduleActive = false,
            quarantineActive = false
        )
        assertFalse("All false must result in DNS OFF", c1.effectiveDnsDesired)
        assertTrue("User can disable manual when no mandatory policy is active", c1.canUserDisableManual)
        assertFalse(c1.isMandatory)
        assertTrue(c1.triggerReasons.isEmpty())

        // Combination 2: Manual only -> effective desired ON
        val c2 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = true,
            nightScheduleActive = false,
            quarantineActive = false
        )
        assertTrue("Manual ON must result in DNS ON", c2.effectiveDnsDesired)
        assertTrue("User can disable manual DNS", c2.canUserDisableManual)
        assertFalse(c2.isMandatory)
        assertEquals(listOf("MANUAL"), c2.triggerReasons)

        // Combination 3: Night schedule only -> effective desired ON
        val c3 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = false,
            nightScheduleActive = true,
            quarantineActive = false
        )
        assertTrue("Night schedule must force DNS ON", c3.effectiveDnsDesired)
        assertFalse("User cannot disable manual DNS while night schedule is active", c3.canUserDisableManual)
        assertTrue(c3.isMandatory)
        assertEquals(listOf("NIGHT_SCHEDULE"), c3.triggerReasons)

        // Combination 4: Quarantine only -> effective desired ON
        val c4 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = false,
            nightScheduleActive = false,
            quarantineActive = true
        )
        assertTrue("Quarantine must force DNS ON", c4.effectiveDnsDesired)
        assertFalse("User cannot disable manual DNS while quarantine is active", c4.canUserDisableManual)
        assertTrue(c4.isMandatory)
        assertEquals(listOf("QUARANTINE"), c4.triggerReasons)

        // Combination 5: Manual + Night -> effective desired ON
        val c5 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = true,
            nightScheduleActive = true,
            quarantineActive = false
        )
        assertTrue("Manual + Night must result in DNS ON", c5.effectiveDnsDesired)
        assertFalse(c5.canUserDisableManual)
        assertTrue(c5.isMandatory)
        assertEquals(listOf("MANUAL", "NIGHT_SCHEDULE"), c5.triggerReasons)

        // Combination 6: Manual + Quarantine -> effective desired ON
        val c6 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = true,
            nightScheduleActive = false,
            quarantineActive = true
        )
        assertTrue("Manual + Quarantine must result in DNS ON", c6.effectiveDnsDesired)
        assertFalse(c6.canUserDisableManual)
        assertTrue(c6.isMandatory)
        assertEquals(listOf("MANUAL", "QUARANTINE"), c6.triggerReasons)

        // Combination 7: Night + Quarantine -> effective desired ON
        val c7 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = false,
            nightScheduleActive = true,
            quarantineActive = true
        )
        assertTrue("Night + Quarantine must result in DNS ON", c7.effectiveDnsDesired)
        assertFalse(c7.canUserDisableManual)
        assertTrue(c7.isMandatory)
        assertEquals(listOf("NIGHT_SCHEDULE", "QUARANTINE"), c7.triggerReasons)

        // Combination 8: All true -> effective desired ON
        val c8 = LemonPolicyCoordinator.evaluatePolicy(
            manualDnsEnabled = true,
            nightScheduleActive = true,
            quarantineActive = true
        )
        assertTrue("All true must result in DNS ON", c8.effectiveDnsDesired)
        assertFalse(c8.canUserDisableManual)
        assertTrue(c8.isMandatory)
        assertEquals(listOf("MANUAL", "NIGHT_SCHEDULE", "QUARANTINE"), c8.triggerReasons)
    }

    @Test
    fun testNightBoundaryTransitionWithManualPreference() {
        // Case A: Manual is OFF. Night begins -> turns ON. Night ends -> turns OFF.
        val beforeNight = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = false)
        assertFalse(beforeNight.effectiveDnsDesired)

        val duringNight = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = true, quarantineActive = false)
        assertTrue(duringNight.effectiveDnsDesired)

        val afterNight = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = false)
        assertFalse(afterNight.effectiveDnsDesired)

        // Case B: Manual is ON (The bug in previous versions!).
        // Before night -> ON. During night -> ON. Night ends at 6 AM -> MUST REMAIN ON!
        val beforeNightManual = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = true, nightScheduleActive = false, quarantineActive = false)
        assertTrue("Manual ON before night must be ON", beforeNightManual.effectiveDnsDesired)

        val duringNightManual = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = true, nightScheduleActive = true, quarantineActive = false)
        assertTrue("Manual ON during night must be ON", duringNightManual.effectiveDnsDesired)

        val afterNightManual = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = true, nightScheduleActive = false, quarantineActive = false)
        assertTrue("Manual ON after night ends MUST NOT be disabled by schedule manager!", afterNightManual.effectiveDnsDesired)
    }

    @Test
    fun testQuarantineTransitionWithManualPreference() {
        // Case A: Manual is OFF. Quarantine starts -> turns ON. Quarantine expires -> turns OFF.
        val beforeQ = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = false)
        assertFalse(beforeQ.effectiveDnsDesired)

        val duringQ = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = true)
        assertTrue(duringQ.effectiveDnsDesired)

        val afterQ = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = false, nightScheduleActive = false, quarantineActive = false)
        assertFalse(afterQ.effectiveDnsDesired)

        // Case B: Manual is ON. Quarantine expires -> MUST REMAIN ON!
        val afterQManual = LemonPolicyCoordinator.evaluatePolicy(manualDnsEnabled = true, nightScheduleActive = false, quarantineActive = false)
        assertTrue("Manual ON after quarantine expires MUST REMAIN ON", afterQManual.effectiveDnsDesired)
    }
}

