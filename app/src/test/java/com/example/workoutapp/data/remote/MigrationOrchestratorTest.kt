package com.example.workoutapp.data.remote

import com.example.workoutapp.data.remote.model.CloudMigrationMeta
import com.example.workoutapp.data.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MigrationOrchestratorTest {

    private val firestoreRepository = mockk<FirestoreRepository>(relaxed = true)
    private val legacyMigrationBackupCodec = mockk<LegacyMigrationBackupCodec>()
    private val categoryRepository = mockk<CategoryRepository>(relaxed = true)

    @Test
    fun `migrateIfNeeded falls back to empty local payload when no legacy source exists`() = runTest {
        val orchestrator = MigrationOrchestrator(firestoreRepository, legacyMigrationBackupCodec, categoryRepository)
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
        val orchestrator = MigrationOrchestrator(firestoreRepository, legacyMigrationBackupCodec, categoryRepository)

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

    @Test
    fun `v1 meta triggers seed categories and backfill on first run`() = runTest {
        coEvery { firestoreRepository.getMigrationMeta("user-123") } returns CloudMigrationMeta(migrationComplete = false, schemaVersion = 1)

        val orchestrator = MigrationOrchestrator(
            firestoreRepository = firestoreRepository,
            legacyMigrationBackupCodec = legacyMigrationBackupCodec,
            categoryRepository = categoryRepository
        )
        orchestrator.migrateIfNeeded("user-123")

        coVerifyOrder {
            for (seed in MigrationOrchestrator.SEED_CATEGORIES) {
                categoryRepository.upsertCategory(seed)
            }
        }
        coVerify { categoryRepository.backfillLegacyAssignments(legacyCategoryId = "legacy") }
    }

    @Test
    fun `v2 meta with migrationComplete true short-circuits without re-seeding`() = runTest {
        coEvery { firestoreRepository.getMigrationMeta("user-123") } returns CloudMigrationMeta(migrationComplete = true, schemaVersion = 2)

        val orchestrator = MigrationOrchestrator(
            firestoreRepository = firestoreRepository,
            legacyMigrationBackupCodec = legacyMigrationBackupCodec,
            categoryRepository = categoryRepository
        )
        orchestrator.migrateIfNeeded("user-123")

        coVerify(exactly = 0) { categoryRepository.upsertCategory(any()) }
        coVerify(exactly = 0) { categoryRepository.backfillLegacyAssignments(any()) }
    }
}
