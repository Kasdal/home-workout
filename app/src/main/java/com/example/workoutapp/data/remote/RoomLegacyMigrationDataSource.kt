package com.example.workoutapp.data.remote

import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.room.entity.toDomain
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLegacyMigrationDataSource @Inject constructor(
    private val dao: WorkoutDao
) : LegacyMigrationDataSource {
    override suspend fun loadPayload(): LegacyMigrationPayload {
        val sessions = dao.getAllSessions().first().map { it.toDomain() }

        return LegacyMigrationPayload(
            userMetrics = dao.getAllUserMetrics().first().map { it.toDomain() },
            exercises = dao.getAllExercises().first().map { it.toDomain() },
            sessions = sessions,
            sessionExercises = sessions.flatMap { session ->
                dao.getSessionExercises(session.id).first().map { it.toDomain() }
            },
            restDays = dao.getAllRestDays().first().map { it.toDomain() },
            settings = dao.getSettings().first()?.toDomain()
        )
    }
}
