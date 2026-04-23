package com.example.workoutapp.data.remote

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MigrationOrchestratorTest {

    private val firestoreRepository = mockk<FirestoreRepository>(relaxed = true)
    private val legacyMigrationBackupCodec = mockk<LegacyMigrationBackupCodec>()

    @Test
    fun `migrateIfNeeded falls back to empty local payload when no legacy source exists`() = runTest {
        val orchestrator = MigrationOrchestrator(firestoreRepository, legacyMigrationBackupCodec)
        coEvery { firestoreRepository.getMigrationMeta("user-123") } returns null

        orchestrator.migrateIfNeeded("user-123")

        coVerify(exactly = 1) {
            firestoreRepository.performInitialMigration(
                uid = "user-123",
                userMetrics = emptyList(),
                exercises = emptyList(),
                sessions = emptyList(),
                sessionExercises = emptyList(),
                restDays = emptyList(),
                settings = null,
                force = false
            )
        }
    }

    @Test
    fun `importLegacyBackup forces migration even when migration meta already exists`() = runTest {
        val payload = mockk<LegacyMigrationPayload>()
        val orchestrator = MigrationOrchestrator(firestoreRepository, legacyMigrationBackupCodec)

        every { payload.userMetrics } returns emptyList()
        every { payload.exercises } returns emptyList()
        every { payload.sessions } returns emptyList()
        every { payload.sessionExercises } returns emptyList()
        every { payload.restDays } returns emptyList()
        every { payload.settings } returns null
        coEvery { legacyMigrationBackupCodec.decode("backup-json") } returns payload

        orchestrator.importLegacyBackup("user-123", "backup-json")

        coVerify(exactly = 1) {
            firestoreRepository.performInitialMigration(
                uid = "user-123",
                userMetrics = emptyList(),
                exercises = emptyList(),
                sessions = emptyList(),
                sessionExercises = emptyList(),
                restDays = emptyList(),
                settings = null,
                force = true
            )
        }
    }
}
