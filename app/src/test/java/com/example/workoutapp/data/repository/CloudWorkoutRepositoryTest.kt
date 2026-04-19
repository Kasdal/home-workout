package com.example.workoutapp.data.repository

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.remote.FirestoreRepository
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CloudWorkoutRepositoryTest {

    private lateinit var authManager: AuthManager
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var repository: CloudWorkoutRepository

    @Before
    fun setup() {
        authManager = mockk(relaxed = true)
        firestoreRepository = mockk(relaxed = true)
        repository = CloudWorkoutRepository(authManager, firestoreRepository)
    }

    @Test
    fun `saveSyncedWorkoutSettings writes partial synced settings directly to firestore`() = runTest {
        val settings = WorkoutSessionSettings(
            restTimerDuration = 45,
            exerciseSwitchDuration = 120,
            undoLastSetEnabled = false
        )
        every { authManager.currentUserId() } returns "user-123"
        coEvery { firestoreRepository.saveSyncedWorkoutSettings("user-123", settings) } returns Unit

        repository.saveSyncedWorkoutSettings(settings)

        coVerify { firestoreRepository.saveSyncedWorkoutSettings("user-123", settings) }
    }

    @Test
    fun `observeSyncedWorkoutSettings delegates to focused firestore settings stream`() = runTest {
        val settings = WorkoutSessionSettings(
            restTimerDuration = 45,
            exerciseSwitchDuration = 120,
            undoLastSetEnabled = false
        )
        every { firestoreRepository.observeSyncedWorkoutSettings("user-123") } returns flowOf(settings)
        every { authManager.currentUser } returns flowOf(mockk {
            every { uid } returns "user-123"
        })

        assertEquals(settings, repository.observeSyncedWorkoutSettings().first())
    }
}
