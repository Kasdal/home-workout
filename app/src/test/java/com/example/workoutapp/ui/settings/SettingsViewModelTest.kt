package com.example.workoutapp.ui.settings

import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.example.workoutapp.model.WorkoutSession
import com.example.workoutapp.util.SoundManager
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var sessionHistoryRepository: SessionHistoryRepository
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        exerciseRepository = mockk(relaxed = true)
        sessionHistoryRepository = mockk(relaxed = true)
        localAppPreferencesRepository = mockk(relaxed = true)
        syncedWorkoutSettingsRepository = mockk(relaxed = true)

        every { localAppPreferencesRepository.settings } returns flowOf(LocalAppSettings())
        every { syncedWorkoutSettingsRepository.observeSessionSettings() } returns flowOf(WorkoutSessionSettings())
        viewModel = SettingsViewModel(
            legacySettingsBootstrapper = LegacySettingsBootstrapper(),
            exerciseRepository = exerciseRepository,
            sessionHistoryRepository = sessionHistoryRepository,
            localAppPreferencesRepository = localAppPreferencesRepository,
            syncedWorkoutSettingsRepository = syncedWorkoutSettingsRepository,
            soundManager = mockk<SoundManager>(relaxed = true),
            sensorRepository = mockk<SensorRepository>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exportData includes calorie audit fields in session CSV`() = runTest {
        every { exerciseRepository.getExercises() } returns flowOf(emptyList())
        every { sessionHistoryRepository.getSessions() } returns flowOf(
            listOf(
                WorkoutSession(
                    date = 123L,
                    durationSeconds = 600L,
                    totalWeightLifted = 1000f,
                    caloriesBurned = 42.5f,
                    notes = "test notes",
                    calorieFormulaVersion = 2,
                    calorieEstimateMode = "CUSTOM_MET",
                    calorieIntensity = "hard",
                    calorieUserWeightKg = 82.5f,
                    calorieMetCorrectionFactor = 1.15f,
                    calorieActiveSeconds = 420.5f,
                    calorieRestSeconds = 180
                )
            )
        )

        var csv = ""
        viewModel.exportData { csv = it }
        advanceUntilIdle()

        assertTrue(csv.contains("Date,Duration (min),Weight Lifted (kg),Calories,Calorie Formula Version,Calorie Estimate Mode,Calorie Intensity,Calorie User Weight (kg),Calorie MET Correction Factor,Calorie Active Seconds,Calorie Rest Seconds,Notes"))
        assertTrue(csv.contains("123,10,1000.0,42.5,2,CUSTOM_MET,hard,82.5,1.15,420.5,180,test notes"))
    }
}
