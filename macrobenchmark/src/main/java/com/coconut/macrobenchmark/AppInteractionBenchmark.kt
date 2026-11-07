package com.coconut.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
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
class AppInteractionBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun snapScreenScrollAndSearchBenchmark() = benchmarkRule.measureRepeated(
        packageName = "com.coconut",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            // Navigate to Snap Screen
            val snapTab = device.wait(Until.findObject(By.text("The Snap")), 5000)
            snapTab?.click()
            device.waitForIdle()
        }
    ) {
        val appList = device.findObject(By.scrollable(true))
        if (appList != null) {
            appList.setGestureMargin(device.displayWidth / 5)
            appList.fling(Direction.DOWN)
            device.waitForIdle()
            appList.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    @Test
    fun contentShieldToggleBenchmark() = benchmarkRule.measureRepeated(
        packageName = "com.coconut",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            val shieldTab = device.wait(Until.findObject(By.text("Shield")), 5000)
            shieldTab?.click()
            device.waitForIdle()
        }
    ) {
        // Find diagnostic test button or scroll through shield options
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.fling(Direction.DOWN)
        device.waitForIdle()
        scrollable?.fling(Direction.UP)
        device.waitForIdle()
    }
}

