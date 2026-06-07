package com.example.workoutapp.data.remote

import com.example.workoutapp.data.remote.model.CloudMigrationMeta
import com.example.workoutapp.data.repository.CategoryRepository
import com.example.workoutapp.model.Category
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
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[0])
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[1])
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[2])
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[3])
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[4])
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[5])
            categoryRepository.upsertCategory(MigrationOrchestrator.SEED_CATEGORIES[6])
        }
        coVerify { categoryRepository.backfillLegacyAssignments(legacyCategoryId = Category.LEGACY_ID) }
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

    @Test
    fun `v1 meta with migrationComplete true still triggers seed and backfill`() = runTest {
        val firestore = mockk<FirestoreRepository>(relaxed = true)
        val categories = mockk<CategoryRepository>(relaxed = true)
        val codec = mockk<LegacyMigrationBackupCodec>(relaxed = true)
        val v1CompleteMeta = CloudMigrationMeta(
            migrationComplete = true,
            schemaVersion = 1,
            backupImportPending = false
        )
        coEvery { firestore.getMigrationMeta("user-123") } returns v1CompleteMeta

        val orchestrator = MigrationOrchestrator(
            firestoreRepository = firestore,
            legacyMigrationBackupCodec = codec,
            categoryRepository = categories
        )
        orchestrator.migrateIfNeeded("user-123")

        coVerify {
            for (seed in MigrationOrchestrator.SEED_CATEGORIES) {
                categories.upsertCategory(seed)
            }
            categories.backfillLegacyAssignments(legacyCategoryId = Category.LEGACY_ID)
        }
    }
}
