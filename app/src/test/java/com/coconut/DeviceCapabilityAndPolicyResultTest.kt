package com.coconut

import com.coconut.policy.ManagementState
import com.coconut.policy.PolicyCapabilities
import com.coconut.policy.PolicyResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityAndPolicyResultTest {

    @Test
    fun testPolicyResultHierarchyAndTruth() {
        val verified: PolicyResult = PolicyResult.AppliedAndVerified
        assertTrue("AppliedAndVerified must report isSuccess = true", verified.isSuccess)
        assertNull("AppliedAndVerified has no error message", verified.errorMessage)

        val awaitingDo: PolicyResult = PolicyResult.AwaitingDeviceOwner("ADB provisioning required.")
        assertFalse("AwaitingDeviceOwner must NOT report isSuccess = true", awaitingDo.isSuccess)
        assertEquals("ADB provisioning required.", awaitingDo.errorMessage)

        val partial: PolicyResult = PolicyResult.PartiallyApplied("Suspension succeeded, uninstall block failed.")
        assertFalse("PartiallyApplied must NOT report full success", partial.isSuccess)
        assertNotNull(partial.errorMessage)

        val unsupported: PolicyResult = PolicyResult.Unsupported("Launcher is protected.")
        assertFalse(unsupported.isSuccess)
        assertEquals("Launcher is protected.", unsupported.errorMessage)

        val failed: PolicyResult = PolicyResult.Failed("DPM call failed.")
        assertFalse(failed.isSuccess)
        assertEquals("DPM call failed.", failed.errorMessage)
    }

    @Test
    fun testPolicyCapabilitiesModel() {
        // Test Device Owner capability state
        val doCaps = PolicyCapabilities(
            managementState = ManagementState.DEVICE_OWNER,
            isDeviceAdmin = true,
            isDeviceOwner = true,
            canSuspendPackages = true,
            canBlockUninstall = true,
            canConfigurePrivateDns = true,
            canApplyUserRestrictions = true,
            receiverComponentValid = true,
            statusDescription = "Fully provisioned"
        )
        assertTrue(doCaps.canSuspendPackages)
        assertTrue(doCaps.canConfigurePrivateDns)
        assertTrue(doCaps.canBlockUninstall)

        // Test Ordinary Device Administrator capability state
        val adminOnlyCaps = PolicyCapabilities(
            managementState = ManagementState.DEVICE_ADMIN_ONLY,
            isDeviceAdmin = true,
            isDeviceOwner = false,
            canSuspendPackages = false,
            canBlockUninstall = false,
            canConfigurePrivateDns = false,
            canApplyUserRestrictions = false,
            receiverComponentValid = true,
            statusDescription = "Ordinary Admin"
        )
        // CRITICAL GUARANTEE: Ordinary Device Admin must NOT be granted DO capabilities!
        assertFalse(adminOnlyCaps.canSuspendPackages)
        assertFalse(adminOnlyCaps.canConfigurePrivateDns)
        assertFalse(adminOnlyCaps.canBlockUninstall)
        assertFalse(adminOnlyCaps.isDeviceOwner)
        assertTrue(adminOnlyCaps.isDeviceAdmin)
    }
}

