package com.example.workoutapp

import com.example.workoutapp.data.local.entity.Settings
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.repository.WorkoutRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var repository: WorkoutRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        
        // Default settings mock
        coEvery { repository.getSettings() } returns flowOf(Settings())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startDestination is workout when user metrics exist`() = runTest {
        coEvery { repository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))
        
        viewModel = MainViewModel(repository)
        advanceUntilIdle()
        
        assertEquals("workout", viewModel.startDestination.value)
    }

    @Test
    fun `startDestination is onboarding when user metrics do not exist`() = runTest {
        coEvery { repository.getUserMetrics() } returns flowOf(null)
        
        viewModel = MainViewModel(repository)
        advanceUntilIdle()
        
        assertEquals("onboarding", viewModel.startDestination.value)
    }
}
