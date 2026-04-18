package com.example.workoutapp.data.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationOrchestrator @Inject constructor(
    private val legacyMigrationDataSource: LegacyMigrationDataSource,
    private val firestoreRepository: FirestoreRepository,
    private val legacyMigrationBackupCodec: LegacyMigrationBackupCodec
) {

    suspend fun migrateIfNeeded(uid: String): Result<Unit> {
        return runCatching {
            val existingMeta = firestoreRepository.getMigrationMeta(uid)
            if (existingMeta?.migrationComplete == true) return@runCatching

            val payload = legacyMigrationDataSource.loadPayload()

            firestoreRepository.performInitialMigration(
                uid = uid,
                userMetrics = payload.userMetrics,
                exercises = payload.exercises,
                sessions = payload.sessions,
                sessionExercises = payload.sessionExercises,
                restDays = payload.restDays,
                settings = payload.settings
            )
        }
    }

    suspend fun exportLegacyBackup(): Result<String> {
        return runCatching {
            legacyMigrationBackupCodec.encode(legacyMigrationDataSource.loadPayload())
        }
    }

    suspend fun importLegacyBackup(uid: String, backupJson: String): Result<Unit> {
        return runCatching {
            val payload = legacyMigrationBackupCodec.decode(backupJson)
            firestoreRepository.performInitialMigration(
                uid = uid,
                userMetrics = payload.userMetrics,
                exercises = payload.exercises,
                sessions = payload.sessions,
                sessionExercises = payload.sessionExercises,
                restDays = payload.restDays,
                settings = payload.settings
            )
        }
    }
}
