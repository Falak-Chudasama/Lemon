package com.coconut.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineRule.collect(
        packageName = "com.coconut"
    ) {
        // Critical Journey 1: App Startup & Home Screen
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // Critical Journey 2: Navigation to Snap Screen & Lazy Column Scrolling
        val snapTab = device.wait(Until.findObject(By.text("The Snap")), 3000)
        snapTab?.click()
        device.waitForIdle()

        val list = device.findObject(By.scrollable(true))
        list?.fling(Direction.DOWN)
        device.waitForIdle()
        list?.fling(Direction.UP)
        device.waitForIdle()

        // Critical Journey 3: Navigation to Content Shield & Settings
        val shieldTab = device.wait(Until.findObject(By.text("Shield")), 3000)
        shieldTab?.click()
        device.waitForIdle()

        val settingsTab = device.wait(Until.findObject(By.text("Settings")), 3000)
        settingsTab?.click()
        device.waitForIdle()
    }
}

