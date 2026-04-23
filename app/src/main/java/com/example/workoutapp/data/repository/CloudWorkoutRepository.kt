package com.example.workoutapp.data.repository

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.RestDay
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.model.Settings
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.model.WorkoutStats
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsStore
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CloudWorkoutRepository @Inject constructor(
    private val authManager: AuthManager,
    private val firestoreRepository: FirestoreRepository
) : ProfileRepository, SessionHistoryRepository, RestDayRepository, ExerciseRepository, SettingsRepository, SyncedWorkoutSettingsStore {

    override fun getUserMetrics(): Flow<UserMetrics?> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(null) else firestoreRepository.observeUserMetrics(user.uid)
    }

    override fun getAllUserMetrics(): Flow<List<UserMetrics>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeAllUserMetrics(user.uid)
    }

    override suspend fun saveUserMetrics(metrics: UserMetrics) {
        firestoreRepository.saveUserMetrics(requireUid(), metrics)
    }

    override suspend fun addUserMetrics(metrics: UserMetrics) {
        firestoreRepository.addUserMetrics(requireUid(), metrics)
    }

    override suspend fun updateUserMetrics(metrics: UserMetrics) {
        firestoreRepository.updateUserMetrics(requireUid(), metrics)
    }

    override suspend fun setActiveProfile(profileId: Int) {
        firestoreRepository.setActiveProfile(requireUid(), profileId)
    }

    override suspend fun deleteUserMetrics(profileId: Int) {
        firestoreRepository.deleteUserMetrics(requireUid(), profileId)
    }

    override fun getExercises(): Flow<List<Exercise>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeExercises(user.uid)
    }

    override suspend fun addExercise(exercise: Exercise) {
        firestoreRepository.upsertExercise(requireUid(), exercise)
    }

    override suspend fun updateExercise(exercise: Exercise) {
        firestoreRepository.upsertExercise(requireUid(), exercise)
    }

    override suspend fun deleteExercise(exerciseId: Int) {
        firestoreRepository.markExerciseDeleted(requireUid(), exerciseId)
    }

    override fun getSessions(): Flow<List<WorkoutSession>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeSessions(user.uid)
    }

    override suspend fun getSession(sessionId: Int): WorkoutSession? {
        return firestoreRepository.getSession(requireUid(), sessionId)
    }

    override fun getWorkoutStats(): Flow<WorkoutStats?> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(null) else firestoreRepository.observeWorkoutStats(user.uid)
    }

    override suspend fun saveSession(session: WorkoutSession): Long {
        return firestoreRepository.saveSession(requireUid(), session)
    }

    override suspend fun deleteSession(sessionId: Int) {
        firestoreRepository.deleteSession(requireUid(), sessionId)
    }

    override fun getSettings(): Flow<Settings?> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(null) else firestoreRepository.observeSettings(user.uid)
    }

    override fun observeSyncedWorkoutSettings(): Flow<WorkoutSessionSettings> {
        return authManager.currentUser.flatMapLatest { user ->
            if (user == null) flowOf(WorkoutSessionSettings())
            else firestoreRepository.observeSyncedWorkoutSettings(user.uid)
        }
    }

    override suspend fun saveSyncedWorkoutSettings(settings: WorkoutSessionSettings) {
        firestoreRepository.saveSyncedWorkoutSettings(requireUid(), settings)
    }

    override fun getRestDays(): Flow<List<RestDay>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeRestDays(user.uid)
    }

    override suspend fun addRestDay(restDay: RestDay) {
        firestoreRepository.addRestDay(requireUid(), restDay)
    }

    override suspend fun deleteRestDay(restDayId: Int) {
        firestoreRepository.deleteRestDay(requireUid(), restDayId)
    }

    override suspend fun getRestDayByDate(date: Long): RestDay? {
        return firestoreRepository.getRestDayByDate(requireUid(), date)
    }

    override suspend fun saveSessionExercises(exercises: List<SessionExercise>) {
        firestoreRepository.saveSessionExercises(requireUid(), exercises)
    }

    override fun getSessionExercises(sessionId: Int): Flow<List<SessionExercise>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeSessionExercises(user.uid, sessionId)
    }

    override fun getExerciseHistory(exerciseName: String): Flow<List<SessionExercise>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeExerciseHistory(user.uid, exerciseName)
    }

    override fun getAllExerciseNames(): Flow<List<String>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeAllExerciseNames(user.uid)
    }

    override fun getAllSessionExercises(): Flow<List<SessionExercise>> = authManager.currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else firestoreRepository.observeAllSessionExercises(user.uid)
    }

    private fun requireUid(): String {
        return authManager.currentUserId() ?: throw IllegalStateException("User is not signed in")
    }
}
