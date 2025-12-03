package com.example.workoutapp.ui.workout

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workoutapp.data.local.entity.Exercise
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysExerciseList() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10),
            Exercise(id = 2, name = "Squat", weight = 150f, sets = 5, reps = 5)
        )

        composeTestRule.setContent {
            WorkoutScreenContent(
                exercises = exercises,
                timerSeconds = 0,
                isTimerRunning = false,
                isTimerPaused = false,
                completedSets = emptyMap(),
                sessionStarted = false,
                sessionElapsedSeconds = 0,
                restTimerDuration = 30,
                exerciseSwitchDuration = 90,
                drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
                snackbarHostState = remember { SnackbarHostState() },
                onNavigate = {},
                onOpenDrawer = {},
                onStartSession = {},
                onCompleteSession = {},
                onAddExercise = {},
                onCompleteNextSet = {},
                onUndoSet = {},
                onUpdateExercise = {},
                onDeleteExercise = {},
                onStartTimer = {},
                onPauseTimer = {},
                onResumeTimer = {},
                onStopTimer = {},
                onSetRestDuration = {},
                onSetExerciseSwitchDuration = {}
            )
        }

        composeTestRule.onNodeWithText("Bench Press").assertIsDisplayed()
        composeTestRule.onNodeWithText("Squat").assertIsDisplayed()
        composeTestRule.onNodeWithText("START SESSION").assertIsDisplayed()
    }

    @Test
    fun displaysSessionActiveState() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 10)
        )

        composeTestRule.setContent {
            WorkoutScreenContent(
                exercises = exercises,
                timerSeconds = 60,
                isTimerRunning = true,
                isTimerPaused = false,
                completedSets = emptyMap(),
                sessionStarted = true,
                sessionElapsedSeconds = 120,
                restTimerDuration = 30,
                exerciseSwitchDuration = 90,
                drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
                snackbarHostState = remember { SnackbarHostState() },
                onNavigate = {},
                onOpenDrawer = {},
                onStartSession = {},
                onCompleteSession = {},
                onAddExercise = {},
                onCompleteNextSet = {},
                onUndoSet = {},
                onUpdateExercise = {},
                onDeleteExercise = {},
                onStartTimer = {},
                onPauseTimer = {},
                onResumeTimer = {},
                onStopTimer = {},
                onSetRestDuration = {},
                onSetExerciseSwitchDuration = {}
            )
        }

        composeTestRule.onNodeWithText("Session Time: 02:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("COMPLETE SESSION").assertIsDisplayed()
    }
}
