package com.example.workoutapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.RestDay
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- User Metrics ---
    @Query("SELECT * FROM user_metrics WHERE isActive = 1 LIMIT 1")
    fun getUserMetrics(): Flow<UserMetrics?>

    @Query("SELECT * FROM user_metrics ORDER BY id DESC")
    fun getAllUserMetrics(): Flow<List<UserMetrics>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMetrics(metrics: UserMetrics)

    @Update
    suspend fun updateUserMetrics(metrics: UserMetrics)

    @Query("UPDATE user_metrics SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE user_metrics SET isActive = 1 WHERE id = :profileId")
    suspend fun setActiveProfile(profileId: Int)

    @Query("DELETE FROM user_metrics WHERE id = :profileId")
    suspend fun deleteUserMetrics(profileId: Int)


    // --- Exercises ---
    @Query("SELECT * FROM exercises WHERE isDeleted = 0")
    fun getAllExercises(): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("UPDATE exercises SET isDeleted = 1 WHERE id = :exerciseId")
    suspend fun deleteExercise(exerciseId: Int)

    // --- Sessions ---
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    // --- Settings ---
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings)

    // --- Rest Days ---
    @Query("SELECT * FROM rest_days ORDER BY date DESC")
    fun getAllRestDays(): Flow<List<RestDay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestDay(restDay: RestDay)

    @Query("DELETE FROM rest_days WHERE id = :restDayId")
    suspend fun deleteRestDay(restDayId: Int)

    @Query("SELECT * FROM rest_days WHERE date = :date LIMIT 1")
    suspend fun getRestDayByDate(date: Long): RestDay?

    // --- Session Exercises ---
    @Insert
    suspend fun insertSessionExercise(exercise: com.example.workoutapp.data.local.entity.SessionExercise)

    @Insert
    suspend fun insertSessionExercises(exercises: List<com.example.workoutapp.data.local.entity.SessionExercise>)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY sortOrder")
    fun getSessionExercises(sessionId: Int): Flow<List<com.example.workoutapp.data.local.entity.SessionExercise>>

    @Query("SELECT * FROM session_exercises WHERE exerciseName = :name ORDER BY sessionId DESC")
    fun getExerciseHistory(name: String): Flow<List<com.example.workoutapp.data.local.entity.SessionExercise>>

    @Query("SELECT DISTINCT exerciseName FROM session_exercises ORDER BY exerciseName")
    fun getAllExerciseNames(): Flow<List<String>>
}
