package com.example.workoutapp

import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.repository.SettingsRepository
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.domain.startup.AppEntryState
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private lateinit var repository: SettingsRepository
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
        every { appLaunchCoordinator.appEntryState() } returns flowOf(AppEntryState.Ready("workout"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `appEntryState starts as null before coordinator emits`() = runTest {
        val upstream = MutableSharedFlow<AppEntryState>()
        every { appLaunchCoordinator.appEntryState() } returns upstream

        viewModel = MainViewModel(repository, localAppPreferencesRepository, appLaunchCoordinator)

        assertEquals(null, viewModel.appEntryState.value)
    }

    @Test
    fun `appEntryState updates when coordinator emits after initialization`() = runTest {
        val upstream = MutableSharedFlow<AppEntryState>()
        every { appLaunchCoordinator.appEntryState() } returns upstream

        viewModel = MainViewModel(repository, localAppPreferencesRepository, appLaunchCoordinator)
        advanceUntilIdle()

        assertEquals(null, viewModel.appEntryState.value)

        upstream.emit(AppEntryState.Ready("onboarding"))
        advanceUntilIdle()

        assertEquals(AppEntryState.Ready("onboarding"), viewModel.appEntryState.value)
    }

    @Test
    fun `appEntryState forwards migration in progress from coordinator`() = runTest {
        every { appLaunchCoordinator.appEntryState() } returns flowOf(AppEntryState.MigrationInProgress)

        viewModel = MainViewModel(repository, localAppPreferencesRepository, appLaunchCoordinator)
        advanceUntilIdle()

        assertEquals(AppEntryState.MigrationInProgress, viewModel.appEntryState.value)
    }
}
