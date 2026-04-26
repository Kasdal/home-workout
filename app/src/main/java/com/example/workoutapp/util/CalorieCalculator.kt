package com.example.workoutapp.util

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.UserMetrics
import kotlin.math.max
import kotlin.math.min

object CalorieCalculator {

    private const val MET_HOLD = 3.8f
    private const val MET_BODYWEIGHT = 7.0f
    private const val MET_STANDARD_LIGHT = 4.5f
    private const val MET_STANDARD_HEAVY = 7.5f
    private const val MET_BREAK = 1.8f
    private const val STANDARD_SECONDS_PER_REP = 3.0f
    private const val BODYWEIGHT_SECONDS_PER_REP = 2.0f
    private const val MIN_ACTIVE_SET_SECONDS = 8f
    private const val MAX_ACTIVE_SET_SECONDS = 75f

    private const val CALORIES_PER_MET_PER_KG_PER_SECOND = 3.5f / 200f / 60f
    private const val MIN_CALORIES = 1f

    fun calculateCalories(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics?,
        restSecondsBetweenSets: Int,
        restSecondsBetweenExercises: Int,
        elapsedSeconds: Long,
        intensity: String = "normal"
    ): Float {
        if (completedSets.values.none { it > 0 }) return MIN_CALORIES

        val weightKg = userMetrics?.weightKg ?: 70f
        val intensityMultiplier = intensityMultiplier(intensity)
        val estimatedActiveSecondsByExercise = exercises.associate { exercise ->
            exercise.id to estimatedActiveSeconds(exercise, completedSets[exercise.id] ?: 0)
        }
        val estimatedActiveSeconds = estimatedActiveSecondsByExercise.values.sum()
        val boundedActiveSeconds = if (elapsedSeconds > 0 && estimatedActiveSeconds > elapsedSeconds) {
            elapsedSeconds.toFloat()
        } else {
            estimatedActiveSeconds
        }
        val activeScale = if (estimatedActiveSeconds > 0f) boundedActiveSeconds / estimatedActiveSeconds else 0f

        val activeCalories = exercises.sumOf { exercise ->
            val setCount = completedSets[exercise.id] ?: 0
            if (setCount <= 0) {
                0.0
            } else {
                val activeSeconds = (estimatedActiveSecondsByExercise[exercise.id] ?: 0f) * activeScale

                (metForExercise(exercise, weightKg) * intensityMultiplier * weightKg * activeSeconds * CALORIES_PER_MET_PER_KG_PER_SECOND).toDouble()
            }
        }.toFloat()

        val startedExercises = exercises.count { (completedSets[it.id] ?: 0) > 0 }
        val estimatedBetweenSetBreakSeconds = exercises.sumOf { exercise ->
            val setCount = completedSets[exercise.id] ?: 0
            max(setCount - 1, 0) * restSecondsBetweenSets
        }
        val estimatedBetweenExerciseBreakSeconds = max(startedExercises - 1, 0) * restSecondsBetweenExercises
        val estimatedBreakSeconds = estimatedBetweenSetBreakSeconds + estimatedBetweenExerciseBreakSeconds
        val breakSeconds = if (elapsedSeconds > 0) {
            max(elapsedSeconds - boundedActiveSeconds.toLong(), 0L).toInt()
        } else {
            estimatedBreakSeconds
        }
        val breakCalories = MET_BREAK * weightKg * breakSeconds * CALORIES_PER_MET_PER_KG_PER_SECOND

        return max(activeCalories + breakCalories, MIN_CALORIES)
    }

    private fun estimatedActiveSeconds(exercise: Exercise, setCount: Int): Float {
        if (setCount <= 0) return 0f

        val secondsPerSet = when (exercise.exerciseType) {
            ExerciseType.HOLD.name -> exercise.holdDurationSeconds.toFloat()
            ExerciseType.BODYWEIGHT.name -> exercise.reps * BODYWEIGHT_SECONDS_PER_REP
            else -> exercise.reps * STANDARD_SECONDS_PER_REP
        }.coerceIn(MIN_ACTIVE_SET_SECONDS, MAX_ACTIVE_SET_SECONDS)

        return setCount * secondsPerSet
    }

    private fun metForExercise(exercise: Exercise, userWeightKg: Float): Float {
        return when (exercise.exerciseType) {
            ExerciseType.HOLD.name -> MET_HOLD
            ExerciseType.BODYWEIGHT.name -> MET_BODYWEIGHT
            else -> {
                val loadRatio = if (userWeightKg > 0f) exercise.weight / userWeightKg else 0f
                min(MET_STANDARD_LIGHT + (loadRatio * 2.5f), MET_STANDARD_HEAVY)
            }
        }
    }

    private fun intensityMultiplier(intensity: String): Float {
        return when (intensity.lowercase()) {
            "easy" -> 0.85f
            "hard" -> 1.15f
            else -> 1.0f
        }
    }
}
