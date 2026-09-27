package com.example.workoutapp.ui.workout

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * System Back is the highest-consequence gesture in a running session. The
 * workout ViewModel is scoped to its navigation entry, so letting Back through
 * destroys the session with no confirmation and no recovery.
 *
 * These tests drive a real `onBackPressedDispatcher` rather than asserting on a
 * lambda, because the defect was that the dispatcher was never consumed at all.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutSessionBackHandlerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun pressBack() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
    }

    private fun backHandlerRegistered(): Boolean {
        var registered = false
        composeTestRule.runOnUiThread {
            registered = composeTestRule.activity.onBackPressedDispatcher
                .hasEnabledCallbacks()
        }
        return registered
    }

    @Test
    fun backIsConsumedWhileSessionIsRunning() {
        var pauseCalls = 0

        composeTestRule.setContent {
            WorkoutSessionBackHandler(
                sessionStarted = true,
                isSessionPaused = false,
                onPauseRequested = { pauseCalls++ }
            )
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "No BackHandler was registered, so Back would destroy the session",
            backHandlerRegistered()
        )

        pressBack()

        assertEquals(
            "Back during an active session must pause it, not leave the screen",
            1,
            pauseCalls
        )
    }

    @Test
    fun backDoesNotDoublePauseAnAlreadyPausedSession() {
        var pauseCalls = 0

        composeTestRule.setContent {
            WorkoutSessionBackHandler(
                sessionStarted = true,
                isSessionPaused = true,
                onPauseRequested = { pauseCalls++ }
            )
        }
        composeTestRule.waitForIdle()

        pressBack()
        pressBack()

        assertEquals(
            "An already paused session must not be paused again",
            0,
            pauseCalls
        )
    }

    @Test
    fun backIsNotInterceptedWhenNoSessionIsRunning() {
        var pauseCalls = 0

        composeTestRule.setContent {
            WorkoutSessionBackHandler(
                sessionStarted = false,
                isSessionPaused = false,
                onPauseRequested = { pauseCalls++ }
            )
        }
        composeTestRule.waitForIdle()

        assertFalse(
            "With no active session Back must navigate normally",
            backHandlerRegistered()
        )
        assertEquals(0, pauseCalls)
    }
}
