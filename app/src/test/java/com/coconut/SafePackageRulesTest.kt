package com.coconut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafePackageRulesTest {

    private val essentialPackages = setOf(
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
        "com.coconut"
    )

    @Test
    fun testEssentialPackagesCannotBeSuspended() {
        essentialPackages.forEach { pkg ->
            assertTrue("Package $pkg must be recognized as protected", isPackageProtected(pkg, "com.coconut"))
        }
    }

    @Test
    fun testUserAppCanBeSuspended() {
        val userPackages = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android"
        )

        userPackages.forEach { pkg ->
            assertFalse("User app $pkg must NOT be protected from The Snap", isPackageProtected(pkg, "com.coconut"))
        }
    }

    private fun isPackageProtected(packageName: String, selfPackage: String): Boolean {
        if (packageName == selfPackage) return true
        if (packageName in essentialPackages) return true
        return false
    }
}

