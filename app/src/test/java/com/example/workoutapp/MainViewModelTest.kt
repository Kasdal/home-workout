package com.example.workoutapp

import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.repository.WorkoutRepository
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import io.mockk.coEvery
import io.mockk.every
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
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var appLaunchCoordinator: AppLaunchCoordinator
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        localAppPreferencesRepository = mockk(relaxed = true)
        appLaunchCoordinator = mockk(relaxed = true)

        coEvery { repository.getSettings() } returns flowOf(null)
        every { localAppPreferencesRepository.settings } returns flowOf(LocalAppSettings())
        every { appLaunchCoordinator.startDestination() } returns flowOf("workout")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startDestination is workout when user metrics exist`() = runTest {
        every { appLaunchCoordinator.startDestination() } returns flowOf("workout")

        viewModel = MainViewModel(repository, localAppPreferencesRepository, appLaunchCoordinator)
        advanceUntilIdle()
        
        assertEquals("workout", viewModel.startDestination.value)
    }

    @Test
    fun `startDestination is onboarding when user metrics do not exist`() = runTest {
        every { appLaunchCoordinator.startDestination() } returns flowOf("onboarding")

        viewModel = MainViewModel(repository, localAppPreferencesRepository, appLaunchCoordinator)
        advanceUntilIdle()
        
        assertEquals("onboarding", viewModel.startDestination.value)
    }

    @Test
    fun `startDestination is null when user is not signed in`() = runTest {
        every { appLaunchCoordinator.startDestination() } returns flowOf(null)

        viewModel = MainViewModel(repository, localAppPreferencesRepository, appLaunchCoordinator)
        advanceUntilIdle()

        assertEquals(null, viewModel.startDestination.value)
    }
}
