package com.example.workoutapp.util

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.CalorieCategory
import com.example.workoutapp.model.ExerciseType
import com.example.workoutapp.model.UserMetrics
import kotlin.math.max
import kotlin.math.min

data class CalorieEstimate(
    val calories: Float,
    val formulaVersion: Int,
    val mode: CalorieEstimateMode,
    val userWeightKg: Float,
    val metCorrectionFactor: Float,
    val activeSeconds: Float,
    val restSeconds: Int,
    val activeCalories: Float,
    val restCalories: Float,
    val intensity: String
)

enum class CalorieEstimateMode { STANDARD_MET, PERSONALIZED_MET }

object CalorieCalculator {

    private const val MET_HOLD = 3.8f
    private const val MET_LIGHT_BODYWEIGHT = 4.0f
    private const val MET_BODYWEIGHT = 7.0f
    private const val MET_LIGHT_RESISTANCE = 3.5f
    private const val MET_STANDARD_LIGHT = 4.5f
    private const val MET_HEAVY_COMPOUND = 6.0f
    private const val MET_LIGHT_RESISTANCE_CAP = 6.0f
    private const val MET_STANDARD_HEAVY = 7.5f
    private const val MET_HEAVY_COMPOUND_CAP = 8.5f
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
        return calculateEstimate(
            completedSets = completedSets,
            exercises = exercises,
            userMetrics = userMetrics,
            restSecondsBetweenSets = restSecondsBetweenSets,
            restSecondsBetweenExercises = restSecondsBetweenExercises,
            elapsedSeconds = elapsedSeconds,
            intensity = intensity
        ).calories
    }

    fun calculateEstimate(
        completedSets: Map<Int, Int>,
        exercises: List<Exercise>,
        userMetrics: UserMetrics?,
        restSecondsBetweenSets: Int,
        restSecondsBetweenExercises: Int,
        elapsedSeconds: Long,
        intensity: String = "normal"
    ): CalorieEstimate {
        val weightKg = userMetrics?.weightKg ?: 70f
        val normalizedIntensity = normalizeIntensity(intensity)
        val metCorrection = metCorrection(userMetrics, weightKg)

        if (completedSets.values.none { it > 0 }) {
            return CalorieEstimate(
                calories = MIN_CALORIES,
                formulaVersion = 1,
                mode = metCorrection.mode,
                userWeightKg = weightKg,
                metCorrectionFactor = metCorrection.factor,
                activeSeconds = 0f,
                restSeconds = 0,
                activeCalories = 0f,
                restCalories = 0f,
                intensity = normalizedIntensity
            )
        }

        val intensityMultiplier = intensityMultiplier(normalizedIntensity)
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

                (metForExercise(exercise, weightKg) * metCorrection.factor * intensityMultiplier * weightKg * activeSeconds * CALORIES_PER_MET_PER_KG_PER_SECOND).toDouble()
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
        val breakCalories = MET_BREAK * metCorrection.factor * weightKg * breakSeconds * CALORIES_PER_MET_PER_KG_PER_SECOND

        return CalorieEstimate(
            calories = max(activeCalories + breakCalories, MIN_CALORIES),
            formulaVersion = 1,
            mode = metCorrection.mode,
            userWeightKg = weightKg,
            metCorrectionFactor = metCorrection.factor,
            activeSeconds = boundedActiveSeconds,
            restSeconds = breakSeconds,
            activeCalories = activeCalories,
            restCalories = breakCalories,
            intensity = normalizedIntensity
        )
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
        return when (calorieCategoryFor(exercise)) {
            CalorieCategory.ISOMETRIC_HOLD -> MET_HOLD
            CalorieCategory.LIGHT_BODYWEIGHT -> MET_LIGHT_BODYWEIGHT
            CalorieCategory.VIGOROUS_BODYWEIGHT -> MET_BODYWEIGHT
            CalorieCategory.LIGHT_RESISTANCE -> resistanceMet(exercise, userWeightKg, MET_LIGHT_RESISTANCE, MET_LIGHT_RESISTANCE_CAP)
            CalorieCategory.MODERATE_RESISTANCE -> resistanceMet(exercise, userWeightKg, MET_STANDARD_LIGHT, MET_STANDARD_HEAVY)
            CalorieCategory.HEAVY_COMPOUND -> resistanceMet(exercise, userWeightKg, MET_HEAVY_COMPOUND, MET_HEAVY_COMPOUND_CAP)
        }
    }

    private fun calorieCategoryFor(exercise: Exercise): CalorieCategory {
        return exercise.calorieCategory
            ?.let { runCatching { CalorieCategory.valueOf(it) }.getOrNull() }
            ?: when (exercise.exerciseType) {
                ExerciseType.HOLD.name -> CalorieCategory.ISOMETRIC_HOLD
                ExerciseType.BODYWEIGHT.name -> CalorieCategory.VIGOROUS_BODYWEIGHT
                else -> CalorieCategory.MODERATE_RESISTANCE
            }
    }

    private fun resistanceMet(exercise: Exercise, userWeightKg: Float, baseMet: Float, capMet: Float): Float {
        val loadRatio = if (userWeightKg > 0f) exercise.weight / userWeightKg else 0f
        return min(baseMet + (loadRatio * 2.5f), capMet)
    }

    private fun intensityMultiplier(intensity: String): Float {
        return when (intensity) {
            "easy" -> 0.85f
            "hard" -> 1.15f
            else -> 1.0f
        }
    }

    private fun normalizeIntensity(intensity: String): String {
        return when (intensity.trim().lowercase()) {
            "easy" -> "easy"
            "hard" -> "hard"
            else -> "normal"
        }
    }

    private fun metCorrection(userMetrics: UserMetrics?, weightKg: Float): MetCorrection {
        if (userMetrics == null || weightKg <= 0f || userMetrics.heightCm <= 0f || userMetrics.age <= 0) {
            return MetCorrection.standard()
        }

        val rmrKcalPerDay = when (userMetrics.gender.trim().lowercase()) {
            "male" -> 66.4730f + (5.0033f * userMetrics.heightCm) + (13.7516f * weightKg) - (6.7550f * userMetrics.age)
            "female" -> 655.0955f + (1.8496f * userMetrics.heightCm) + (9.5634f * weightKg) - (4.6756f * userMetrics.age)
            else -> return MetCorrection.standard()
        }

        if (rmrKcalPerDay <= 0f) return MetCorrection.standard()

        val rmrMlKgMin = (((rmrKcalPerDay / 1440f) / 5f) / weightKg) * 1000f
        if (rmrMlKgMin <= 0f) return MetCorrection.standard()

        return MetCorrection(
            factor = (3.5f / rmrMlKgMin).coerceIn(0.85f, 1.25f),
            mode = CalorieEstimateMode.PERSONALIZED_MET
        )
    }

    private data class MetCorrection(
        val factor: Float,
        val mode: CalorieEstimateMode
    ) {
        companion object {
            fun standard() = MetCorrection(1f, CalorieEstimateMode.STANDARD_MET)
        }
    }
}
