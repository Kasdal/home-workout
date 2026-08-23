package com.example.workoutapp.domain.startup

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.remote.model.CloudMigrationMeta
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.settings.MigrationPreferences
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchCoordinatorTest {

    private val repository = mockk<ProfileRepository>()
    private val firestoreRepository = mockk<FirestoreRepository>()
    private val authManager = mockk<AuthManager>()
    private val firebaseUser = mockk<FirebaseUser>()
    private val migrationPreferences = mockk<MigrationPreferences>(relaxed = true)

    private fun stubLocalState(complete: Boolean = false, destination: String? = null) {
        every { migrationPreferences.migrationComplete } returns flowOf(complete)
        every { migrationPreferences.lastStartDestination } returns flowOf(destination)
    }

    private fun coordinator() =
        AppLaunchCoordinator(repository, firestoreRepository, authManager, migrationPreferences)

    @Test
    fun `returns auth required when signed out`() = runTest {
        every { authManager.currentUser } returns flowOf(null)

        val result = coordinator().appEntryState()

        assertEquals(AppEntryState.AuthRequired, result.first())
    }

    @Test
    fun `returns migration in progress when signed in user has no migration metadata`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState()
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(null)

        val result = coordinator().appEntryState()

        assertEquals(AppEntryState.MigrationInProgress, result.first())
    }

    @Test
    fun `returns migration in progress when signed in user migration is incomplete`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState()
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = false)
        )

        val result = coordinator().appEntryState()

        assertEquals(AppEntryState.MigrationInProgress, result.first())
    }

    @Test
    fun `returns workout when signed in user migration is complete and metrics exist`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState()
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = true)
        )
        every { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))

        val result = coordinator().appEntryState()

        assertEquals(AppEntryState.Ready("workout"), result.first { it is AppEntryState.Ready })
    }

    @Test
    fun `returns onboarding when signed in user migration is complete and metrics are missing`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState()
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = true)
        )
        every { repository.getUserMetrics() } returns flowOf(null)

        val result = coordinator().appEntryState()

        assertEquals(AppEntryState.Ready("onboarding"), result.first { it is AppEntryState.Ready })
    }

    @Test
    fun `keeps ready state when migration metadata later becomes null`() = runTest {
        val migrationMetaFlow = MutableSharedFlow<CloudMigrationMeta?>(replay = 1)
        val metricsFlow = MutableSharedFlow<UserMetrics?>(replay = 1)
        val states = mutableListOf<AppEntryState>()

        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState()
        every { firestoreRepository.observeMigrationMeta("user-123") } returns migrationMetaFlow
        every { repository.getUserMetrics() } returns metricsFlow

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator()
                .appEntryState()
                .collect(states::add)
        }

        migrationMetaFlow.emit(CloudMigrationMeta(migrationComplete = true))
        metricsFlow.emit(UserMetrics(weightKg = 80f))
        advanceUntilIdle()

        migrationMetaFlow.emit(null)
        advanceUntilIdle()

        // The synthetic null from onStart emits a transient MigrationInProgress before
        // the real metadata resolves; distinctUntilChanged then keeps one Ready.
        assertEquals(listOf(AppEntryState.MigrationInProgress, AppEntryState.Ready("workout")), states)

        job.cancel()
    }

    @Test
    fun `holds app entry in migration state while backup import is pending`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState()
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = true)
        )
        every { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))

        val coordinator = coordinator()
        coordinator.setBackupImportPending(true)

        val result = coordinator.appEntryState()

        assertEquals(AppEntryState.MigrationInProgress, result.first())
    }

    @Test
    fun `enters app from local flag when live meta is unavailable`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState(complete = true, destination = "workout")
        every { firestoreRepository.observeMigrationMeta("user-123") } returns MutableSharedFlow()

        val result = coordinator().appEntryState().first()

        assertEquals(AppEntryState.Ready("workout"), result)
    }

    @Test
    fun `startup fails visibly after timeout when nothing resolves`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState(complete = false)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns MutableSharedFlow()

        val states = mutableListOf<AppEntryState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator()
                .appEntryState()
                .collect(states::add)
        }

        advanceTimeBy(AppLaunchCoordinator.STARTUP_TIMEOUT_MS + 1)
        advanceUntilIdle()

        assertEquals(AppEntryState.MigrationInProgress, states.first())
        assertTrue(states.contains(AppEntryState.StartupFailed))

        job.cancel()
    }

    @Test
    fun `successful migration persists local fallback state`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        stubLocalState(complete = false)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = true)
        )
        every { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coordinator().appEntryState().collect { }
        }
        advanceUntilIdle()

        io.mockk.coVerify { migrationPreferences.markMigrationComplete("workout") }

        job.cancel()
    }
}
