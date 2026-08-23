package com.example.workoutapp.domain.session

import com.example.workoutapp.data.remote.model.CloudWorkoutSession
import com.example.workoutapp.data.remote.model.toLocal
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.UserMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCompletionCalculatorTest {

    private val calculator = SessionCompletionCalculator()

    @Test
    fun `calculate aggregates total weight and builds session exercises`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Push Up", weight = 0f, reps = 20, sets = 3, exerciseType = ExerciseType.BODYWEIGHT.name)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 2, 2 to 1),
            elapsedSeconds = 1200,
            endTime = 123456789L,
            userMetrics = UserMetrics(weightKg = 80f),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal"
        )

        assertEquals(2, result.sessionExercises.size)
        assertEquals(2 * 10 * 100f + 1 * 20 * 80f, result.session.totalWeightLifted, 0.01f)
        assertEquals(123456789L, result.session.date)
        assertEquals(1200L, result.session.durationSeconds)
    }

    @Test
    fun `calculate uses hold duration as display reps and hold volume conversion`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Plank", weight = 0f, reps = 1, sets = 4, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 30)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 2),
            elapsedSeconds = 600,
            endTime = 1L,
            userMetrics = null,
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal"
        )

        assertEquals(30, result.sessionExercises.single().reps)
        assertEquals(12f, result.sessionExercises.single().volume, 0.01f)
    }

    @Test
    fun `calculate returns positive calories when work is completed`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 80f, reps = 8, sets = 4)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 4),
            elapsedSeconds = 1800,
            endTime = 1L,
            userMetrics = UserMetrics(weightKg = 75f),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal"
        )

        assertTrue(result.session.caloriesBurned > 0f)
    }

    @Test
    fun `calculate copies calorie estimate audit fields onto session`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 70f, reps = 10, sets = 1, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 1),
            elapsedSeconds = 60,
            endTime = 1L,
            userMetrics = UserMetrics(weightKg = 70f, gender = "Other"),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "HARD"
        )

        assertEquals(1, result.session.calorieFormulaVersion)
        assertEquals("STANDARD_MET", result.session.calorieEstimateMode)
        assertEquals("hard", result.session.calorieIntensity)
        assertEquals(70f, result.session.calorieUserWeightKg, 0.01f)
        assertEquals(1f, result.session.calorieMetCorrectionFactor, 0.01f)
        assertEquals(30f, result.session.calorieActiveSeconds, 0.01f)
        assertEquals(30, result.session.calorieRestSeconds)
    }

    @Test
    fun `legacy cloud sessions default calorie audit fields`() {
        val session = CloudWorkoutSession(
            id = 7,
            date = 1L,
            durationSeconds = 60L,
            totalWeightLifted = 100f,
            caloriesBurned = 12f
        ).toLocal()

        assertEquals(1, session.calorieFormulaVersion)
        assertEquals("STANDARD_MET", session.calorieEstimateMode)
        assertEquals("normal", session.calorieIntensity)
        assertEquals(70f, session.calorieUserWeightKg, 0.01f)
        assertEquals(1f, session.calorieMetCorrectionFactor, 0.01f)
        assertEquals(0f, session.calorieActiveSeconds, 0.01f)
        assertEquals(0, session.calorieRestSeconds)
    }

    @Test
    fun `calculate does not inflate volume for skipped exercises and records their names`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Squat", weight = 150f, reps = 5, sets = 3, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 4),
            elapsedSeconds = 900,
            endTime = 1L,
            userMetrics = UserMetrics(weightKg = 80f),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal",
            skippedExerciseIds = setOf(2)
        )

        assertEquals(4 * 10 * 100f, result.session.totalVolume, 0.01f)
        assertTrue(result.sessionExercises.none { it.exerciseName == "Squat" })
        assertEquals(listOf("Squat"), result.session.skippedExerciseNames)
    }

    @Test
    fun `warm-up only exercises are excluded from working volume but kept in history`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Bar Warmup", weight = 20f, reps = 10, sets = 2, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 4, 2 to 2),
            elapsedSeconds = 900,
            endTime = 1L,
            userMetrics = UserMetrics(weightKg = 80f),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal",
            warmUpOnlyExerciseIds = setOf(2)
        )

        assertEquals(4 * 10 * 100f, result.session.totalVolume, 0.01f)

        val warmUpEntry = result.sessionExercises.single { it.exerciseName == "Bar Warmup" }
        assertTrue(warmUpEntry.isWarmUp)
        assertEquals(2 * 10 * 20f, warmUpEntry.volume, 0.01f)
    }

    @Test
    fun `rpe and notes round-trip onto session and exercise entries`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = calculator.calculate(
            exercises = exercises,
            completedSets = mapOf(1 to 3),
            elapsedSeconds = 600,
            endTime = 1L,
            userMetrics = UserMetrics(weightKg = 80f),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal",
            exerciseRpe = mapOf(1 to 8),
            exerciseNotes = mapOf(1 to "felt strong"),
            sessionRpe = 7,
            sessionNotes = "good day"
        )

        assertEquals(7, result.session.rpe)
        assertEquals("good day", result.session.notes)
        val entry = result.sessionExercises.single()
        assertEquals(8, entry.rpe)
        assertEquals("felt strong", entry.notes)
    }
}
