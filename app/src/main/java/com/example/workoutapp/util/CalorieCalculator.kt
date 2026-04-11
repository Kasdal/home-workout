package com.example.workoutapp.util

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.data.local.entity.UserMetrics
import kotlin.math.max
import kotlin.math.min

object CalorieCalculator {

    private const val MET_HOLD = 4.0f
    private const val MET_BODYWEIGHT = 4.5f
    private const val MET_STANDARD = 8.0f

    private const val CAL_PER_KG_PER_SEC_AT_MET8 = 0.1556f
    private const val MAX_VOLUME_SCALING = 1.5f
    private const val MIN_CALORIES = 1f

    fun calculateCalories(
        durationSeconds: Int,
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics?,
    ): Float {
        if (durationSeconds <= 0) return MIN_CALORIES

        val weightKg = userMetrics?.weightKg ?: 70f
        val age = userMetrics?.age ?: 30

        val totalVolume = computeTotalVolume(completedSets, exercises, weightKg)
        val avgMet = computeAverageMet(completedSets, exercises)
        val ageFactor = computeAgeFactor(age)
        val volumeScaling = computeVolumeScaling(totalVolume, weightKg, durationSeconds)

        val calPerSecond = avgMet * CAL_PER_KG_PER_SEC_AT_MET8
        val calories = calPerSecond * durationSeconds * ageFactor * volumeScaling

        return max(calories, MIN_CALORIES)
    }

    private fun computeTotalVolume(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userWeightKg: Float,
    ): Float {
        var total = 0f
        completedSets.forEach { (exId, setCount) ->
            val exercise = exercises.find { it.id == exId } ?: return@forEach
            if (setCount <= 0) return@forEach

            val volume = when (exercise.exerciseType) {
                ExerciseType.HOLD.name -> {
                    val repsEquiv = (exercise.holdDurationSeconds / 5f).toInt().coerceAtLeast(1)
                    setCount * repsEquiv * 1f
                }
                ExerciseType.BODYWEIGHT.name -> {
                    setCount * exercise.reps * userWeightKg
                }
                else -> {
                    setCount * exercise.reps * exercise.weight
                }
            }
            total += volume
        }
        return total
    }

    private fun computeAverageMet(completedSets: Map<Int, Int>, exercises: List<Exercise>): Float {
        var totalWeight = 0f
        var weightedMetSum = 0f

        completedSets.forEach { (exId, setCount) ->
            val exercise = exercises.find { it.id == exId } ?: return@forEach
            if (setCount <= 0) return@forEach

            val met = when (exercise.exerciseType) {
                ExerciseType.HOLD.name -> MET_HOLD
                ExerciseType.BODYWEIGHT.name -> MET_BODYWEIGHT
                else -> MET_STANDARD
            }
            val weight = setCount * exercise.reps * max(exercise.weight, 1f)
            totalWeight += weight
            weightedMetSum += met * weight
        }

        return if (totalWeight > 0f) weightedMetSum / totalWeight else MET_STANDARD
    }

    private fun computeAgeFactor(age: Int): Float {
        if (age <= 30) return 1.0f
        val decayRate = 0.015f
        val decadesOver30 = (age - 30) / 10f
        return max(0.7f, 1.0f - decayRate * decadesOver30)
    }

    private fun computeVolumeScaling(totalVolume: Float, weightKg: Float, durationSeconds: Int): Float {
        if (totalVolume <= 0f || durationSeconds <= 0 || weightKg <= 0f) return 0.5f
        val theoreticalMax = weightKg * durationSeconds
        val ratio = totalVolume / theoreticalMax
        return min(ratio * 15f, MAX_VOLUME_SCALING).coerceAtLeast(0.5f)
    }
}
