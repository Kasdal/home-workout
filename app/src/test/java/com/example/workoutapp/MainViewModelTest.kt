package com.example.workoutapp

import android.content.Context
import com.example.workoutapp.data.remote.UpdateChecker
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.data.settings.UpdateCheckPreferences
import com.example.workoutapp.data.storage.LegacyPhotoMigrator
import com.example.workoutapp.domain.startup.AppEntryState
import com.example.workoutapp.domain.startup.AppLaunchCoordinator
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
    private lateinit var legacySettingsBootstrapper: LegacySettingsBootstrapper
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var appLaunchCoordinator: AppLaunchCoordinator
    private lateinit var updateCheckPreferences: UpdateCheckPreferences
    private lateinit var updateChecker: UpdateChecker
    private lateinit var appContext: Context
    private lateinit var legacyPhotoMigrator: LegacyPhotoMigrator
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        legacySettingsBootstrapper = mockk(relaxed = true)
        localAppPreferencesRepository = mockk(relaxed = true)
        appLaunchCoordinator = mockk(relaxed = true)
        updateCheckPreferences = mockk(relaxed = true)
        updateChecker = mockk(relaxed = true)
        appContext = mockk(relaxed = true)
        legacyPhotoMigrator = mockk(relaxed = true)

        every { localAppPreferencesRepository.settings } returns flowOf(LocalAppSettings())
        every { appLaunchCoordinator.appEntryState() } returns flowOf(AppEntryState.Ready("workout"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = MainViewModel(
        legacySettingsBootstrapper = legacySettingsBootstrapper,
        localAppPreferencesRepository = localAppPreferencesRepository,
        updateCheckPreferences = updateCheckPreferences,
        updateChecker = updateChecker,
        appLaunchCoordinator = appLaunchCoordinator,
        appContext = appContext,
        legacyPhotoMigrator = legacyPhotoMigrator
    )

    @Test
    fun `appEntryState starts as null before coordinator emits`() = runTest {
        val upstream = MutableSharedFlow<AppEntryState>()
        every { appLaunchCoordinator.appEntryState() } returns upstream

        viewModel = createViewModel()

        assertEquals(null, viewModel.appEntryState.value)
    }

    @Test
    fun `appEntryState updates when coordinator emits after initialization`() = runTest {
        val upstream = MutableSharedFlow<AppEntryState>()
        every { appLaunchCoordinator.appEntryState() } returns upstream

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(null, viewModel.appEntryState.value)

        upstream.emit(AppEntryState.Ready("onboarding"))
        advanceUntilIdle()

        assertEquals(AppEntryState.Ready("onboarding"), viewModel.appEntryState.value)
    }

    @Test
    fun `appEntryState forwards migration in progress from coordinator`() = runTest {
        every { appLaunchCoordinator.appEntryState() } returns flowOf(AppEntryState.MigrationInProgress)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(AppEntryState.MigrationInProgress, viewModel.appEntryState.value)
    }
}
