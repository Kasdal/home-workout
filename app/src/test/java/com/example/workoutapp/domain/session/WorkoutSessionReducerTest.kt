package com.example.workoutapp.domain.session

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseSessionMode
import com.example.workoutapp.data.local.entity.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionReducerTest {

    private val reducer = WorkoutSessionReducer()

    @Test
    fun `completeNextSet increments set count and starts rest timer for standard exercise`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name, usesSensor = false)
        )

        val result = reducer.completeNextSet(
            exercises = exercises,
            completedSets = emptyMap(),
            exerciseId = 1,
            restTimerDuration = 30,
            exerciseSwitchDuration = 90
        )

        assertEquals(1, result.completedSets[1])
        assertEquals(1, result.activeExerciseSelection.activeExerciseId)
        assertEquals(ExerciseSessionMode.MANUAL_REPS, result.activeExerciseSelection.activeExerciseMode)
        assertEquals(PostSetTimerRequest.Start(30), result.timerRequest)
    }

    @Test
    fun `completeNextSet starts switch timer after last set`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 2, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = reducer.completeNextSet(
            exercises = exercises,
            completedSets = mapOf(1 to 1),
            exerciseId = 1,
            restTimerDuration = 30,
            exerciseSwitchDuration = 90
        )

        assertEquals(2, result.completedSets[1])
        assertEquals(null, result.activeExerciseSelection.activeExerciseId)
        assertEquals(PostSetTimerRequest.Start(90), result.timerRequest)
    }

    @Test
    fun `completeNextSet uses hold duration for hold exercises`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Plank", weight = 0f, reps = 1, sets = 3, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 45)
        )

        val result = reducer.completeNextSet(
            exercises = exercises,
            completedSets = emptyMap(),
            exerciseId = 1,
            restTimerDuration = 30,
            exerciseSwitchDuration = 90
        )

        assertEquals(PostSetTimerRequest.Start(45), result.timerRequest)
        assertEquals(ExerciseSessionMode.HOLD_TIMER, result.activeExerciseSelection.activeExerciseMode)
    }

    @Test
    fun `undoSet decrements completed set count`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 4)
        )

        val result = reducer.undoSet(
            exercises = exercises,
            completedSets = mapOf(1 to 2),
            exerciseId = 1
        )

        assertEquals(1, result.completedSets[1])
        assertEquals(PostSetTimerRequest.None, result.timerRequest)
    }

    @Test
    fun `selectActiveExercise picks first incomplete exercise and mode`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 2),
            Exercise(id = 2, name = "Squat", weight = 0f, reps = 12, sets = 3, usesSensor = true)
        )

        val selection = reducer.selectActiveExercise(exercises, mapOf(1 to 2))

        assertEquals(2, selection.activeExerciseId)
        assertEquals(ExerciseSessionMode.SENSOR_REPS, selection.activeExerciseMode)
    }
}
