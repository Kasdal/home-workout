package com.example.workoutapp.ui.auth

import com.example.workoutapp.data.remote.MigrationOrchestrator
import com.example.workoutapp.data.remote.MigrationBootstrapResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthMigrationCoordinatorTest {

    private val migrationOrchestrator = mockk<MigrationOrchestrator>()

    @Test
    fun `migrateIfNeeded returns orchestrator result for the uid`() = runTest {
        val expected = Result.success(MigrationBootstrapResult.READY)
        val coordinator = AuthMigrationCoordinator(migrationOrchestrator)

        coEvery { migrationOrchestrator.migrateIfNeeded("user-123") } returns expected

        val actual = coordinator.migrateIfNeeded("user-123")

        assertEquals(expected, actual)
        coVerify(exactly = 1) { migrationOrchestrator.migrateIfNeeded("user-123") }
    }

    @Test
    fun `migrateIfNeeded serializes concurrent migrations`() = runTest {
        val firstCallEntered = CompletableDeferred<Unit>()
        val releaseFirstCall = CompletableDeferred<Unit>()
        val coordinator = AuthMigrationCoordinator(migrationOrchestrator)

        coEvery { migrationOrchestrator.migrateIfNeeded(any()) } coAnswers {
            firstCallEntered.complete(Unit)
            releaseFirstCall.await()
            Result.success(MigrationBootstrapResult.READY)
        }

        val first = async { coordinator.migrateIfNeeded("user-123") }
        firstCallEntered.await()
        val second = async { coordinator.migrateIfNeeded("user-123") }

        coVerify(exactly = 1) { migrationOrchestrator.migrateIfNeeded(any()) }

        releaseFirstCall.complete(Unit)

        first.await()
        second.await()

        coVerify(exactly = 2) { migrationOrchestrator.migrateIfNeeded("user-123") }
    }

    @Test
    fun `importLegacyBackup returns orchestrator result for uid and payload`() = runTest {
        val expected = Result.success(Unit)
        val coordinator = AuthMigrationCoordinator(migrationOrchestrator)

        coEvery { migrationOrchestrator.importLegacyBackup("user-123", "backup-json") } returns expected

        val actual = coordinator.importLegacyBackup("user-123", "backup-json")

        assertEquals(expected, actual)
        coVerify(exactly = 1) { migrationOrchestrator.importLegacyBackup("user-123", "backup-json") }
    }

    @Test
    fun `continueWithoutBackupImport returns orchestrator result for uid`() = runTest {
        val expected = Result.success(Unit)
        val coordinator = AuthMigrationCoordinator(migrationOrchestrator)

        coEvery { migrationOrchestrator.continueWithoutBackupImport("user-123") } returns expected

        val actual = coordinator.continueWithoutBackupImport("user-123")

        assertEquals(expected, actual)
        coVerify(exactly = 1) { migrationOrchestrator.continueWithoutBackupImport("user-123") }
    }
}
