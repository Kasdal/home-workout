package com.example.workoutapp.ui.auth

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.auth.GoogleSignInClientFactory
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var authFlow: MutableStateFlow<FirebaseUser?>
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authManager = mockk(relaxed = true)
        googleSignInClientFactory = mockk(relaxed = true)
        authMigrationCoordinator = mockk()
        firebaseUser = mockk()
        authFlow = MutableStateFlow(null)

        every { authManager.currentUser } returns authFlow
        every { authManager.currentUserId() } returns "user-123"
        every { firebaseUser.uid } returns "user-123"

        coEvery { authMigrationCoordinator.migrateIfNeeded("user-123") } returns Result.success(Unit)
        coEvery { authMigrationCoordinator.exportLegacyBackup() } returns Result.success("backup-json")
        coEvery {
            authMigrationCoordinator.importLegacyBackup("user-123", any())
        } returns Result.success(Unit)
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
        val migrationResult = CompletableDeferred<Result<Unit>>()
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
    fun `export success sets info message and invokes completion callback`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        var exportedBackup: String? = null

        viewModel.exportLegacyBackup { exportedBackup = it }
        advanceUntilIdle()

        assertEquals("backup-json", exportedBackup)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Legacy backup exported.", viewModel.state.value.infoMessage)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `export failure clears info message and surfaces error`() = runTest {
        coEvery { authMigrationCoordinator.exportLegacyBackup() } returns Result.failure(
            IllegalStateException("Export failed")
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        var callbackInvoked = false

        viewModel.exportLegacyBackup { callbackInvoked = true }
        advanceUntilIdle()

        assertFalse(callbackInvoked)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.infoMessage)
        assertEquals("Export failed", viewModel.state.value.errorMessage)
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
                infoMessage = "Backup imported to cloud."
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
                errorMessage = "Import failed"
            ),
            viewModel.state.value
        )
    }

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(authManager, googleSignInClientFactory, authMigrationCoordinator)
    }
}
