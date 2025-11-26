package com.example.workoutapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.workoutapp.data.local.entity.Exercise
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
    suspend fun insertSession(session: WorkoutSession)
}
