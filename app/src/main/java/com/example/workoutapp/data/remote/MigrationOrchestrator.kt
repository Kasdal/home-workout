package com.example.workoutapp.data.remote

import com.example.workoutapp.data.remote.model.CloudMigrationMeta
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
            if (existingMeta?.migrationComplete == true) {
                return@runCatching if (existingMeta.backupImportPending) {
                    MigrationBootstrapResult.NEEDS_BACKUP_IMPORT
                } else {
                    MigrationBootstrapResult.READY
                }
            }

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

            if (hasRemoteData) {
                MigrationBootstrapResult.READY
            } else {
                firestoreRepository.setMigrationMeta(
                    uid,
                    CloudMigrationMeta(
                        migrationComplete = true,
                        backupImportPending = true,
                        migratedAt = System.currentTimeMillis(),
                        userMetricsCount = 0,
                        exercisesCount = 0,
                        sessionsCount = 0,
                        sessionExercisesCount = 0,
                        restDaysCount = 0,
                        schemaVersion = 1
                    )
                )
                MigrationBootstrapResult.NEEDS_BACKUP_IMPORT
            }
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

    suspend fun continueWithoutBackupImport(uid: String): Result<Unit> {
        return runCatching {
            val existingMeta = firestoreRepository.getMigrationMeta(uid)
            firestoreRepository.setMigrationMeta(
                uid,
                CloudMigrationMeta(
                    migrationComplete = true,
                    backupImportPending = false,
                    migratedAt = existingMeta?.migratedAt ?: System.currentTimeMillis(),
                    userMetricsCount = existingMeta?.userMetricsCount ?: 0,
                    exercisesCount = existingMeta?.exercisesCount ?: 0,
                    sessionsCount = existingMeta?.sessionsCount ?: 0,
                    sessionExercisesCount = existingMeta?.sessionExercisesCount ?: 0,
                    restDaysCount = existingMeta?.restDaysCount ?: 0,
                    schemaVersion = existingMeta?.schemaVersion ?: 1
                )
            )
        }
    }
}
