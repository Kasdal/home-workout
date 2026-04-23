package com.example.workoutapp.ui.auth

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.auth.GoogleSignInClientFactory
import com.example.workoutapp.data.remote.MigrationBootstrapResult
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authManager: AuthManager
    private lateinit var googleSignInClientFactory: GoogleSignInClientFactory
    private lateinit var authMigrationCoordinator: AuthMigrationCoordinator
    private lateinit var appLaunchCoordinator: AppLaunchCoordinator
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var authFlow: MutableStateFlow<FirebaseUser?>
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authManager = mockk(relaxed = true)
        googleSignInClientFactory = mockk(relaxed = true)
        authMigrationCoordinator = mockk()
        appLaunchCoordinator = mockk(relaxed = true)
        firebaseUser = mockk()
        authFlow = MutableStateFlow(null)

        every { authManager.currentUser } returns authFlow
        every { authManager.currentUserId() } returns "user-123"
        every { firebaseUser.uid } returns "user-123"

        coEvery { authMigrationCoordinator.migrateIfNeeded("user-123") } returns Result.success(MigrationBootstrapResult.READY)
        coEvery {
            authMigrationCoordinator.importLegacyBackup("user-123", any())
        } returns Result.success(Unit)
        coEvery { authMigrationCoordinator.continueWithoutBackupImport("user-123") } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `auth observation updates signed in state and resets on sign out`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(AuthUiState(), viewModel.state.value)

        authFlow.value = firebaseUser
        advanceUntilIdle()

        assertEquals(
            AuthUiState(
                isSignedIn = true,
                isMigrationComplete = true
            ),
            viewModel.state.value
        )

        authFlow.value = null
        advanceUntilIdle()

        assertEquals(AuthUiState(), viewModel.state.value)
    }

    @Test
    fun `migration failure maps coordinator error into auth ui state`() = runTest {
        val migrationResult = CompletableDeferred<Result<MigrationBootstrapResult>>()
        coEvery { authMigrationCoordinator.migrateIfNeeded("user-123") } coAnswers {
            migrationResult.await()
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        authFlow.value = firebaseUser
        runCurrent()

        assertEquals(
            AuthUiState(
                isLoading = true,
                isSignedIn = true,
                isMigrationComplete = false
            ),
            viewModel.state.value
        )

        migrationResult.complete(Result.failure(IllegalStateException("Migration failed")))
        advanceUntilIdle()

        assertEquals(
            AuthUiState(
                isSignedIn = true,
                isMigrationComplete = false,
                errorMessage = "Migration failed"
            ),
            viewModel.state.value
        )
    }

    @Test
    fun `empty remote migration prompts backup import instead of navigating immediately`() = runTest {
        coEvery { authMigrationCoordinator.migrateIfNeeded("user-123") } returns Result.success(MigrationBootstrapResult.NEEDS_BACKUP_IMPORT)
        val viewModel = createViewModel()
        advanceUntilIdle()

        authFlow.value = firebaseUser
        advanceUntilIdle()

        assertEquals(
            AuthUiState(
                isSignedIn = true,
                isMigrationComplete = false,
                awaitingBackupImport = true,
                infoMessage = "Import a backup file if you have one, or continue without importing."
            ),
            viewModel.state.value
        )
    }

    @Test
    fun `continueWithoutImport marks migration complete and clears prompt state`() = runTest {
        coEvery { authMigrationCoordinator.migrateIfNeeded("user-123") } returns Result.success(MigrationBootstrapResult.NEEDS_BACKUP_IMPORT)
        val viewModel = createViewModel()
        advanceUntilIdle()

        authFlow.value = firebaseUser
        advanceUntilIdle()
        viewModel.continueWithoutImport()
        advanceUntilIdle()

        assertEquals(
            AuthUiState(
                isLoading = false,
                isSignedIn = true,
                isMigrationComplete = true,
                awaitingBackupImport = false,
                infoMessage = null,
                errorMessage = null
            ),
            viewModel.state.value
        )
    }

    @Test
    fun `backup import prompt updates app launch gate signal`() = runTest {
        coEvery { authMigrationCoordinator.migrateIfNeeded("user-123") } returns Result.success(MigrationBootstrapResult.NEEDS_BACKUP_IMPORT)
        val viewModel = createViewModel()
        advanceUntilIdle()

        authFlow.value = firebaseUser
        advanceUntilIdle()

        verify { appLaunchCoordinator.setBackupImportPending(true) }

        viewModel.continueWithoutImport()

        verify { appLaunchCoordinator.setBackupImportPending(false) }
    }

    @Test
    fun `import success toggles loading and surfaces completion info`() = runTest {
        val importResult = CompletableDeferred<Result<Unit>>()
        coEvery { authMigrationCoordinator.importLegacyBackup("user-123", "backup-json") } coAnswers {
            importResult.await()
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.importLegacyBackup("backup-json")
        runCurrent()

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.infoMessage)
        assertNull(viewModel.state.value.errorMessage)

        importResult.complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(
            AuthUiState(
                isLoading = false,
                isMigrationComplete = true,
                awaitingBackupImport = false,
                infoMessage = null
            ),
            viewModel.state.value
        )
    }

    @Test
    fun `import failure toggles loading off and surfaces error`() = runTest {
        val importResult = CompletableDeferred<Result<Unit>>()
        coEvery { authMigrationCoordinator.importLegacyBackup("user-123", "backup-json") } coAnswers {
            importResult.await()
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.importLegacyBackup("backup-json")
        runCurrent()

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.infoMessage)
        assertNull(viewModel.state.value.errorMessage)

        importResult.complete(Result.failure(IllegalStateException("Import failed")))
        advanceUntilIdle()

        assertEquals(
            AuthUiState(
                isLoading = false,
                isMigrationComplete = false,
                awaitingBackupImport = true,
                errorMessage = "Import failed"
            ),
            viewModel.state.value
        )
    }

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(authManager, googleSignInClientFactory, authMigrationCoordinator, appLaunchCoordinator)
    }
}
