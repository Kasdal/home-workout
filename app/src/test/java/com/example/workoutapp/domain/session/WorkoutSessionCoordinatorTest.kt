package com.example.workoutapp.domain.session

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.ExerciseSessionMode
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.model.WorkoutStats
import com.example.workoutapp.data.repository.SessionHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionCoordinatorTest {

    private val sessionHistoryRepository = FakeSessionHistoryRepository()

    private val exercises = listOf(
        Exercise(id = 1, name = "Bench", weight = 100f, reps = 10, sets = 2, usesSensor = false),
        Exercise(id = 2, name = "Squat", weight = 140f, reps = 5, sets = 3, usesSensor = true)
    )

    private val coordinator = WorkoutSessionCoordinator(
        sessionReducer = WorkoutSessionReducer(),
        sessionCompletionCalculator = SessionCompletionCalculator(),
        sessionHistoryRepository = sessionHistoryRepository
    )

    @Test
    fun `startSession returns initial active exercise selection`() {
        val result = coordinator.startSession(
            exercises = exercises,
            completedSets = emptyMap()
        )

        assertEquals(emptyMap<Int, Int>(), result.completedSets)
        assertEquals(1, result.activeExerciseSelection.activeExerciseId)
        assertEquals(
            ExerciseSessionMode.MANUAL_REPS,
            result.activeExerciseSelection.activeExerciseMode
        )
    }

    @Test
    fun `completeNextSet returns state update and timer request when progress changes`() {
        val result = coordinator.completeNextSet(
            exercises = exercises,
            completedSets = emptyMap(),
            exerciseId = 1,
            restTimerDuration = 30,
            exerciseSwitchDuration = 90
        )

        assertTrue(result.didUpdate)
        assertEquals(PostSetTimerRequest.Start(30), result.timerRequest)
        assertEquals(1, result.stateUpdate?.completedSets?.get(1))
        assertEquals(1, result.stateUpdate?.activeExerciseSelection?.activeExerciseId)
        assertEquals(
            ExerciseSessionMode.MANUAL_REPS,
            result.stateUpdate?.activeExerciseSelection?.activeExerciseMode
        )
    }

    @Test
    fun `completeNextSet returns no update when reducer leaves progress unchanged`() {
        val result = coordinator.completeNextSet(
            exercises = exercises,
            completedSets = mapOf(1 to 2),
            exerciseId = 1,
            restTimerDuration = 30,
            exerciseSwitchDuration = 90
        )

        assertFalse(result.didUpdate)
        assertEquals(PostSetTimerRequest.None, result.timerRequest)
        assertNull(result.stateUpdate)
    }

    @Test
    fun `undoSet returns no update when undo is disabled`() {
        val result = coordinator.undoSet(
            exercises = exercises,
            completedSets = mapOf(1 to 1),
            exerciseId = 1,
            undoEnabled = false
        )

        assertFalse(result.didUpdate)
        assertNull(result.stateUpdate)
    }

    @Test
    fun `undoSet returns updated state when undo is enabled`() {
        val result = coordinator.undoSet(
            exercises = exercises,
            completedSets = mapOf(1 to 1),
            exerciseId = 1,
            undoEnabled = true
        )

        assertTrue(result.didUpdate)
        assertEquals(0, result.stateUpdate?.completedSets?.get(1))
        assertEquals(1, result.stateUpdate?.activeExerciseSelection?.activeExerciseId)
        assertEquals(
            ExerciseSessionMode.MANUAL_REPS,
            result.stateUpdate?.activeExerciseSelection?.activeExerciseMode
        )
    }

    @Test
    fun `completeSession calculates persists and returns reset payload`() = runBlocking {
        val result = coordinator.completeSession(
            exercises = exercises,
            completedSets = mapOf(1 to 2, 2 to 1),
            elapsedSeconds = 600,
            endTime = 1_234_567L,
            userMetrics = UserMetrics(weightKg = 80f),
            restTimerDuration = 30,
            exerciseSwitchDuration = 90,
            calorieIntensity = "normal"
        )

        assertEquals(101, result.completedSession.id)
        assertEquals(result.completedSession, sessionHistoryRepository.savedSession)
        assertEquals(emptyMap<Int, Int>(), result.stateUpdate.completedSets)
        assertNull(result.stateUpdate.activeExerciseSelection.activeExerciseId)
        assertEquals(ExerciseSessionMode.MANUAL_REPS, result.stateUpdate.activeExerciseSelection.activeExerciseMode)
        assertEquals(listOf(101, 101), sessionHistoryRepository.savedSessionExercises.map { it.sessionId })
        assertEquals(listOf("Bench", "Squat"), sessionHistoryRepository.savedSessionExercises.map { it.exerciseName })
    }

    private class FakeSessionHistoryRepository : SessionHistoryRepository {
        var savedSession: WorkoutSession? = null
        var savedSessionExercises: List<SessionExercise> = emptyList()

        override fun getSessions(): Flow<List<WorkoutSession>> = flowOf(emptyList())

        override suspend fun getSession(sessionId: Int): WorkoutSession? = null

        override fun getWorkoutStats(): Flow<WorkoutStats?> = flowOf(null)

        override suspend fun saveSession(session: WorkoutSession): Long {
            savedSession = session.copy(id = 101)
            return 101L
        }

        override suspend fun deleteSession(sessionId: Int) = Unit

        override suspend fun saveSessionExercises(exercises: List<SessionExercise>) {
            savedSessionExercises = exercises
        }

        override fun getSessionExercises(sessionId: Int): Flow<List<SessionExercise>> = flowOf(emptyList())

        override fun getExerciseHistory(exerciseName: String): Flow<List<SessionExercise>> = flowOf(emptyList())

        override fun getAllExerciseNames(): Flow<List<String>> = flowOf(emptyList())

        override fun getAllSessionExercises(): Flow<List<SessionExercise>> = flowOf(emptyList())
    }
}
