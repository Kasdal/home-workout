package com.example.workoutapp.data.remote

import javax.inject.Inject
import javax.inject.Singleton

enum class MigrationBootstrapResult {
    READY,
    NEEDS_BACKUP_IMPORT
}

@Singleton
class MigrationOrchestrator @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val legacyMigrationBackupCodec: LegacyMigrationBackupCodec
) {

    suspend fun migrateIfNeeded(uid: String): Result<MigrationBootstrapResult> {
        return runCatching {
            val existingMeta = firestoreRepository.getMigrationMeta(uid)
            if (existingMeta?.migrationComplete == true) return@runCatching MigrationBootstrapResult.READY

            firestoreRepository.performInitialMigration(
                uid = uid,
                userMetrics = emptyList(),
                exercises = emptyList(),
                sessions = emptyList(),
                sessionExercises = emptyList(),
                restDays = emptyList(),
                settings = null
            )

            val updatedMeta = firestoreRepository.getMigrationMeta(uid)
            val hasRemoteData = updatedMeta != null && (
                updatedMeta.userMetricsCount > 0 ||
                    updatedMeta.exercisesCount > 0 ||
                    updatedMeta.sessionsCount > 0 ||
                    updatedMeta.sessionExercisesCount > 0 ||
                    updatedMeta.restDaysCount > 0
                )

            if (hasRemoteData) MigrationBootstrapResult.READY else MigrationBootstrapResult.NEEDS_BACKUP_IMPORT
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
                settings = payload.settings,
                force = true
            )
        }
    }
}
