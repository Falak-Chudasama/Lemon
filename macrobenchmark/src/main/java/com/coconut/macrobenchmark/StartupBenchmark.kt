package com.coconut.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupColdNoCompilation() = benchmarkStartup(
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD
    )

    @Test
    fun startupColdWithBaselineProfile() = benchmarkStartup(
        compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        startupMode = StartupMode.COLD
    )

    @Test
    fun startupWarmWithBaselineProfile() = benchmarkStartup(
        compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
        startupMode = StartupMode.WARM
    )

    private fun benchmarkStartup(
        compilationMode: CompilationMode,
        startupMode: StartupMode
    ) = benchmarkRule.measureRepeated(
        packageName = "com.coconut",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = startupMode,
        iterations = 5,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()
    }
}

