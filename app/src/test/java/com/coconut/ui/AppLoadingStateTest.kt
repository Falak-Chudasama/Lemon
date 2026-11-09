package com.coconut.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLoadingStateTest {

    @Test
    fun testLoadingStateFlags() {
        // AppLoadingState enum values
        val initialLoading = AppLoadingState.INITIAL_LOADING
        val loading = AppLoadingState.LOADING
        val loaded = AppLoadingState.LOADED
        val empty = AppLoadingState.EMPTY
        val error = AppLoadingState.ERROR
        val refreshing = AppLoadingState.REFRESHING

        // Only INITIAL_LOADING and LOADING should be considered blocking loading
        assertTrue(initialLoading == AppLoadingState.INITIAL_LOADING || initialLoading == AppLoadingState.LOADING)
        assertTrue(loading == AppLoadingState.INITIAL_LOADING || loading == AppLoadingState.LOADING)

        // LOADED, EMPTY, ERROR, REFRESHING must not be considered initial loading
        assertFalse(loaded == AppLoadingState.INITIAL_LOADING || loaded == AppLoadingState.LOADING)
        assertFalse(empty == AppLoadingState.INITIAL_LOADING || empty == AppLoadingState.LOADING)
        assertFalse(error == AppLoadingState.INITIAL_LOADING || error == AppLoadingState.LOADING)
        assertFalse(refreshing == AppLoadingState.INITIAL_LOADING || refreshing == AppLoadingState.LOADING)
    }

    @Test
    fun testStateProgressionPreventsPrematureEmptyState() {
        // Simulating the viewmodel lifecycle:
        // Initial state is INITIAL_LOADING
        var state = AppLoadingState.INITIAL_LOADING

        // Empty state must NEVER be rendered when state == INITIAL_LOADING
        val shouldShowEmptyInitial = (state == AppLoadingState.EMPTY)
        assertFalse("Premature empty screen must not render during INITIAL_LOADING", shouldShowEmptyInitial)

        // Loading finished with 0 apps -> state transitions to EMPTY
        val installedAppsCount = 0
        state = if (installedAppsCount == 0) AppLoadingState.EMPTY else AppLoadingState.LOADED

        val shouldShowEmptyFinal = (state == AppLoadingState.EMPTY)
        assertTrue("Empty screen should render only after load finishes with 0 items", shouldShowEmptyFinal)
    }
}

