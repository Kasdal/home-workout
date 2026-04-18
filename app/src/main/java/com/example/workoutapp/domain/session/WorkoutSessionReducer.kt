package com.example.workoutapp.domain.session

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.ExerciseSessionMode
import com.example.workoutapp.data.local.entity.ExerciseType
import javax.inject.Inject

data class ActiveExerciseSelection(
    val activeExerciseId: Int?,
    val activeExerciseMode: ExerciseSessionMode
)

sealed interface PostSetTimerRequest {
    data object None : PostSetTimerRequest
    data class Start(val seconds: Int) : PostSetTimerRequest
}

data class SessionProgressUpdate(
    val completedSets: Map<Int, Int>,
    val activeExerciseSelection: ActiveExerciseSelection,
    val timerRequest: PostSetTimerRequest
)

class WorkoutSessionReducer @Inject constructor() {
    fun completeNextSet(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int,
        restTimerDuration: Int,
        exerciseSwitchDuration: Int
    ): SessionProgressUpdate {
        val exercise = exercises.find { it.id == exerciseId }
            ?: return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets),
                timerRequest = PostSetTimerRequest.None
            )

        val current = completedSets.toMutableMap()
        val currentCount = current[exerciseId] ?: 0
        if (currentCount >= exercise.sets) {
            return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets),
                timerRequest = PostSetTimerRequest.None
            )
        }

        val newCount = currentCount + 1
        current[exerciseId] = newCount

        val updatedSets = current.toMap()
        val timerRequest = when {
            exercise.exerciseType == ExerciseType.HOLD.name -> PostSetTimerRequest.Start(exercise.holdDurationSeconds)
            newCount >= exercise.sets -> PostSetTimerRequest.Start(exerciseSwitchDuration)
            else -> PostSetTimerRequest.Start(restTimerDuration)
        }

        return SessionProgressUpdate(
            completedSets = updatedSets,
            activeExerciseSelection = selectActiveExercise(exercises, updatedSets),
            timerRequest = timerRequest
        )
    }

    fun undoSet(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int
    ): SessionProgressUpdate {
        val current = completedSets.toMutableMap()
        val currentCount = current[exerciseId] ?: 0
        if (currentCount <= 0) {
            return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets),
                timerRequest = PostSetTimerRequest.None
            )
        }

        current[exerciseId] = currentCount - 1
        val updatedSets = current.toMap()

        return SessionProgressUpdate(
            completedSets = updatedSets,
            activeExerciseSelection = selectActiveExercise(exercises, updatedSets),
            timerRequest = PostSetTimerRequest.None
        )
    }

    fun selectActiveExercise(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>
    ): ActiveExerciseSelection {
        val activeExercise = exercises.firstOrNull { exercise ->
            val completed = completedSets[exercise.id] ?: 0
            completed < exercise.sets
        }

        val mode = when {
            activeExercise == null -> ExerciseSessionMode.MANUAL_REPS
            activeExercise.exerciseType == ExerciseType.HOLD.name -> ExerciseSessionMode.HOLD_TIMER
            activeExercise.usesSensor -> ExerciseSessionMode.SENSOR_REPS
            else -> ExerciseSessionMode.MANUAL_REPS
        }

        return ActiveExerciseSelection(
            activeExerciseId = activeExercise?.id,
            activeExerciseMode = mode
        )
    }
}
