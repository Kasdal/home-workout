package com.example.workoutapp.data.repository

import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.RestDay
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutDao
) : WorkoutRepository {

    override fun getUserMetrics(): Flow<UserMetrics?> = dao.getUserMetrics()

    override fun getAllUserMetrics(): Flow<List<UserMetrics>> = dao.getAllUserMetrics()

    override suspend fun saveUserMetrics(metrics: UserMetrics) {
        dao.deactivateAllProfiles()
        dao.insertUserMetrics(metrics.copy(isActive = true))
    }

    override suspend fun addUserMetrics(metrics: UserMetrics) = dao.insertUserMetrics(metrics)

    override suspend fun updateUserMetrics(metrics: UserMetrics) = dao.updateUserMetrics(metrics)

    override suspend fun setActiveProfile(profileId: Int) {
        dao.deactivateAllProfiles()
        dao.setActiveProfile(profileId)
    }

    override suspend fun deleteUserMetrics(profileId: Int) = dao.deleteUserMetrics(profileId)


    override fun getExercises(): Flow<List<Exercise>> = dao.getAllExercises()

    override suspend fun addExercise(exercise: Exercise) = dao.insertExercise(exercise)

    override suspend fun updateExercise(exercise: Exercise) = dao.updateExercise(exercise)

    override suspend fun deleteExercise(exerciseId: Int) = dao.deleteExercise(exerciseId)

    override fun getSessions(): Flow<List<WorkoutSession>> = dao.getAllSessions()

    override suspend fun saveSession(session: WorkoutSession): Long = dao.insertSession(session)
    
    override suspend fun deleteSession(sessionId: Int) = dao.deleteSession(sessionId)
    
    override fun getSettings() = dao.getSettings()
    
    override suspend fun saveSettings(settings: Settings) = dao.insertSettings(settings)
    
    override fun getRestDays(): Flow<List<RestDay>> = dao.getAllRestDays()
    
    override suspend fun addRestDay(restDay: RestDay) = dao.insertRestDay(restDay)
    
    override suspend fun deleteRestDay(restDayId: Int) = dao.deleteRestDay(restDayId)
    
    override suspend fun getRestDayByDate(date: Long): RestDay? = dao.getRestDayByDate(date)

    override suspend fun saveSessionExercises(exercises: List<com.example.workoutapp.data.local.entity.SessionExercise>) = 
        dao.insertSessionExercises(exercises)

    override fun getSessionExercises(sessionId: Int): Flow<List<com.example.workoutapp.data.local.entity.SessionExercise>> = 
        dao.getSessionExercises(sessionId)

    override fun getExerciseHistory(exerciseName: String): Flow<List<com.example.workoutapp.data.local.entity.SessionExercise>> = 
        dao.getExerciseHistory(exerciseName)

    override fun getAllExerciseNames(): Flow<List<String>> = dao.getAllExerciseNames()
}
