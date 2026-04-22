package com.example.workoutapp.data.remote

import com.example.workoutapp.data.local.entity.Settings
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

private data class SerializableLegacyMigrationPayload(
    val userMetrics: List<com.example.workoutapp.model.UserMetrics>,
    val exercises: List<com.example.workoutapp.model.Exercise>,
    val sessions: List<com.example.workoutapp.model.WorkoutSession>,
    val sessionExercises: List<com.example.workoutapp.model.SessionExercise>,
    val restDays: List<com.example.workoutapp.model.RestDay>,
    val settings: Settings?
)

@Singleton
class LegacyMigrationBackupCodec @Inject constructor() {
    private val gson = Gson()

    fun encode(payload: LegacyMigrationPayload): String {
        return gson.toJson(
            SerializableLegacyMigrationPayload(
                userMetrics = payload.userMetrics,
                exercises = payload.exercises,
                sessions = payload.sessions,
                sessionExercises = payload.sessionExercises,
                restDays = payload.restDays,
                settings = payload.settings
            )
        )
    }

    fun decode(json: String): LegacyMigrationPayload {
        val payload = gson.fromJson(json, SerializableLegacyMigrationPayload::class.java)
        return LegacyMigrationPayload(
            userMetrics = payload.userMetrics,
            exercises = payload.exercises,
            sessions = payload.sessions,
            sessionExercises = payload.sessionExercises,
            restDays = payload.restDays,
            settings = payload.settings
        )
    }
}
