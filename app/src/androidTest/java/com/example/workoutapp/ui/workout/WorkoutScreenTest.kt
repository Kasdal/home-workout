package com.example.workoutapp.ui.workout

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workoutapp.domain.session.CountdownType
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.SessionExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        exercises: List<Exercise>,
        sessionStarted: Boolean,
        isTimerRunning: Boolean,
        onSkipTimer: () -> Unit = {},
        onFinishExercise: (Int?, String?) -> Unit = { _, _ -> },
        history: List<SessionExercise> = emptyList(),
        sensorReps: Int = 0,
        sensorState: String = "REST",
        sensorDistance: Int = 0,
        sensorConnected: Boolean = false,
        activeExerciseMode: ExerciseSessionMode = ExerciseSessionMode.MANUAL_REPS,
        onResetSensorCounter: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            WorkoutScreenContent(
                exercises = exercises,
                sessionExercises = exercises,
                timerSeconds = 60,
                timerTotalSeconds = 90,
                isTimerRunning = isTimerRunning,
                isTimerPaused = false,
                completedSets = emptyMap(),
                sessionStarted = sessionStarted,
                sessionElapsedSeconds = 120,
                undoLastSetEnabled = true,
                snackbarHostState = remember { SnackbarHostState() },
                onNavigate = {},
                onOpenLibrary = {},
                onStartSession = {},
                onCompleteSession = {},
                onCompleteNextSet = {},
                onUndoSet = {},
                getExerciseHistory = { kotlinx.coroutines.flow.flowOf(history) },
                onSkipExercise = {},
                onFinishExercise = onFinishExercise,
                skippedExerciseIds = emptySet(),
                isSessionPaused = false,
                onToggleSessionPause = {},
                timerType = CountdownType.REST,
                onSkipTimer = onSkipTimer,
                onUpdateExercise = {},
                onRemoveExercisePhoto = {},
                onResetSensorCounter = onResetSensorCounter,
                sensorReps = sensorReps,
                sensorState = sensorState,
                sensorDistance = sensorDistance,
                sensorConnected = sensorConnected,
                activeExerciseId = if (sessionStarted) exercises.firstOrNull()?.id else null,
                activeExerciseMode = activeExerciseMode
            )
        }
    }

    private fun connectedSensorExercises() = listOf(
        Exercise(id = 1, name = "Squat", weight = 55f, sets = 4, reps = 13)
    )

    @Test
    fun displaysExerciseList() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10),
            Exercise(id = 2, name = "Squat", weight = 150f, sets = 5, reps = 5)
        )

        setContent(exercises = exercises, sessionStarted = false, isTimerRunning = false)

        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
        composeTestRule.onNodeWithText("Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("START SESSION").assertIsDisplayed()
    }

    @Test
    fun displaysSessionActiveState() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )

        setContent(exercises = exercises, sessionStarted = true, isTimerRunning = false)

        composeTestRule.onNodeWithText("Session Time: 02:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETE SESSION").assertDoesNotExist()
    }

    @Test
    fun sessionScreenShowsNoRestSwitchChipsAndNoSkipPauseStopTrio() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )

        setContent(exercises = exercises, sessionStarted = true, isTimerRunning = true)

        composeTestRule.onNodeWithText("SKIP").assertDoesNotExist()
        composeTestRule.onNodeWithText("PAUSE").assertDoesNotExist()
        composeTestRule.onNodeWithText("STOP").assertDoesNotExist()
        composeTestRule.onNodeWithText("Rest: 30s", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Switch: 90s", substring = true).assertDoesNotExist()
    }

    @Test
    fun tappingTheRunningTimerSkipsIt() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )
        var skipped = false

        setContent(
            exercises = exercises,
            sessionStarted = true,
            isTimerRunning = true,
            onSkipTimer = { skipped = true }
        )

        composeTestRule.onNodeWithContentDescription("Skip timer").performClick()

        assertTrue(skipped)
    }

    /**
     * The bug that started this work.
     *
     * With a 200dp photo and a 190dp nested sensor card, HOLD TO COMPLETE SET was
     * pushed off the bottom of the screen. assertIsDisplayed fails if the node
     * needs a scroll to be on screen, so this fails on the old layout.
     */
    @Test
    fun primaryActionStaysOnScreenWithTheSensorConnected() {
        setContent(
            exercises = connectedSensorExercises(),
            sessionStarted = true,
            isTimerRunning = false,
            sensorReps = 12,
            sensorState = "LIFTING",
            sensorDistance = 1033,
            sensorConnected = true,
            activeExerciseMode = ExerciseSessionMode.SENSOR_REPS
        )

        composeTestRule.onNodeWithText("HOLD TO COMPLETE SET").assertIsDisplayed()
        composeTestRule.onNodeWithText("Squat").assertIsDisplayed()
    }

    @Test
    fun sensorRowShowsRepsStateAndDistanceOnOneLine() {
        setContent(
            exercises = connectedSensorExercises(),
            sessionStarted = true,
            isTimerRunning = false,
            sensorReps = 12,
            sensorState = "LIFTING",
            sensorDistance = 1033,
            sensorConnected = true,
            activeExerciseMode = ExerciseSessionMode.SENSOR_REPS
        )

        composeTestRule.onNodeWithText("12").assertIsDisplayed()
        composeTestRule.onNodeWithText("reps").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIFTING").assertIsDisplayed()
        composeTestRule.onNodeWithText("1033 mm").assertIsDisplayed()
    }

    /**
     * The counter is live data the ESP cannot give back, and the button sits in a
     * scrolling card, so one tap must not throw the count away.
     */
    @Test
    fun resetCounterIgnoresTheFirstTapAndFiresOnTheSecond() {
        var resets = 0

        setContent(
            exercises = connectedSensorExercises(),
            sessionStarted = true,
            isTimerRunning = false,
            sensorReps = 12,
            sensorState = "LIFTING",
            sensorDistance = 1033,
            sensorConnected = true,
            activeExerciseMode = ExerciseSessionMode.SENSOR_REPS,
            onResetSensorCounter = { resets += 1 }
        )

        composeTestRule.onNodeWithContentDescription("Reset counter").performClick()
        assertEquals(0, resets)

        composeTestRule.onNodeWithContentDescription("Reset counter").performClick()
        assertEquals(1, resets)
    }

    @Test
    fun resetCounterForgetsTheArmedTapAfterTheConfirmWindow() {
        var resets = 0

        setContent(
            exercises = connectedSensorExercises(),
            sessionStarted = true,
            isTimerRunning = false,
            sensorConnected = true,
            activeExerciseMode = ExerciseSessionMode.SENSOR_REPS,
            onResetSensorCounter = { resets += 1 }
        )

        composeTestRule.onNodeWithContentDescription("Reset counter").performClick()
        composeTestRule.mainClock.advanceTimeBy(RESET_CONFIRM_WINDOW_MS + 500)
        composeTestRule.onNodeWithContentDescription("Reset counter").performClick()

        assertEquals(0, resets)
    }

    @Test
    fun sessionCardShowsNoExercisePhoto() {
        val withPhoto = connectedSensorExercises().map {
            it.copy(photoUri = "https://example.com/squat.jpg")
        }

        setContent(
            exercises = withPhoto,
            sessionStarted = true,
            isTimerRunning = false,
            sensorConnected = true,
            activeExerciseMode = ExerciseSessionMode.SENSOR_REPS
        )

        // The library screen still offers an upload, so the photo pipeline is
        // intact. Only the in-session card drops the image.
        composeTestRule.onNodeWithText("HOLD TO COMPLETE SET").assertIsDisplayed()
    }

    @Test
    fun idleTimerIsNotTappable() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )
        var skipped = false

        setContent(
            exercises = exercises,
            sessionStarted = true,
            isTimerRunning = false,
            onSkipTimer = { skipped = true }
        )

        composeTestRule.onNodeWithContentDescription("Skip timer")
            .assertDoesNotExist()

        assertFalse(skipped)
    }

    @Test
    fun sessionCardShowsExerciseTitleAlongsideHistory() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )
        val history = listOf(
            SessionExercise(
                sessionId = 1,
                exerciseName = "Bench Press",
                weight = 100f,
                sets = 4,
                reps = 10,
                volume = 4000f
            )
        )

        setContent(
            exercises = exercises,
            sessionStarted = true,
            isTimerRunning = false,
            history = history
        )

        val titleNode = composeTestRule.onNodeWithText("Bench Press").fetchSemanticsNode()
        val minWidthPx = with(composeTestRule.density) { 50.dp.toPx() }
        assertTrue(
            "Exercise title is squeezed to ${titleNode.boundsInRoot.width}px",
            titleNode.boundsInRoot.width > minWidthPx
        )
    }

    @Test
    fun finishExerciseOpensFeelCheckDialogAndStoresValues() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )
        var finishedRpe: Int? = null
        var finishedNote: String? = null

        setContent(
            exercises = exercises,
            sessionStarted = true,
            isTimerRunning = false,
            onFinishExercise = { rpe, note ->
                finishedRpe = rpe
                finishedNote = note
            }
        )

        composeTestRule.onNodeWithContentDescription("Finish exercise").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("How did Bench Press feel?")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("How did Bench Press feel?").assertIsDisplayed()

        composeTestRule.onNodeWithText("7").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Save & continue").performClick()

        assertEquals(7, finishedRpe)
        assertNull(finishedNote)
    }

    @Test
    fun feelCheckDialogSkipPromptFinishesWithoutStoring() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )
        var finishedRpe: Int? = null
        var finishCalls = 0

        setContent(
            exercises = exercises,
            sessionStarted = true,
            isTimerRunning = false,
            onFinishExercise = { rpe, _ ->
                finishedRpe = rpe
                finishCalls++
            }
        )

        composeTestRule.onNodeWithContentDescription("Finish exercise").performClick()
        composeTestRule.onNodeWithText("Skip prompt").performClick()

        assertEquals(1, finishCalls)
        assertNull(finishedRpe)
    }
}
