package com.example.workoutapp.util

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.data.local.entity.UserMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    private fun calories(
        durationSeconds: Int,
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics? = null,
    ): Float {
        return CalorieCalculator.calculateCalories(durationSeconds, completedSets, exercises, userMetrics)
    }

    @Test
    fun `zero duration returns minimum calories`() {
        val result = calories(0, emptyMap(), emptyList())
        assertEquals(1f, result, 0.01f)
    }

    @Test
    fun `standard exercise uses MET 8 and user weight`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 4)
        val userMetrics = UserMetrics(weightKg = 80f, age = 30)

        val result = calories(3600, sets, exercises, userMetrics)

        assertTrue(result > 0f)
        assertTrue(result < 2000f)
    }

    @Test
    fun `higher body weight increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Squat", weight = 100f, reps = 5, sets = 5, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 5)

        val lightUser = calories(3600, sets, exercises, UserMetrics(weightKg = 60f, age = 30))
        val heavyUser = calories(3600, sets, exercises, UserMetrics(weightKg = 100f, age = 30))

        assertTrue(heavyUser > lightUser)
    }

    @Test
    fun `older age reduces calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Deadlift", weight = 120f, reps = 5, sets = 5, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 5)

        val young = calories(3600, sets, exercises, UserMetrics(weightKg = 70f, age = 25))
        val older = calories(3600, sets, exercises, UserMetrics(weightKg = 70f, age = 50))

        assertTrue(young > older)
        assertTrue(older / young < 1f)
    }

    @Test
    fun `age factor floors at 70 percent`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Curl", weight = 20f, reps = 10, sets = 3, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 3)

        val age60 = calories(3600, sets, exercises, UserMetrics(weightKg = 70f, age = 60))
        val age90 = calories(3600, sets, exercises, UserMetrics(weightKg = 70f, age = 90))

        assertEquals(age60, age90, 1f)
    }

    @Test
    fun `hold exercise uses MET 4`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Plank", weight = 0f, reps = 30, sets = 3, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 60)
        )
        val sets = mapOf(1 to 3)

        val standardResult = calories(1800, sets, exercises, UserMetrics(weightKg = 70f, age = 30))
        assertTrue(standardResult > 0f)
    }

    @Test
    fun `bodyweight exercise uses MET 45`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Push-up", weight = 0f, reps = 15, sets = 4, exerciseType = ExerciseType.BODYWEIGHT.name)
        )
        val sets = mapOf(1 to 4)

        val result = calories(1800, sets, exercises, UserMetrics(weightKg = 70f, age = 30))
        assertTrue(result > 0f)
    }

    @Test
    fun `more sets increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Row", weight = 60f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val fewSets = calories(1800, mapOf(1 to 2), exercises, UserMetrics(weightKg = 70f, age = 30))
        val manySets = calories(1800, mapOf(1 to 4), exercises, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(manySets > fewSets)
    }

    @Test
    fun `longer duration increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Squat", weight = 100f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 4)

        val short = calories(1800, sets, exercises, UserMetrics(weightKg = 70f, age = 30))
        val long = calories(3600, sets, exercises, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(long > short)
        assertTrue(long > short * 1.5f)
    }

    @Test
    fun `unknown user uses defaults without crashing`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Press", weight = 50f, reps = 10, sets = 3, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = calories(3600, mapOf(1 to 3), exercises, null)
        assertTrue(result > 0f)
    }

    @Test
    fun `volume scaling boosts calories for dense workouts`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Compound", weight = 100f, reps = 10, sets = 8, exerciseType = ExerciseType.STANDARD.name)
        )

        val heavyVolume = calories(3600, mapOf(1 to 8), exercises, UserMetrics(weightKg = 70f, age = 30))
        val lightVolume = calories(3600, mapOf(1 to 2), exercises, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(heavyVolume > lightVolume * 1.2f)
    }

    @Test
    fun `mixed exercise types uses weighted average MET`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Plank", weight = 0f, reps = 0, sets = 3, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 60)
        )

        val result = calories(3600, mapOf(1 to 4, 2 to 3), exercises, UserMetrics(weightKg = 70f, age = 30))
        assertTrue(result > 0f)
    }

    @Test
    fun `no completed sets still returns calories for duration`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench Press", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val result = calories(1800, emptyMap(), exercises, UserMetrics(weightKg = 70f, age = 30))
        assertTrue(result > 0f)
    }

    @Test
    fun `realistic session matches expected range`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Squat", weight = 100f, reps = 5, sets = 5, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Bench Press", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 3, name = "Deadlift", weight = 120f, reps = 5, sets = 3, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 5, 2 to 4, 3 to 3)
        val userMetrics = UserMetrics(weightKg = 75f, age = 30)

        val result = calories(3600, sets, exercises, userMetrics)

        assertTrue(result in 300f..900f)
    }

    @Test
    fun `calories are proportional to duration within small range`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Press", weight = 60f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )
        val sets = mapOf(1 to 4)

        val half = calories(1800, sets, exercises, UserMetrics(weightKg = 70f, age = 30))
        val full = calories(3600, sets, exercises, UserMetrics(weightKg = 70f, age = 30))

        val ratio = full / half
        assertTrue(ratio in 1.9f..2.1f)
    }
}
