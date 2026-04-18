package com.example.workoutapp.domain.session

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseType
import com.example.workoutapp.data.local.entity.SessionExercise
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.util.CalorieCalculator
import javax.inject.Inject
import kotlin.math.roundToInt

data class SessionCompletionResult(
    val session: WorkoutSession,
    val sessionExercises: List<SessionExercise>
)

class SessionCompletionCalculator @Inject constructor() {
    fun calculate(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        elapsedSeconds: Long,
        endTime: Long,
        userMetrics: UserMetrics?,
        restTimerDuration: Int,
        exerciseSwitchDuration: Int
    ): SessionCompletionResult {
        val userWeight = userMetrics?.weightKg ?: 70f
        var totalWeight = 0f
        var totalVolume = 0f

        completedSets.forEach { (exerciseId, setCount) ->
            val exercise = exercises.find { it.id == exerciseId } ?: return@forEach
            val volume = calculateExerciseVolume(exercise, setCount, userWeight)
            totalWeight += volume
            totalVolume += volume
        }

        val session = WorkoutSession(
            date = endTime,
            durationSeconds = elapsedSeconds,
            totalWeightLifted = totalWeight,
            caloriesBurned = CalorieCalculator.calculateCalories(
                completedSets = completedSets,
                exercises = exercises,
                userMetrics = userMetrics,
                restSecondsBetweenSets = restTimerDuration,
                restSecondsBetweenExercises = exerciseSwitchDuration
            ),
            totalVolume = totalVolume
        )

        val sessionExercises = exercises.mapIndexedNotNull { index, exercise ->
            val setCount = completedSets[exercise.id] ?: 0
            if (setCount <= 0) return@mapIndexedNotNull null

            val displayReps = if (exercise.exerciseType == ExerciseType.HOLD.name) {
                exercise.holdDurationSeconds
            } else {
                exercise.reps
            }

            SessionExercise(
                sessionId = 0,
                exerciseName = exercise.name,
                weight = exercise.weight,
                sets = setCount,
                reps = displayReps,
                volume = calculateExerciseVolume(exercise, setCount, userWeight),
                sortOrder = index
            )
        }

        return SessionCompletionResult(session = session, sessionExercises = sessionExercises)
    }

    private fun calculateExerciseVolume(exercise: Exercise, setCount: Int, userWeight: Float): Float {
        return when (exercise.exerciseType) {
            ExerciseType.HOLD.name -> {
                val holdRepsEquivalent = (exercise.holdDurationSeconds / 5f).roundToInt().coerceAtLeast(1)
                setCount * holdRepsEquivalent * 1f
            }

            ExerciseType.BODYWEIGHT.name -> setCount * exercise.reps * userWeight
            else -> setCount * exercise.reps * exercise.weight
        }
    }
}
