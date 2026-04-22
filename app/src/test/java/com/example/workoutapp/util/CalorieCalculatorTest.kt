package com.example.workoutapp.util

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.UserMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieCalculatorTest {

    private fun calories(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics? = null,
        restSecondsBetweenSets: Int = 30,
        restSecondsBetweenExercises: Int = 60,
    ): Float {
        return CalorieCalculator.calculateCalories(
            completedSets = completedSets,
            exercises = exercises,
            userMetrics = userMetrics,
            restSecondsBetweenSets = restSecondsBetweenSets,
            restSecondsBetweenExercises = restSecondsBetweenExercises
        )
    }

    @Test
    fun `no completed sets returns minimum calories`() {
        val result = calories(emptyMap(), emptyList())
        assertEquals(1f, result, 0.01f)
    }

    @Test
    fun `higher body weight increases calories`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Squat", weight = 100f, reps = 5, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val lightUser = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 60f, age = 30))
        val heavyUser = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 100f, age = 30))

        assertTrue(heavyUser > lightUser)
    }

    @Test
    fun `more sets increase calories because active and rest time increase`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Row", weight = 60f, reps = 10, sets = 4, exerciseType = ExerciseType.STANDARD.name)
        )

        val fewSets = calories(mapOf(1 to 2), exercises, UserMetrics(weightKg = 70f, age = 30))
        val manySets = calories(mapOf(1 to 4), exercises, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(manySets > fewSets)
    }

    @Test
    fun `longer programmed rest increases calories modestly`() {
        val exercises = listOf(
            Exercise(id = 1, name = "Bench", weight = 80f, reps = 8, sets = 4, exerciseType = ExerciseType.STANDARD.name),
            Exercise(id = 2, name = "Plank", weight = 0f, reps = 1, sets = 3, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 60)
        )

        val shortRests = calories(
            completedSets = mapOf(1 to 4, 2 to 3),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 75f, age = 30),
            restSecondsBetweenSets = 15,
            restSecondsBetweenExercises = 30
        )
        val longRests = calories(
            completedSets = mapOf(1 to 4, 2 to 3),
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 75f, age = 30),
            restSecondsBetweenSets = 30,
            restSecondsBetweenExercises = 60
        )

        assertTrue(longRests > shortRests)
    }

    @Test
    fun `bodyweight work burns more than hold work for same set count`() {
        val bodyweight = listOf(
            Exercise(id = 1, name = "Push-up", weight = 0f, reps = 15, sets = 4, exerciseType = ExerciseType.BODYWEIGHT.name)
        )
        val hold = listOf(
            Exercise(id = 1, name = "Plank", weight = 0f, reps = 1, sets = 4, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 20)
        )

        val bodyweightCalories = calories(mapOf(1 to 4), bodyweight, UserMetrics(weightKg = 70f, age = 30))
        val holdCalories = calories(mapOf(1 to 4), hold, UserMetrics(weightKg = 70f, age = 30))

        assertTrue(bodyweightCalories > holdCalories)
    }

    @Test
    fun `realistic hour-long structured workout stays in realistic range`() {
        val exercises = buildList {
            repeat(11) { index ->
                add(
                    Exercise(
                        id = index + 1,
                        name = "Lift ${index + 1}",
                        weight = 50f,
                        reps = 13,
                        sets = 4,
                        exerciseType = ExerciseType.STANDARD.name
                    )
                )
            }
            add(Exercise(id = 12, name = "Push-up", weight = 0f, reps = 15, sets = 2, exerciseType = ExerciseType.BODYWEIGHT.name))
            add(Exercise(id = 13, name = "Plank", weight = 0f, reps = 1, sets = 1, exerciseType = ExerciseType.HOLD.name, holdDurationSeconds = 180))
        }

        val completedSets = (1..11).associateWith { 4 }.toMutableMap().apply {
            this[12] = 2
            this[13] = 1
        }

        val result = calories(
            completedSets = completedSets,
            exercises = exercises,
            userMetrics = UserMetrics(weightKg = 100f, age = 44),
            restSecondsBetweenSets = 30,
            restSecondsBetweenExercises = 60
        )

        assertTrue(result in 250f..450f)
    }
}
