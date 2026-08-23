package com.example.workoutapp.domain.session

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.ExerciseType
import javax.inject.Inject

data class ActiveExerciseSelection(
    val activeExerciseId: Int?,
    val activeExerciseMode: ExerciseSessionMode
)

sealed interface PostSetTimerRequest {
    data object None : PostSetTimerRequest
    data class Start(val seconds: Int, val timerType: CountdownType = CountdownType.REST) : PostSetTimerRequest
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
        exerciseSwitchDuration: Int,
        skippedExerciseIds: Set<Int> = emptySet()
    ): SessionProgressUpdate {
        val exercise = exercises.find { it.id == exerciseId }
            ?: return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets, skippedExerciseIds),
                timerRequest = PostSetTimerRequest.None
            )

        val current = completedSets.toMutableMap()
        val currentCount = current[exerciseId] ?: 0
        if (currentCount >= exercise.sets) {
            return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets, skippedExerciseIds),
                timerRequest = PostSetTimerRequest.None
            )
        }

        val newCount = currentCount + 1
        current[exerciseId] = newCount

        val updatedSets = current.toMap()
        val timerRequest = when {
            exercise.exerciseType == ExerciseType.HOLD.name -> PostSetTimerRequest.Start(
                exercise.holdDurationSeconds,
                CountdownType.HOLD
            )
            newCount >= exercise.sets -> PostSetTimerRequest.Start(exerciseSwitchDuration, CountdownType.SWITCH)
            else -> PostSetTimerRequest.Start(restTimerDuration, CountdownType.REST)
        }

        return SessionProgressUpdate(
            completedSets = updatedSets,
            activeExerciseSelection = selectActiveExercise(exercises, updatedSets, skippedExerciseIds),
            timerRequest = timerRequest
        )
    }

    fun undoSet(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int,
        skippedExerciseIds: Set<Int> = emptySet()
    ): SessionProgressUpdate {
        val current = completedSets.toMutableMap()
        val currentCount = current[exerciseId] ?: 0
        if (currentCount <= 0) {
            return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets, skippedExerciseIds),
                timerRequest = PostSetTimerRequest.None
            )
        }

        current[exerciseId] = currentCount - 1
        val updatedSets = current.toMap()

        return SessionProgressUpdate(
            completedSets = updatedSets,
            activeExerciseSelection = selectActiveExercise(exercises, updatedSets, skippedExerciseIds),
            timerRequest = PostSetTimerRequest.None
        )
    }

    fun finishExercise(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int,
        exerciseSwitchDuration: Int,
        skippedExerciseIds: Set<Int> = emptySet()
    ): SessionProgressUpdate {
        val exercise = exercises.find { it.id == exerciseId }
            ?: return SessionProgressUpdate(
                completedSets = completedSets,
                activeExerciseSelection = selectActiveExercise(exercises, completedSets, skippedExerciseIds),
                timerRequest = PostSetTimerRequest.None
            )

        val updatedSets = completedSets.toMutableMap().apply {
            put(exerciseId, exercise.sets)
        }.toMap()

        return SessionProgressUpdate(
            completedSets = updatedSets,
            activeExerciseSelection = selectActiveExercise(exercises, updatedSets, skippedExerciseIds),
            timerRequest = PostSetTimerRequest.Start(exerciseSwitchDuration, CountdownType.SWITCH)
        )
    }

    fun skipExercise(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int,
        exerciseSwitchDuration: Int,
        skippedExerciseIds: Set<Int> = emptySet()
    ): SessionProgressUpdate {
        val updatedSkipped = skippedExerciseIds + exerciseId

        return SessionProgressUpdate(
            completedSets = completedSets,
            activeExerciseSelection = selectActiveExercise(exercises, completedSets, updatedSkipped),
            timerRequest = PostSetTimerRequest.Start(exerciseSwitchDuration, CountdownType.SWITCH)
        )
    }

    fun selectActiveExercise(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        skippedExerciseIds: Set<Int> = emptySet()
    ): ActiveExerciseSelection {
        val activeExercise = exercises.firstOrNull { exercise ->
            exercise.id !in skippedExerciseIds &&
                (completedSets[exercise.id] ?: 0) < exercise.sets
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
