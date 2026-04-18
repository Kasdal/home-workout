package com.example.workoutapp.domain.startup

import com.example.workoutapp.auth.AuthManager
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.repository.WorkoutRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLaunchCoordinatorTest {

    private val repository = mockk<WorkoutRepository>()
    private val authManager = mockk<AuthManager>()

    @Test
    fun `returns null when signed out`() = runTest {
        every { authManager.currentUser } returns flowOf(null)

        val result = AppLaunchCoordinator(repository, authManager).startDestination()

        assertEquals(null, result.first())
    }

    @Test
    fun `returns workout when signed in user has metrics`() = runTest {
        every { authManager.currentUser } returns flowOf(mockk())
        every { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))

        val result = AppLaunchCoordinator(repository, authManager).startDestination()

        assertEquals("workout", result.first())
    }

    @Test
    fun `returns onboarding when signed in user has no metrics`() = runTest {
        every { authManager.currentUser } returns flowOf(mockk())
        every { repository.getUserMetrics() } returns flowOf(null)

        val result = AppLaunchCoordinator(repository, authManager).startDestination()

        assertEquals("onboarding", result.first())
    }
}
