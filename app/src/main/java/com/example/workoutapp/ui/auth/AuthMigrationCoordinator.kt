package com.example.workoutapp.ui.auth

import com.example.workoutapp.data.remote.MigrationOrchestrator
import com.example.workoutapp.data.remote.MigrationBootstrapResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMigrationCoordinator @Inject constructor(
    private val migrationOrchestrator: MigrationOrchestrator
) {

    private val migrationMutex = Mutex()

    suspend fun migrateIfNeeded(uid: String): Result<MigrationBootstrapResult> {
        return migrationMutex.withLock {
            migrationOrchestrator.migrateIfNeeded(uid)
        }
    }

    suspend fun importLegacyBackup(uid: String, backupJson: String): Result<Unit> {
        return migrationOrchestrator.importLegacyBackup(uid, backupJson)
    }
}
