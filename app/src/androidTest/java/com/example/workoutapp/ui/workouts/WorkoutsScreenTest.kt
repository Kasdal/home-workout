package com.example.workoutapp.ui.workouts

import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.workoutapp.model.Exercise
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsBottomNavBarOnWorkoutsTab() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 100f, sets = 4, reps = 8)
        )

        composeTestRule.setContent {
            WorkoutsScreenContent(
                exercises = exercises,
                snackbarHostState = remember { SnackbarHostState() },
                onNavigateToRoute = { _: String -> },
                onAddExercise = { _: Exercise -> },
                onUpdateExercise = { _: Exercise -> },
                onDeleteExercise = { _: Int -> },
                onUpdateExercisePhoto = { _: Int, _: Uri -> },
                getExerciseHistory = { _: String -> flowOf(emptyList()) },
                onReorderExercises = { _: List<Exercise> -> }
            )
        }

        composeTestRule.onNodeWithText("Workout Library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Workouts").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Workouts")
            .assertIsDisplayed()
            .assertIsSelected()
        composeTestRule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }
}
