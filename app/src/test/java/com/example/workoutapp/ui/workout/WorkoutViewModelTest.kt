package com.example.workoutapp.ui.workout

import com.example.workoutapp.data.local.entity.Exercise
import com.example.workoutapp.data.local.entity.UserMetrics
import com.example.workoutapp.data.local.entity.WorkoutSession
import com.example.workoutapp.data.repository.ExerciseRepository
import com.example.workoutapp.data.repository.ProfileRepository
import com.example.workoutapp.data.repository.SensorRepository
import com.example.workoutapp.data.repository.SessionHistoryRepository
import com.example.workoutapp.data.settings.LegacySettingsBootstrapper
import com.example.workoutapp.data.settings.LocalAppPreferencesRepository
import com.example.workoutapp.data.settings.LocalAppSettings
import com.example.workoutapp.data.settings.SyncedWorkoutSettingsRepository
import com.example.workoutapp.data.settings.WorkoutSessionSettings
import com.example.workoutapp.domain.session.SessionCompletionCalculator
import com.example.workoutapp.util.SoundManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private lateinit var viewModel: WorkoutViewModel
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var sessionHistoryRepository: SessionHistoryRepository
    private lateinit var legacySettingsBootstrapper: LegacySettingsBootstrapper
    private lateinit var localAppPreferencesRepository: LocalAppPreferencesRepository
    private lateinit var syncedWorkoutSettingsRepository: SyncedWorkoutSettingsRepository
    private lateinit var soundManager: SoundManager
    private lateinit var sensorRepository: SensorRepository
    private lateinit var sessionCompletionCalculator: SessionCompletionCalculator
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        exerciseRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        sessionHistoryRepository = mockk(relaxed = true)
        legacySettingsBootstrapper = mockk(relaxed = true)
        localAppPreferencesRepository = mockk(relaxed = true)
        syncedWorkoutSettingsRepository = mockk(relaxed = true)
        soundManager = mockk(relaxed = true)
        sensorRepository = mockk(relaxed = true)
        sessionCompletionCalculator = SessionCompletionCalculator()

        // Default mocks
        coEvery { exerciseRepository.getExercises() } returns flowOf(
            listOf(
                Exercise(id = 1, name = "Bench Press", weight = 100f, reps = 10, sets = 4),
                Exercise(id = 2, name = "Squat", weight = 150f, reps = 5, sets = 5)
            )
        )
        coEvery { profileRepository.getUserMetrics() } returns flowOf(UserMetrics(weightKg = 80f))
        every { localAppPreferencesRepository.settings } returns flowOf(LocalAppSettings())
        every { syncedWorkoutSettingsRepository.observeSessionSettings() } returns flowOf(WorkoutSessionSettings())

        viewModel = WorkoutViewModel(
            exerciseRepository,
            profileRepository,
            sessionHistoryRepository,
            legacySettingsBootstrapper,
            localAppPreferencesRepository,
            syncedWorkoutSettingsRepository,
            soundManager,
            sensorRepository,
            sessionCompletionCalculator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init seeds local settings through bootstrapper`() = runTest {
        advanceUntilIdle()

        coVerify { legacySettingsBootstrapper.seedFromLegacySettingsIfPresent() }
    }

    @Test
    fun `startSession sets sessionStarted to true and starts timer`() = runTest {
        viewModel.startSession()
        assertTrue(viewModel.sessionStarted.value)
        
        advanceTimeBy(1001)
        assertEquals(1, viewModel.sessionElapsedSeconds.value)

        viewModel.pauseSession()
    }

    @Test
    fun `completeSession saves session and resets state`() = runTest {
        viewModel.startSession()
        advanceTimeBy(3600000) // 1 hour
        
        // Simulate completing sets
        viewModel.completeNextSet(1) // Bench Press set 1
        runCurrent()
        
        coEvery { sessionHistoryRepository.saveSession(any()) } returns 1L
        
        viewModel.completeSession { session ->
            assertEquals(3600L, session.durationSeconds)
            assertEquals(1000f, session.totalWeightLifted) // 1 set * 10 reps * 100 weight
        }
        runCurrent()
        
        coVerify { sessionHistoryRepository.saveSession(any()) }
        coVerify { sessionHistoryRepository.saveSessionExercises(any()) }
        
        assertFalse(viewModel.sessionStarted.value)
        assertEquals(0, viewModel.sessionElapsedSeconds.value)
    }

    @Test
    fun `completeNextSet increments set count and starts rest timer`() = runTest {
        viewModel.completeNextSet(1)
        runCurrent()
        
        val sets = viewModel.completedSets.value
        assertEquals(1, sets[1])
        
        // Should start rest timer (default 30s)
        assertTrue(viewModel.isTimerRunning.value)
        assertEquals(30, viewModel.timerSeconds.value)
    }

    @Test
    fun `undoSet decrements completed set count`() = runTest {
        viewModel.completeNextSet(1)
        runCurrent()

        viewModel.undoSet(1)
        runCurrent()

        val sets = viewModel.completedSets.value
        assertEquals(0, sets[1])
    }
    
    @Test
    fun `completeNextSet starts exercise switch timer on last set`() = runTest {
        // Bench press has 4 sets
        viewModel.completeNextSet(1)
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()
        viewModel.completeNextSet(1)
        runCurrent()
        
        val sets = viewModel.completedSets.value
        assertEquals(4, sets[1])
        
        // Should start switch timer (default 90s)
        assertTrue(viewModel.isTimerRunning.value)
        assertEquals(90, viewModel.timerSeconds.value)
    }

    @Test
    fun `timer countdown plays beep`() = runTest {
        viewModel.startTimer(5)
        
        advanceTimeBy(2000) // 3 seconds remaining
        runCurrent()
        verify(atLeast = 1) { soundManager.playTimerSound("beep", 1.0f, true) }
        
        advanceTimeBy(3000) // Finished
        runCurrent()
        verify(atLeast = 1) { soundManager.playTimerSound("beep", 1.0f, true) }
        assertFalse(viewModel.isTimerRunning.value)
    }
}
