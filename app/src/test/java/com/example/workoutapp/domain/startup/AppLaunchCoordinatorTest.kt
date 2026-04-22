package com.example.workoutapp.domain.startup

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.model.UserMetrics
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.remote.model.CloudMigrationMeta
import com.example.workoutapp.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchCoordinatorTest {

    private val repository = mockk<ProfileRepository>()
    private val firestoreRepository = mockk<FirestoreRepository>()
    private val authManager = mockk<AuthManager>()
    private val firebaseUser = mockk<FirebaseUser>()

    @Test
    fun `returns auth required when signed out`() = runTest {
        every { authManager.currentUser } returns flowOf(null)

        val result = AppLaunchCoordinator(repository, firestoreRepository, authManager).appEntryState()

        assertEquals(AppEntryState.AuthRequired, result.first())
    }

    @Test
    fun `returns migration in progress when signed in user has no migration metadata`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(null)

        val result = AppLaunchCoordinator(repository, firestoreRepository, authManager).appEntryState()

        assertEquals(AppEntryState.MigrationInProgress, result.first())
    }

    @Test
    fun `returns migration in progress when signed in user migration is incomplete`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = false)
        )

        val result = AppLaunchCoordinator(repository, firestoreRepository, authManager).appEntryState()

        assertEquals(AppEntryState.MigrationInProgress, result.first())
    }

    @Test
    fun `returns workout when signed in user migration is complete and metrics exist`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = true)
        )
        every { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))

        val result = AppLaunchCoordinator(repository, firestoreRepository, authManager).appEntryState()

        assertEquals(AppEntryState.Ready("workout"), result.first())
    }

    @Test
    fun `returns onboarding when signed in user migration is complete and metrics are missing`() = runTest {
        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns flowOf(
            CloudMigrationMeta(migrationComplete = true)
        )
        every { repository.getUserMetrics() } returns flowOf(null)

        val result = AppLaunchCoordinator(repository, firestoreRepository, authManager).appEntryState()

        assertEquals(AppEntryState.Ready("onboarding"), result.first())
    }

    @Test
    fun `keeps ready state when migration metadata later becomes null`() = runTest {
        val migrationMetaFlow = MutableSharedFlow<CloudMigrationMeta?>(replay = 1)
        val metricsFlow = MutableSharedFlow<UserMetrics?>(replay = 1)
        val states = mutableListOf<AppEntryState>()

        every { firebaseUser.uid } returns "user-123"
        every { authManager.currentUser } returns flowOf(firebaseUser)
        every { firestoreRepository.observeMigrationMeta("user-123") } returns migrationMetaFlow
        every { repository.getUserMetrics() } returns metricsFlow

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            AppLaunchCoordinator(repository, firestoreRepository, authManager)
                .appEntryState()
                .collect(states::add)
        }

        migrationMetaFlow.emit(CloudMigrationMeta(migrationComplete = true))
        metricsFlow.emit(UserMetrics(weightKg = 80f))
        advanceUntilIdle()

        migrationMetaFlow.emit(null)
        advanceUntilIdle()

        assertEquals(listOf(AppEntryState.Ready("workout")), states)

        job.cancel()
    }
}
