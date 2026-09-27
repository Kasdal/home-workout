package com.example.workoutapp.data.repository

import com.example.workoutapp.model.ExerciseStats
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface SessionHistoryRepository {
    fun getSessions(): Flow<List<WorkoutSession>>
    suspend fun getSession(sessionId: Int): WorkoutSession?
    suspend fun saveSession(session: WorkoutSession): Long
    suspend fun deleteSession(sessionId: Int)
    suspend fun saveSessionExercises(exercises: List<SessionExercise>, sessionDateMillis: Long)
    fun getSessionExercises(sessionId: Int): Flow<List<SessionExercise>>
    fun getExerciseHistory(exerciseName: String): Flow<List<SessionExercise>>
    fun getAllSessionExercises(): Flow<List<SessionExercise>>

    /**
     * Denormalised per-exercise analytics, one entry per distinct exercise name.
     * Replaces reading every [SessionExercise] to compute PRs and trends.
     */
    fun observeExerciseStats(): Flow<List<ExerciseStats>>

    /**
     * Rebuilds [observeExerciseStats] from existing sessionExercises.
     *
     * Idempotent: the merge is keyed on session id, so running it twice produces
     * the same aggregates. @return the number of documents written.
     */
    suspend fun backfillExerciseStats(): Int
}
