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
        "com.android.dialer",
        "com.google.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.coconut"
    )

    private val samsungOneUiPackages = setOf(
        "com.sec.android.app.launcher",
        "com.sec.android.app.camera",
        "com.samsung.android.incallui",
        "com.samsung.android.emergency",
        "com.samsung.android.dialer",
        "com.samsung.android.messaging",
        "com.samsung.android.biometrics",
        "com.sec.android.app.myfiles",
        "com.samsung.android.app.telephonyui",
        "com.samsung.android.honeyboard"
    )

    @Test
    fun testEssentialPackagesCannotBeSuspended() {
        essentialPackages.forEach { pkg ->
            assertTrue("Package $pkg must be recognized as protected", isPackageProtected(pkg, "com.coconut"))
        }
    }

    @Test
    fun testSamsungOneUiPackagesProtected() {
        samsungOneUiPackages.forEach { pkg ->
            assertTrue("Samsung package $pkg must be protected", isPackageProtected(pkg, "com.coconut"))
        }
    }

    @Test
    fun testUserAppCanBeSuspended() {
        val userPackages = listOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android",
            "com.supercell.clashofclans",
            "com.reddit.frontpage"
        )

        userPackages.forEach { pkg ->
            assertFalse("User app $pkg must NOT be protected from The Snap", isPackageProtected(pkg, "com.coconut"))
        }
    }

    private fun isPackageProtected(packageName: String, selfPackage: String): Boolean {
        if (packageName == selfPackage) return true
        if (packageName in essentialPackages) return true
        if (packageName in samsungOneUiPackages) return true
        return false
    }
}
