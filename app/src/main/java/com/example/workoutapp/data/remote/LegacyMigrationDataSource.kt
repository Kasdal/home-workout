package com.example.workoutapp.data.remote

import com.example.workoutapp.model.Exercise
import com.example.workoutapp.model.RestDay
import com.example.workoutapp.model.SessionExercise
import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.model.WorkoutSession

data class LegacyMigrationPayload(
    val userMetrics: List<UserMetrics>,
    val exercises: List<Exercise>,
    val sessions: List<WorkoutSession>,
    val sessionExercises: List<SessionExercise>,
    val restDays: List<RestDay>,
    val settings: Settings?
)

interface LegacyMigrationDataSource {
    suspend fun loadPayload(): LegacyMigrationPayload
}
