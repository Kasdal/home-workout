package com.example.workoutapp.util

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.data.local.entity.UserMetrics
import kotlin.math.max

object CalorieCalculator {

    private const val MET_HOLD = 3.8f
    private const val MET_BODYWEIGHT = 8.0f
    private const val MET_STANDARD = 6.0f
    private const val MET_BREAK = 1.8f
    private const val ESTIMATED_ACTIVE_SET_SECONDS = 20

    private const val CALORIES_PER_MET_PER_KG_PER_SECOND = 3.5f / 200f / 60f
    private const val MIN_CALORIES = 1f

    fun calculateCalories(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics?,
        restSecondsBetweenSets: Int,
        restSecondsBetweenExercises: Int,
    ): Float {
        if (completedSets.values.none { it > 0 }) return MIN_CALORIES

        val weightKg = userMetrics?.weightKg ?: 70f
        val activeCalories = exercises.sumOf { exercise ->
            val setCount = completedSets[exercise.id] ?: 0
            if (setCount <= 0) {
                0.0
            } else {
                val activeSeconds = when (exercise.exerciseType) {
                    ExerciseType.HOLD.name -> setCount * exercise.holdDurationSeconds
                    else -> setCount * ESTIMATED_ACTIVE_SET_SECONDS
                }

                (metForExercise(exercise) * weightKg * activeSeconds * CALORIES_PER_MET_PER_KG_PER_SECOND).toDouble()
            }
        }.toFloat()

        val startedExercises = exercises.count { (completedSets[it.id] ?: 0) > 0 }
        val betweenSetBreakSeconds = exercises.sumOf { exercise ->
            val setCount = completedSets[exercise.id] ?: 0
            max(setCount - 1, 0) * restSecondsBetweenSets
        }
        val betweenExerciseBreakSeconds = max(startedExercises - 1, 0) * restSecondsBetweenExercises
        val breakSeconds = betweenSetBreakSeconds + betweenExerciseBreakSeconds
        val breakCalories = MET_BREAK * weightKg * breakSeconds * CALORIES_PER_MET_PER_KG_PER_SECOND

        return max(activeCalories + breakCalories, MIN_CALORIES)
    }

    private fun metForExercise(exercise: Exercise): Float {
        return when (exercise.exerciseType) {
            ExerciseType.HOLD.name -> MET_HOLD
            ExerciseType.BODYWEIGHT.name -> MET_BODYWEIGHT
            else -> MET_STANDARD
        }
    }
}
