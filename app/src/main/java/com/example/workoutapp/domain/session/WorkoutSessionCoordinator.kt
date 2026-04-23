package com.example.workoutapp.domain.session

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.data.repository.SessionHistoryRepository
import javax.inject.Inject

data class WorkoutSessionStateUpdate(
    val completedSets: Map<Int, Int>,
    val activeExerciseSelection: ActiveExerciseSelection
)

data class WorkoutSessionPolicyResult(
    val didUpdate: Boolean,
    val stateUpdate: WorkoutSessionStateUpdate?,
    val timerRequest: PostSetTimerRequest = PostSetTimerRequest.None
)

data class WorkoutSessionPersistenceResult(
    val completedSession: WorkoutSession,
    val stateUpdate: WorkoutSessionStateUpdate
)

class WorkoutSessionCoordinator @Inject constructor(
    private val sessionReducer: WorkoutSessionReducer,
    private val sessionCompletionCalculator: SessionCompletionCalculator,
    private val sessionHistoryRepository: SessionHistoryRepository
) {
    fun startSession(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>
    ): WorkoutSessionStateUpdate {
        return WorkoutSessionStateUpdate(
            completedSets = completedSets,
            activeExerciseSelection = sessionReducer.selectActiveExercise(
                exercises = exercises,
                completedSets = completedSets
            )
        )
    }

    fun completeNextSet(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int,
        restTimerDuration: Int,
        exerciseSwitchDuration: Int
    ): WorkoutSessionPolicyResult {
        val reducerUpdate = sessionReducer.completeNextSet(
            exercises = exercises,
            completedSets = completedSets,
            exerciseId = exerciseId,
            restTimerDuration = restTimerDuration,
            exerciseSwitchDuration = exerciseSwitchDuration
        )

        if (reducerUpdate.completedSets == completedSets) {
            return WorkoutSessionPolicyResult(
                didUpdate = false,
                stateUpdate = null
            )
        }

        return WorkoutSessionPolicyResult(
            didUpdate = true,
            stateUpdate = WorkoutSessionStateUpdate(
                completedSets = reducerUpdate.completedSets,
                activeExerciseSelection = reducerUpdate.activeExerciseSelection
            ),
            timerRequest = reducerUpdate.timerRequest
        )
    }

    fun undoSet(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        exerciseId: Int,
        undoEnabled: Boolean
    ): WorkoutSessionPolicyResult {
        if (!undoEnabled) {
            return WorkoutSessionPolicyResult(
                didUpdate = false,
                stateUpdate = null
            )
        }

        val reducerUpdate = sessionReducer.undoSet(
            exercises = exercises,
            completedSets = completedSets,
            exerciseId = exerciseId
        )

        if (reducerUpdate.completedSets == completedSets) {
            return WorkoutSessionPolicyResult(
                didUpdate = false,
                stateUpdate = null
            )
        }

        return WorkoutSessionPolicyResult(
            didUpdate = true,
            stateUpdate = WorkoutSessionStateUpdate(
                completedSets = reducerUpdate.completedSets,
                activeExerciseSelection = reducerUpdate.activeExerciseSelection
            )
        )
    }

    suspend fun completeSession(
        exercises: List<Exercise>,
        completedSets: Map<Int, Int>,
        elapsedSeconds: Long,
        endTime: Long,
        userMetrics: UserMetrics?,
        restTimerDuration: Int,
        exerciseSwitchDuration: Int
    ): WorkoutSessionPersistenceResult {
        val completion = sessionCompletionCalculator.calculate(
            exercises = exercises,
            completedSets = completedSets,
            elapsedSeconds = elapsedSeconds,
            endTime = endTime,
            userMetrics = userMetrics,
            restTimerDuration = restTimerDuration,
            exerciseSwitchDuration = exerciseSwitchDuration
        )

        val sessionId = sessionHistoryRepository.saveSession(completion.session).toInt()
        val sessionExercises = completion.sessionExercises.map {
            it.copy(sessionId = sessionId)
        }

        if (sessionExercises.isNotEmpty()) {
            sessionHistoryRepository.saveSessionExercises(sessionExercises)
        }

        return WorkoutSessionPersistenceResult(
            completedSession = completion.session.copy(id = sessionId),
            stateUpdate = WorkoutSessionStateUpdate(
                completedSets = emptyMap(),
                activeExerciseSelection = ActiveExerciseSelection(
                    activeExerciseId = null,
                    activeExerciseMode = ExerciseSessionMode.MANUAL_REPS
                )
            )
        )
    }
}
