package com.example.workoutapp.data.remote

import com.example.workoutapp.data.local.dao.WorkoutDao
import com.example.workoutapp.data.local.entity.Settings
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationOrchestrator @Inject constructor(
    private val dao: WorkoutDao,
    private val firestoreRepository: FirestoreRepository
) {

    suspend fun migrateIfNeeded(uid: String): Result<Unit> {
        return runCatching {
            val existingMeta = firestoreRepository.getMigrationMeta(uid)
            if (existingMeta?.migrationComplete == true) return@runCatching

            val userMetrics = dao.getAllUserMetrics().first()
            val exercises = dao.getAllExercises().first()
            val sessions = dao.getAllSessions().first()
            val restDays = dao.getAllRestDays().first()
            val settings = dao.getSettings().first() ?: Settings()

            val sessionExercises = sessions.flatMap { session ->
                dao.getSessionExercises(session.id).first()
            }

            firestoreRepository.performInitialMigration(
                uid = uid,
                userMetrics = userMetrics,
                exercises = exercises,
                sessions = sessions,
                sessionExercises = sessionExercises,
                restDays = restDays,
                settings = settings
            )
        }
    }
}
